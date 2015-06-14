package mailserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientConnection implements Runnable {

	private static int nextid = 0;
	private static boolean debug = false;

	private Socket clientSocket;
	private CommandInterpreter ci;
	private BufferedReader in;
	private BufferedWriter out;
	private int id;

	public ClientConnection(Socket clientSocket, int timeout)
			throws IOException {
		this.clientSocket = clientSocket;
		clientSocket.setSoTimeout(timeout);

		ci = new CommandInterpreter();
		in = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(
				clientSocket.getOutputStream()));
		id = nextid++;

		System.out.printf("New connection (id: %d) from %s\n", id, clientSocket
				.getInetAddress().toString());

		String ready = "+OK POP3 server ready\r\n";
		printMessage(ready, "response");

		out.write(ready);
		out.flush();
	}

	@Override
	public void run() {
		String request, response;
		boolean timedOut = false;

		try {
			while (!ci.isQuit()) {

				try {
					request = in.readLine();
					printMessage(request, "request");
				} catch (SocketTimeoutException e) {
					// Close socket after timeout
					ci.timeout();
					timedOut = true;
					break;
				}

				response = ci.handleInput(request);
				printMessage(response, "response");
				out.write(response);
				out.flush();
			}

			clientSocket.close();
			if (timedOut) {
				System.out.printf("Connection (id: %d) from %s timed out.\n",
						id, clientSocket.getInetAddress());
			} else {
				System.out.printf("Connection (id: %d) from %s was closed.\n",
						id, clientSocket.getInetAddress());
			}

		} catch (IOException | NullPointerException e) {
			if (debug)
				e.printStackTrace();
		}
	}

	/**
	 * Print the message to the console, without its newline character.
	 * 
	 * @param message
	 * @param type
	 */
	private void printMessage(String message, String type) {
		System.out.printf("%d %s: %s\n", id, type, message.replace("\n", "")
				.replace("\r", ""));
	}

	public int getID() {
		return id;
	}
}
