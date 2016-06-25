package mailserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Facilitates receiving POP3 commands and sending responses to them in a
 * separate thread.
 */
public class ClientConnection implements Runnable {

    /** The next available unique ID to label a ClientConnection when logging. */
    private static int sNextId = 0;

    /** The socket that facilitates communication with the client. */
    private Socket mClientSocket;
    /** Used to interpret POP3 commands received from the client. */
    private CommandInterpreter mCommandInterpreter;
    /** Used to receive commands from the client. */
    private BufferedReader mReader;
    /** Used to send responses to the client. */
    private BufferedWriter mWriter;
    /** A unique ID for this ClientConnection to distinguish it in the log. */
    private int mId;

    /**
     * Construct a ClientConnection.
     *
     * @param clientSocket the socket to communicate with the client
     * @param timeout      the amount of time to wait for a response in milliseconds
     *                     before closing the connection
     * @throws IOException
     */
    public ClientConnection(Socket clientSocket, int timeout)
            throws IOException {

        mClientSocket = clientSocket;
        clientSocket.setSoTimeout(timeout);

        mCommandInterpreter = new CommandInterpreter();
        mReader = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()));
        mWriter = new BufferedWriter(new OutputStreamWriter(
                clientSocket.getOutputStream()));
        mId = sNextId++;

        System.out.printf("New connection (id: %d) from %s\n", mId, clientSocket
                .getInetAddress().toString());

        String ready = "+OK POP3 server ready\r\n";
        printMessage(ready, "response");

        mWriter.write(ready);
        mWriter.flush();
    }

    @Override
    public void run() {
        String request;
        String response;
        boolean timedOut = false;

        try {
            while (!mCommandInterpreter.isQuit()) {

                try {
                    request = mReader.readLine();
                    printMessage(request, "request");

                } catch (SocketTimeoutException e) {
                    // Close socket after timeout
                    mCommandInterpreter.timeout();
                    timedOut = true;
                    break;
                }

                response = mCommandInterpreter.handleInput(request);
                printMessage(response, "response");
                mWriter.write(response);
                mWriter.flush();
            }

            mClientSocket.close();

            if (timedOut) {
                System.out.printf("Connection (id: %d) from %s timed out.\n",
                        mId, mClientSocket.getInetAddress());
            } else {
                System.out.printf("Connection (id: %d) from %s was closed.\n",
                        mId, mClientSocket.getInetAddress());
            }

        } catch (IOException e) {
            Log.e(ClientConnection.class.getSimpleName(),
                    "run: An I/O error occurred", e);
        }
    }

    /**
     * Print the message to the console, without its newline character.
     *
     * @param message the message received
     * @param type    the type of the message (request or response)
     */
    private void printMessage(String message, String type) {
        System.out.printf("%d %s: %s\n", mId, type,
                message.replace("\n", "").replace("\r", ""));
    }
}
