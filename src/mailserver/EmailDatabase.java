package mailserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Execute POP3 commands on a MySQL mail database.
 * <p>
 * Note: A Login class must contain the static information for logging onto the
 * MySQL server, including the HOST name, USERNAME and PASSWORD.
 */
public class EmailDatabase implements IDatabase {

    /** Tag used for logger / debugging. */
    private static final String TAG = IDatabase.class.getSimpleName();

    /** The connection to the SQL database. */
    private Connection mConnection;
    /** Used for executing SQL statements and returning the results. */
    private Statement mStatement;
    /** Whether the user is logged in or not. */
    private boolean mLoggedIn;
    /** The username for the user logged in / attempting to log in. */
    private String mUsername;
    /** The corresponding password for mUsername. */
    private String mPassword;
    /** The ID for the maildrop of mUsername. */
    private int mMaildropID;
    /** The number of messages that are not deleted in the session. */
    private int mNumUndeleted;

    /**
     * Index is set to true if the message has been marked as deleted. As with
     * iMailIDs, the zeroth index is ignored by the program.
     */
    private boolean[] mMarkedDeleted;

    /**
     * A List of the mailbox's iMailIDs, assigned when a PASS command is
     * successfully executed. The zeroth index is ignored by the program, hence
     * it is set to null.
     */
    private List<Integer> mMailIDs;

    /**
     * Construct a new EmailDatabase
     */
    public EmailDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            mConnection = DriverManager.getConnection(Login.HOST,
                    Login.USERNAME, Login.PASSWORD);
            mStatement = mConnection.createStatement();

        } catch (SQLException | InstantiationException
                | IllegalAccessException | ClassNotFoundException e) {
            Log.e(TAG, "constructor: Failed to connect to database.", e);
        }

        mLoggedIn = false;
    }

    @Override
    public String user(String uname) {
        try {
            ResultSet rs = mStatement.executeQuery("SELECT * FROM m_Maildrop");

            while (rs.next()) {
                if (rs.getString("vchUsername").equals(uname)) {
                    if (rs.getInt("tiLocked") == 0) {
                        // If the maildrop exists and it is unlocked
                        mUsername = uname;
                        mPassword = rs.getString("vchPassword");
                        mMaildropID = rs.getInt("iMailDropID");
                        return "+OK " + uname + " is a valid mailbox";

                    } else {
                        return "-ERR mailbox " + uname + " currently locked";
                    }
                }
            }

        } catch (SQLException e) {
            Log.e(TAG, "user: Failed to execute USER command", e);
        }

        return "-ERR mailbox " + uname + " does not exist";
    }

    @Override
    public String pass(String pword) {
        if (mPassword.equals(pword)) {
            try {
                lockMaildrop();
                ResultSet rs = mStatement.executeQuery(
                        "SELECT iMailID FROM m_Mail WHERE iMaildropID = "
                                + mMaildropID);

                // No message at position zero, assign each email a number from
                // 1 to n
                mMailIDs = new ArrayList<>();
                mMailIDs.add(null);
                while (rs.next()) {
                    mMailIDs.add(rs.getInt("iMailID"));
                }

                // Set all messages as unmarked for deletion
                mMarkedDeleted = new boolean[mMailIDs.size()];
                Arrays.fill(mMarkedDeleted, false);
                // Subtract 1 to ignore zeroth index
                mNumUndeleted = mMailIDs.size() - 1;
                mLoggedIn = true;

                System.out.println("iMailIDs of messages for user "
                        + mUsername + ": " + mMailIDs);
                System.out.println("Size of each message in octets:");
                for (int i = 1; i < mMailIDs.size(); i++) {
                    System.out.println(mMailIDs.get(i) + " "
                            + getOctets(i));
                }

            } catch (SQLException e) {
                Log.e(TAG, "pass: Failed to execute PASS command", e);
            }
            return "+OK maildrop locked and ready";

        } else {
            return "-ERR invalid mPassword for user " + mUsername;
        }
    }

    @Override
    public String dele(int messageNumber) {
        if (mMailIDs.size() > messageNumber) {

            // If the message is already marked for deletion
            if (mMarkedDeleted[messageNumber]) {
                return "-ERR message " + messageNumber + " already deleted";
            }

            // Mark the message for deletion
            mMarkedDeleted[messageNumber] = true;
            mNumUndeleted--;
            return "+OK message " + messageNumber + " deleted";

        } else {
            return "-ERR no such message";
        }
    }

    @Override
    public String list(int messageNumber) {
        if (messageNumber < 1) {
            // Return total number and size of all unmarked messages
            StringBuilder response = new StringBuilder();
            response.append("+OK ").append(mNumUndeleted).append(" messages (")
                    .append(getOctets(-1)).append(" octets)\r\n");

            for (int i = 1; i < mMailIDs.size(); i++) {
                if (!mMarkedDeleted[i]) {
                    response.append(i).append(" ").append(getOctets(i))
                            .append("\r\n");
                }
            }
            response.append(".");
            return response.toString();

        } else if (mMailIDs.size() > messageNumber &&
                !mMarkedDeleted[messageNumber]) {
            // Return size of message messageNumber if unmarked
            return "+OK " + messageNumber + " " + getOctets(messageNumber);

        } else {
            return "-ERR no such message";
        }
    }

    @Override
    public String getMessage(int messageNumber, int lineCount) {
        if (mMailIDs.size() > messageNumber && !mMarkedDeleted[messageNumber]) {
            String fullMessageBody = "";

            try {
                ResultSet rs = mStatement.executeQuery(
                        "SELECT txMailContent FROM m_Mail WHERE iMailID = "
                                + mMailIDs.get(messageNumber));
                rs.next();
                fullMessageBody = rs.getString("txMailContent");

            } catch (SQLException e) {
                Log.e(TAG, "getMessage: Failed to get message " + messageNumber,
                        e);
            }

            // RETR (return whole message, CommandInterpreter calls with
            // lineCount = -1)
            // TOP (return lineCount lines)

            // Split the header from the body into two array elements
            String[] header_body = fullMessageBody.split("\n\n", 2);
            // Last position in lines[] contains the remainder of the
            // unsplit message that is not to be sent
            String[] lines = header_body[1].split("\n", lineCount + 1);

            StringBuilder response = new StringBuilder("+OK\r\n");
            // Concatenate the header and partial message body
            response.append(header_body[0]).append("\n\n");
            for (int i = 0; i < lines.length - 1; i++) {
                if (lines[i].startsWith(".")) {
                    // Byte-stuff line by prepending with termination octet
                    response.append(".");
                }
                response.append(lines[i]).append("\r\n");
            }
            // Append the termination octet
            response.append(".");

            return response.toString();

        } else {
            return "-ERR no such message";
        }
    }

    @Override
    public String rset() {
        // Unmark all messages
        Arrays.fill(mMarkedDeleted, false);
        mNumUndeleted = mMailIDs.size() - 1;
        return "+OK";
    }

    @Override
    public String stat() {
        // Return total number and size of unmarked messages
        return "+OK " + mNumUndeleted + " " + getOctets(-1);
    }

    @Override
    public String uidl(int messageNumber) {
        if (messageNumber < 1) {
            // Return UIDL for all unmarked messages
            try {
                StringBuilder response = new StringBuilder("+OK\r\n");
                ResultSet rs = mStatement.executeQuery(
                        "SELECT iMailID, vchUIDL FROM m_Mail WHERE iMaildropID"
                                + " = " + mMaildropID);
                while (rs.next()) {
                    int i = getMessageNumber(rs.getInt("iMailID"));
                    if (!mMarkedDeleted[i]) {
                        response.append(i).append(" ")
                                .append(rs.getString("vchUIDL")).append("\r\n");
                    }
                }
                response.append(".");
                return response.toString();

            } catch (SQLException e) {
                Log.e(TAG, "uidl: Failed to execute UIDL command", e);
            }

        } else if (mMailIDs.size() > messageNumber
                && !mMarkedDeleted[messageNumber]) {
            // Return UIDL for specific unmarked message
            try {
                ResultSet rs = mStatement.executeQuery(
                        "SELECT vchUIDL FROM m_Mail WHERE iMailID = "
                                + mMailIDs.get(messageNumber));
                rs.next();
                return "+OK " + messageNumber + " " + rs.getString("vchUIDL");

            } catch (SQLException e) {
                Log.e(TAG, "uidl: Failed to execute UIDL command for message "
                        + messageNumber, e);
            }
        }

        return "-ERR no such message";
    }

    @Override
    public String quit() {
        int numDeleted = 0;

        try {
            unlockMaildrop();

            // Delete all marked messages and end session
            for (int i = 1; i < mMarkedDeleted.length; i++) {
                if (mMarkedDeleted[i]) {
                    mStatement.executeUpdate(
                            "DELETE FROM m_Mail WHERE iMailID = "
                                    + mMailIDs.get(i));
                    numDeleted++;
                }
            }

            mStatement.close();
            mConnection.close();

        } catch (SQLException e) {
            Log.e(TAG, "quit: Failed to execute QUIT command", e);
            return "-ERR some deleted messages not removed";
        }

        return "+OK " + numDeleted
                + " messages removed, POP3 server signing off (" + mNumUndeleted
                + " messages left)";
    }

    @Override
    public void timeout() {
        try {
            if (mLoggedIn) {
                unlockMaildrop();
            }
            mStatement.close();
            mConnection.close();

        } catch (SQLException e) {
            Log.e(TAG, "timeout: Failed unlock maildrop or close DB connection",
                    e);
        }
    }

    /**
     * Maps the iMailID of a message in the database to its index in the
     * ArrayList of iMailIDs.
     */
    private int getMessageNumber(int mID) {
        for (int i = 1; i < mMailIDs.size(); i++) {
            if (mMailIDs.get(i).equals(mID)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the size of the maildrop or a specific message in octets
     * (excluding marked messages).
     *
     * @param messageNumber if greater than zero, return the size of a specific message.
     *                      Otherwise, return the size of the whole maildrop.
     */
    private int getOctets(int messageNumber) {
        int totalOctets = 0;

        String query = "SELECT iMailID, LENGTH(txMailContent) AS octets"
                + " FROM m_Mail WHERE iMaildropID = " + mMaildropID;
        if (messageNumber > 0) {
            // Find size of specific message instead of total size
            query += " AND iMailID = " + mMailIDs.get(messageNumber);
        }

        try {
            ResultSet rs = mStatement.executeQuery(query);
            while (rs.next()) {
                int i = getMessageNumber(rs.getInt("iMailID"));
                if (!mMarkedDeleted[i]) {
                    totalOctets += rs.getInt("octets");
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "getOctets: Failed to retrieve size of message "
                    + messageNumber, e);
        }

        return totalOctets;
    }

    /**
     * Locks the maildrop (identified by the user's iMaildropID) by setting the
     * tiLocked field to 1.
     */
    private void lockMaildrop() throws SQLException {
        mStatement.executeUpdate("UPDATE m_Maildrop SET tiLocked = 1 "
                + "WHERE vchUsername = '" + mUsername + "'");
    }

    /**
     * Locks the maildrop (identified by the user's iMaildropID) by setting the
     * tiLocked field to 0.
     */
    private void unlockMaildrop() throws SQLException {
        mStatement.executeUpdate("UPDATE m_Maildrop SET tiLocked = 0 "
                + "WHERE vchUsername = '" + mUsername + "'");
    }

}
