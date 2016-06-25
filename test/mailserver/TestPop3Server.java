package mailserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TestPop3Server {

    private static Socket[] sockets;
    private static BufferedReader[] readers;
    private static BufferedWriter[] writers;

    /**
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        int numRequests = 12;
        String[][] requests = {
                {"USER alex", "PASS hello123", "DELE 2", "LIST", "LIST 3", "RETR 1", "TOP 2 10", "RSET", "STAT", "UIDL", "UIDL 3", "QUIT"},
                {"USER bob", "PASS qwerty", "DELE 3", "LIST", "LIST 1", "RETR 3", "TOP 3 0", "RSET", "STAT", "UIDL", "UIDL 2", "QUIT"},
                {"USER claire", "PASS qazwsx", "DELE 1", "LIST", "LIST 2", "RETR 4", "TOP 1 100", "RSET", "STAT", "UIDL", "UIDL 2", "QUIT"}
        };

        // Default port
        int port = 110;
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

        setup(numSockets, port);

        // Read in server greetings
        for (BufferedReader br : readers) {
            System.out.println(br.readLine());
        }

        for (int i = 0; i < requests.length; i++) {
            for (int j = 0; j < numRequests; j++) {
                String request = requests[i][j] + "\r\n";
                // Execute that request for the socket and print the response
                System.out.print("socket   " + i + ": " + request);
                System.out.println("response " + i + ": " +
                        issueRequest(request, readers[i], writers[i]));
            }
        }

        for (Socket s : sockets) {
            s.close();
        }
    }

    /**
     *
     * @param numSockets
     * @param port
     * @throws IOException
     */
    private static void setup(int numSockets, int port) throws IOException {
        sockets = new Socket[numSockets];
        for (int i = 0; i < numSockets; i++) {
            sockets[i] = new Socket(InetAddress.getLocalHost(), port);
        }

        readers = new BufferedReader[numSockets];
        for (int i = 0; i < numSockets; i++) {
            readers[i] = new BufferedReader(
                    new InputStreamReader(sockets[i].getInputStream())
            );
        }

        writers = new BufferedWriter[numSockets];
        for (int i = 0; i < numSockets; i++) {
            writers[i] = new BufferedWriter(
                    new OutputStreamWriter(sockets[i].getOutputStream())
            );
        }
    }

    /**
     *
     * @param request
     * @param br
     * @param bw
     * @return
     * @throws IOException
     */
    private static String issueRequest(String request, BufferedReader br,
                                      BufferedWriter bw)
            throws IOException {

        bw.write(request);
        bw.flush();

        StringBuilder builder = new StringBuilder();
        String buf;

        while ((buf = br.readLine()) != null) {
            builder.append(buf);
        }

        return builder.toString();
    }
}
