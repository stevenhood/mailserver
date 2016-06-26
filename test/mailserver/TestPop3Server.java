package mailserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TestPop3Server {

    private static Socket[] sSockets;
    private static BufferedReader[] sReaders;
    private static BufferedWriter[] sWriters;

    /**
     * @param args optional command line argument (port)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        int numRequests = 12;
        String[][] requests = {
                {"USER alex", "PASS hello123", "DELE 2", "LIST",
                        "LIST 3", "RETR 1", "TOP 2 10", "RSET",
                        "STAT", "UIDL", "UIDL 3", "QUIT"},
                {"USER bob", "PASS qwerty", "DELE 3", "LIST",
                        "LIST 1", "RETR 3", "TOP 3 0", "RSET",
                        "STAT", "UIDL", "UIDL 2", "QUIT"},
                {"USER claire", "PASS qazwsx", "DELE 1", "LIST",
                        "LIST 2", "RETR 4", "TOP 1 100", "RSET",
                        "STAT", "UIDL", "UIDL 2", "QUIT"}
        };

        int port = Pop3Server.DEFAULT_PORT;
        int numSockets = 3;

        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                e.printStackTrace();
                System.err.println("Usage: <port>");
            }

        } else if (args.length > 1) {
            System.err.println("Usage: <port>");
            return;
        }

        TestPop3Server tester = new TestPop3Server();

        tester.setup(numSockets, port);

        // Read in server greetings
        for (BufferedReader br : sReaders) {
            System.out.println(br.readLine());
        }

        for (int i = 0; i < requests.length; i++) {
            for (int j = 0; j < numRequests; j++) {
                // Execute that request for the socket and print the response
                String request = requests[i][j] + "\r\n";
                String response = tester.issueRequest(request, sReaders[i],
                        sWriters[i]);

                System.out.print("socket   " + i + ": " + request);
                System.out.println("response " + i + ": " + response);
            }
        }

        for (Socket s : sSockets) {
            s.close();
        }
    }

    /**
     * Setup connections to a mailserver instance.
     *
     * @param numSockets number of sockets to create
     * @param port       the port number on which the mailserver instance is running
     * @throws IOException
     */
    private void setup(int numSockets, int port) throws IOException {
        sSockets = new Socket[numSockets];
        for (int i = 0; i < numSockets; i++) {
            sSockets[i] = new Socket(InetAddress.getLocalHost(), port);
        }

        sReaders = new BufferedReader[numSockets];
        for (int i = 0; i < numSockets; i++) {
            sReaders[i] = new BufferedReader(
                    new InputStreamReader(sSockets[i].getInputStream())
            );
        }

        sWriters = new BufferedWriter[numSockets];
        for (int i = 0; i < numSockets; i++) {
            sWriters[i] = new BufferedWriter(
                    new OutputStreamWriter(sSockets[i].getOutputStream())
            );
        }
    }

    /**
     * Processes the request from a client and returns the reponse from the
     * server.
     *
     * @param request the request from the client to send to the server
     * @param reader  the input stream to send data to the client
     * @param writer  the output stream to send data to the server
     * @return the response from the server
     * @throws IOException
     */
    private String issueRequest(String request, BufferedReader reader,
                                BufferedWriter writer)
            throws IOException {

        // Send the request to the server
        writer.write(request);
        writer.flush();

        // Get the server's response
        StringBuilder response = new StringBuilder();
        String buf;

        while (null != (buf = reader.readLine())) {
            response.append(buf);
        }

        return response.toString();
    }
}
