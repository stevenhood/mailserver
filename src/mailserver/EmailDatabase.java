package mailserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Note: A Login class must contain the information for logging onto the
 * MySQL server, including the HOST name, USERNAME and PASSWORD.
 * 
 * @author Steven Hood
 *
 */
public class EmailDatabase implements Database {

	private Connection connection;
	private Statement statement;

	private boolean loggedIn;
	private String username;
	private String password;
	private int maildropID;

	/**
	 * An ArrayList of the mailbox's iMailIDs, assigned when a PASS command is
	 * successfully executed. The zeroth index is ignored by the program, hence
	 * it is set to null.
	 */
	private ArrayList<Integer> iMailIDs;

	/**
	 * Index is set to true if the message has been marked as deleted. As with
	 * iMailIDs, the zeroth index is ignored by the program.
	 */
	private boolean[] markedDeleted;
	private int numUndeleted;

	private static boolean debug = false;

	public EmailDatabase() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = DriverManager.getConnection(Login.HOST, Login.USERNAME, Login.PASSWORD);
			statement = connection.createStatement();

		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			if (debug)
				e.printStackTrace();
		}

		loggedIn = false;
	}

	public String user(String uname) {
		try {
			ResultSet rs = statement.executeQuery("SELECT * FROM m_Maildrop");

			while (rs.next()) {
				if (rs.getString("vchUsername").equals(uname)) {
					if (rs.getInt("tiLocked") == 0) {
						// If the maildrop exists and it is unlocked
						username = uname;
						password = rs.getString("vchPassword");
						maildropID = rs.getInt("iMailDropID");
						return "+OK " + uname + " is a valid mailbox";

					} else {
						return "-ERR mailbox " + uname + " currently locked";
					}
				}
			}

		} catch (SQLException e) {
			if (debug)
				e.printStackTrace();
		}

		return "-ERR mailbox " + uname + " does not exist";
	}

	public String pass(String pword) {
		if (password.equals(pword)) {
			try {
				lockMaildrop();
				ResultSet rs = statement.executeQuery("SELECT iMailID FROM m_Mail WHERE iMaildropID = " + maildropID);

				// No message at position zero, assign each email a number from
				// 1 to n
				iMailIDs = new ArrayList<Integer>();
				iMailIDs.add(null);
				while (rs.next()) {
					iMailIDs.add(rs.getInt("iMailID"));
				}

				// Set all messages as unmarked for deletion
				markedDeleted = new boolean[iMailIDs.size()];
				Arrays.fill(markedDeleted, false);
				numUndeleted = iMailIDs.size() - 1;
				loggedIn = true;

				if (debug) {
					System.out.println("iMailIDs of messages for user " + username + ": " + iMailIDs);
					System.out.println("Size of each message in octets:");
					for (int i = 1; i < iMailIDs.size(); i++)
						System.out.println(iMailIDs.get(i) + " " + getOctets(i));
				}

			} catch (SQLException e) {
				if (debug)
					e.printStackTrace();
			}
			return "+OK maildrop locked and ready";

		} else {
			return "-ERR invalid password for user " + username;
		}
	}

	public String dele(int messageNumber) {
		if (iMailIDs.size() > messageNumber) {

			// If the message is already marked for deletion
			if (markedDeleted[messageNumber]) {
				return "-ERR message " + messageNumber + " already deleted";
			}

			// Mark the message for deletion
			markedDeleted[messageNumber] = true;
			numUndeleted--;
			return "+OK message " + messageNumber + " deleted";

		} else {
			return "-ERR no such message";
		}
	}

	public String list(int messageNumber) {
		if (messageNumber < 1) {
			// Return total number and size of all unmarked messages
			StringBuilder response = new StringBuilder();
			response.append("+OK ").append(numUndeleted).append(" messages (")
				.append(getOctets(-1)).append(" octets)\r\n");

			for (int i = 1; i < iMailIDs.size(); i++) {
				if (!markedDeleted[i]) {
					response.append(i).append(" ").append(getOctets(i)).append("\r\n");
				}
			}
			response.append(".");
			return response.toString();

		} else if (iMailIDs.size() > messageNumber && !markedDeleted[messageNumber]) {
			// Return size of message messageNumber if unmarked
			return "+OK " + messageNumber + " " + getOctets(messageNumber);

		} else {
			return "-ERR no such message";
		}
	}

	public String getMessage(int messageNumber, int lineCount) {
		if (iMailIDs.size() > messageNumber && !markedDeleted[messageNumber]) {
			String fullMessageBody = "";

			try {
				ResultSet rs = statement.executeQuery("SELECT txMailContent FROM m_Mail WHERE iMailID = " + iMailIDs.get(messageNumber));
				rs.next();
				fullMessageBody = rs.getString("txMailContent");

			} catch (SQLException e) {
				if (debug)
					e.printStackTrace();
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

	public String rset() {
		// Unmark all messages
		Arrays.fill(markedDeleted, false);
		numUndeleted = iMailIDs.size() - 1;
		return "+OK";
	}

	public String stat() {
		// Return total number and size of unmarked messages
		return "+OK " + numUndeleted + " " + getOctets(-1);
	}

	public String uidl(int messageNumber) {
		if (messageNumber < 1) {
			// Return UIDL for all unmarked messages
			try {
				StringBuilder response = new StringBuilder("+OK\r\n");
				ResultSet rs = statement.executeQuery("SELECT iMailID, vchUIDL FROM m_Mail WHERE iMaildropID = " + maildropID);
				while (rs.next()) {
					int i = getMessageNumber(rs.getInt("iMailID"));
					if (!markedDeleted[i]) {
						response.append(i).append(" ")
								.append(rs.getString("vchUIDL")).append("\r\n");
					}
				}
				response.append(".");
				return response.toString();

			} catch (SQLException e) {
				if (debug)
					e.printStackTrace();
			}

		} else if (iMailIDs.size() > messageNumber && !markedDeleted[messageNumber]) {
			// Return UIDL for specific unmarked message
			try {
				ResultSet rs = statement.executeQuery("SELECT vchUIDL FROM m_Mail WHERE iMailID = " + iMailIDs.get(messageNumber));
				rs.next();
				return "+OK " + messageNumber + " " + rs.getString("vchUIDL");

			} catch (SQLException e) {
				if (debug)
					e.printStackTrace();
			}
		}

		return "-ERR no such message";
	}

	public String quit() {
		int numDeleted = 0;

		try {
			unlockMaildrop();

			// Delete all marked messages and end session
			for (int i = 1; i < markedDeleted.length; i++) {
				if (markedDeleted[i]) {
					statement.executeUpdate("DELETE FROM m_Mail WHERE iMailID = " + iMailIDs.get(i));
					numDeleted++;
				}
			}

			statement.close();
			connection.close();

		} catch (SQLException e) {
			if (debug) {
				e.printStackTrace();
			}
			return "-ERR some deleted messages not removed";
		}

		return "+OK " + numDeleted + " messages removed, POP3 server signing off (" + numUndeleted + " messages left)";
	}

	public void timeout() {
		try {
			if (loggedIn) {
				unlockMaildrop();
			}
			statement.close();
			connection.close();

		} catch (SQLException e) {
			if (debug)
				e.printStackTrace();
		}
	}

	/**
	 * Maps the iMailID of a message in the database to its index in the
	 * ArrayList of iMailIDs.
	 */
	private int getMessageNumber(int mID) {
		for (int i = 1; i < iMailIDs.size(); i++) {
			if (iMailIDs.get(i).equals(mID)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the size of the maildrop or a specific message in octets
	 * (excluding marked messages).
	 * 
	 * @param messageNumber
	 *            if greater than zero, return the size of a specific message.
	 *            Otherwise, return the size of the whole maildrop.
	 */
	private int getOctets(int messageNumber) {
		int totalOctets = 0;

		String query = "SELECT iMailID, LENGTH(txMailContent) AS octets FROM m_Mail WHERE iMaildropID = " + maildropID;
		if (messageNumber > 0) {
			// Find size of specific message instead of total size
			query += " AND iMailID = " + iMailIDs.get(messageNumber);
		}

		try {
			ResultSet rs = statement.executeQuery(query);
			while (rs.next()) {
				int i = getMessageNumber(rs.getInt("iMailID"));
				if (!markedDeleted[i]) {
					totalOctets += rs.getInt("octets");
				}
			}
		} catch (SQLException e) {
			if (debug)
				e.printStackTrace();
		}

		return totalOctets;
	}

	/**
	 * Locks the maildrop (identified by the user's iMaildropID) by setting the
	 * tiLocked field to 1.
	 */
	private void lockMaildrop() throws SQLException {
		statement.executeUpdate("UPDATE m_Maildrop SET tiLocked = 1 WHERE vchUsername = '" + username + "'");
	}

	/**
	 * Locks the maildrop (identified by the user's iMaildropID) by setting the
	 * tiLocked field to 0.
	 */
	private void unlockMaildrop() throws SQLException {
		statement.executeUpdate("UPDATE m_Maildrop SET tiLocked = 0 WHERE vchUsername = '" + username + "'");
	}

}
