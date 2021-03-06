package mailserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * <h1>G52APR Mail Server</h1>
 * <p/>
 * Overall, the Pop3Server class listens on a port for clients and instantiates
 * a threaded ClientConnection object for each client that connects. The
 * ClientConnection is responsible for accepting a request from the client,
 * passing it into the CommandInterpreter and sending the response it returns to
 * the client. The CommandInterpreter is responsible for processing the request
 * and calling the appropriate method(s) in EmailDatabase for any required
 * operations or response information. EmailDatabase is responsible for direct
 * access to the database and performs queries and update operations, as well as
 * provide the CommandInterpreter with responses.
 * <p/>
 * The database back end EmailDatabase is instantiated by connecting to the
 * database and creating a statement object from the connection object that is
 * returned from a successful connection. This statement object is used to
 * execute the SELECT and UPDATE SQL queries that are required for the POP3
 * commands.
 * <p/>
 * The user can only lock and access the specified mailbox if it is not already
 * locked. Once the password has been entered, the back end fills an ArrayList
 * of integers with the iMailIDs, with the zeroth position set to null since
 * emails are numbered from 1 to n as per the POP3 specification. A static array
 * (since the server now knows how many emails there are) of booleans is
 * instantiated and filled with "false" to indicate that no emails have been
 * deleted initially (again, the server has no interest in the zeroth index).
 * <p/>
 * I have added a "timeout" method which unlocks the maildrop (if applicable)
 * and closes the database statement and connection without deleting the marked
 * messages. This is used, as per the POP3 specification, when the client times
 * out after the set period of inactivity. I have also written some useful
 * private methods that perform common tasks throughout the back end, such as
 * getting the size of a specific message or all messages in octets, and getting
 * the index in the ArrayList of an iMailID.
 * <p/>
 * Explanation of the functionality of each method public method in
 * EmailDatabase is explained in the javadoc comments in the Database interface.
 * <p/>
 * When building up a multi-line response string I have made use of the
 * StringBuilder class for memory efficiency, to prevent creating and throwing
 * away n immutable strings.
 * <p/>
 * <h1>Changes made to CommandInterpreter</h1>
 * <p/>
 * I have added a "timeout" method to CommandInterpreter which calls the timeout
 * method in EmailDatabase (the Database interface implementation).
 * <p/>
 * <h1>Changes made to the network section (ClientConnection)</h1>
 * <p/>
 * The Connection class has been renamed ClientConnection to prevent a name
 * clash with the JDBC Connection class. When a SocketTimeoutException is
 * thrown, the timeout method in the CommandInterpreter is called.
 * <p/>
 * <h1>Testing</h1>
 * <p/>
 * I have manually tested the database back-end by entering a series of commands
 * using the unix telnet program. Evidence of this has been provided in the file
 * testing.txt. I have also updated the class TestPop3Server.java to test the
 * back-end implementation with the accounts in the database (alex, bob and
 * claire) with 3 separate sockets.
 * <p/>
 * <h1>Problems encountered</h1>
 * <p/>
 * I experienced erratic problems when I tried to add a static modifier to the
 * connection object in EmailDatabase. It caused problems with concurrency, and
 * even logging a mailbox back in again after it had been logged out. Using a
 * static initialiser did not change this.
 *
 * @author Steven Hood
 * @version 14-06-2015
 */
public class Pop3Server {

    /**
     * Tag used for logging.
     */
    private static final String TAG = Pop3Server.class.getSimpleName();

    /** The default port to bind the server to. */
    public static final int DEFAULT_PORT = 110;

    /** The default response timeout before closing the connection. */
    public static final int DEFAULT_TIMEOUT = 600000;

    /** The port the server will be bound to. */
    private final int mPort;

    /** The response timeout before closing the connection. */
    private final int mTimeout;

    /** Whether the server is running or not. */
    private boolean mRunning;

    /**
     * @param args optional command line arguments (port, timeout)
     */
    public static void main(String[] args) {
        int port;
        int timeout;

        if (args.length < 2) {
            // Default values if unspecified, 10 minute inactivity timeout
            port = DEFAULT_PORT;
            timeout = DEFAULT_TIMEOUT;

        } else {
            try {
                port = Integer.parseInt(args[0]);
                // Convert from seconds to milliseconds
                timeout = Integer.parseInt(args[1]) * 1000;

            } catch (NumberFormatException e) {
                System.err.println("Usage: Pop3Server [PORT] [TIMEOUT]");
                System.err.println("Error: Port and timeout must be integers");
                return;
            }
        }

        if (!testConnection()) {
            System.err.println("Error: Cannot connect to database");
            return;
        } else if (port < 0) {
            System.err.println("Error: Port value out of range");
            return;
        }

        Pop3Server server = new Pop3Server(port, timeout);
        server.start();

        System.out.printf("Running on port %d\n", port);
        System.out.printf("Timeout in %dms\n\n", timeout);
    }

    public Pop3Server(int port, int timeout) {
        mPort = port;
        mTimeout = timeout;
    }

    /**
     * Start the server. Can only be called again after stop() has been called.
     */
    public void start() {

        if (mRunning) {
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                ServerSocket serverSocket;
                mRunning = true;

                try {
                    serverSocket = new ServerSocket(mPort);

                    while (mRunning) {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(
                                new ClientConnection(clientSocket, mTimeout)
                        ).start();
                    }

                    serverSocket.close();

                } catch (IOException e) {
                    Log.e(TAG, "run: Failed to accept new connection", e);
                }
            }

        }).start();

    }

    /**
     * Stop the server.
     */
    public void stop() {
        mRunning = false;
    }

    /**
     * Test the connection to the database is functioning correctly.
     *
     * @return <code>true</code> if the connection is working.
     * Otherwise <code>false</code>.
     */
    private static boolean testConnection() {
        boolean success = true;

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection connection = DriverManager.getConnection(Login.HOST,
                    Login.USERNAME, Login.PASSWORD);
            Statement statement = connection.createStatement();

            statement.close();
            connection.close();

        } catch (SQLException | InstantiationException
                | IllegalAccessException | ClassNotFoundException e) {
            Log.e(TAG, "testConnection: Failed to connect to database", e);
            success = false;
        }

        return success;
    }
}
