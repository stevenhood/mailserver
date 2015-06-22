package mailserver;

public class CommandInterpreter {

	private static final String OK = "+OK";
	private static final String ERR_MISSINGARGS = "-ERR required arguments missing";
	private static final String ERR_EXCESSIVEARGS = "-ERR excessive number of arguments";
	private static final String ERR_NEGMSGNUM = "-ERR message number must be greater than zero";
	private static boolean debug = false;

	/**
	 * Maintains the current server state (see State).
	 */
	private State state;

	/**
	 * Maintains whether or not the client has successfully issued a USER
	 * command. Prevents the PASS command from being issued prior.
	 */
	private boolean userIssued;

	private boolean isQuit;

	/**
	 * An implementation of the Database interface which contains all of the
	 * server responses to send to the client.
	 */
	private Database database;

	private String[] arguments;

	public CommandInterpreter() {
		setstateAuthorization();
		database = new EmailDatabase();
		userIssued = false;
		isQuit = false;
	}

	/**
	 * Interpret a request from a client and return a response.
	 * 
	 * @param input
	 *            POP3 command to be interpreted.
	 * @return the server's response string.
	 */
	public String handleInput(String input) {
		String command = "";
		String response = "";

		try {
			arguments = input.split(" ", -1);
			// Command is case-insensitive
			command = arguments[0].toUpperCase();

			switch (state) {
			case AUTHORIZATION:
				response = authorization(command);
				break;
			case TRANSACTION:
				response = transaction(command);
				break;
			case UPDATE:
				response = "-ERR cannot issue commands in UPDATE state";
				break;
			}

		} catch (IndexOutOfBoundsException e) {
			if (debug) {
				e.printStackTrace();
			}
			response = "-ERR command too short";

		} catch (NumberFormatException e) {
			if (debug) {
				e.printStackTrace();
			}
			response = "-ERR one or more arguments must be integer values";
		}

		// Include the request at the end of the response where the response
		// from the command interpreter is flexible
		if (!(command.equals("LIST") || command.equals("STAT") || command
				.equals("UIDL"))) {
			response += " " + input;
		}

		// Append CRLF and return
		return response + "\r\n";
	}

	public void timeout() {
		database.timeout();
	}

	private String authorization(String command) {
		String response = "";

		switch (command) {
		case "USER":
			response = user();
			break;
		case "PASS":
			response = pass();
			break;
		case "QUIT":
			response = quit();
			break;
		default:
			response = "-ERR command " + command
					+ " invalid in AUTHORIZATION state";
			break;
		}
		return response;
	}

	private String transaction(String command) {
		String response = "";

		switch (command) {
		case "DELE":
			response = dele();
			break;
		case "LIST":
			response = list();
			break;
		case "NOOP":
			response = noop();
			break;
		case "RETR":
			response = retr();
			break;
		case "TOP":
			response = top();
			break;
		case "RSET":
			response = rset();
			break;
		case "STAT":
			response = stat();
			break;
		case "UIDL":
			response = uidl();
			break;
		case "QUIT":
			response = quit();
			break;
		default:
			response = "-ERR command " + command
					+ " invalid in TRANSACTION state";
			break;
		}
		return response;
	}

	private String user() {
		// Required argument mailbox name
		if (userIssued) {
			return "-ERR USER command already issued";

		} else if (arguments.length < 2) {
			return ERR_MISSINGARGS;

		} else if (arguments.length > 2) {
			return ERR_EXCESSIVEARGS;

		} else {
			String response = database.user(arguments[1]);

			// Allow a PASS command to be issued if authentication is successful
			if (response.contains(OK)) {
				userIssued = true;
			}

			return response;
		}
	}

	private String pass() {
		// Required argument password and USER command issued prior
		if (!userIssued) {
			return "-ERR no USER command issued";

		} else if (arguments.length < 2) {
			return ERR_MISSINGARGS;

		} else {
			// Concat all arguments (excluding command in index 0) together as
			// the whole password
			StringBuilder password = new StringBuilder();
			for (int i = 1; i < arguments.length; i++) {
				password.append(arguments[i]);

				if (i < arguments.length - 1) {
					password.append(" ");
				}
			}

			String response = database.pass(password.toString());

			if (response.contains(OK)) {
				// Enter the TRANSACTION state if authorisation is
				// successful
				setstateTransaction();
			}

			return response;
		}
	}

	private String dele() {
		// Required argument message number
		if (arguments.length < 2) {
			return ERR_MISSINGARGS;

		} else if (arguments.length > 2) {
			return ERR_EXCESSIVEARGS;

		} else {
			int messageNumber = Integer.parseInt(arguments[1]);

			if (messageNumber < 1) {
				return ERR_NEGMSGNUM;
			}

			return database.dele(messageNumber);
		}
	}

	private String list() {
		int messageNumber;

		// Optional argument message number unspecified
		if (arguments.length < 2) {
			// tells database.list to return data for all messages
			messageNumber = -1;

		} else if (arguments.length > 2) {
			return ERR_EXCESSIVEARGS;

		} else {
			messageNumber = Integer.parseInt(arguments[1]);

			if (messageNumber < 1) {
				return ERR_NEGMSGNUM;
			}
		}

		// Either a single message or all messages in maildrop
		return database.list(messageNumber);
	}

	private String noop() {
		if (arguments.length > 1) {
			return ERR_EXCESSIVEARGS;
		} else {
			return OK;
		}
	}

	private String retr() {
		// Required argument message number
		if (arguments.length < 2) {
			return ERR_MISSINGARGS;

		} else if (arguments.length > 2) {
			return ERR_EXCESSIVEARGS;

		} else {
			int messageNumber = Integer.parseInt(arguments[1]);

			if (messageNumber < 1) {
				return ERR_NEGMSGNUM;
			}

			return database.getMessage(messageNumber, -1);
		}
	}

	private String top() {
		// Two required arguments message number and positive line
		// count
		if (arguments.length < 3) {
			return ERR_MISSINGARGS;

		} else if (arguments.length > 3) {
			return ERR_EXCESSIVEARGS;

		} else {
			int messageNumber = Integer.parseInt(arguments[1]);

			if (messageNumber < 1) {
				return ERR_NEGMSGNUM;
			}

			// Number of lines of message to return
			int lineCount = Integer.parseInt(arguments[2]);

			if (lineCount < 0) {
				return "-ERR negative line count";
			}

			return database.getMessage(messageNumber, lineCount);
		}
	}

	private String rset() {
		// Unmark all messages marked as deleted
		if (arguments.length > 1) {
			return ERR_EXCESSIVEARGS;
		} else {
			return database.rset();
		}
	}

	private String stat() {
		if (arguments.length > 1) {
			return ERR_EXCESSIVEARGS;
		} else {
			return database.stat();
		}
	}

	private String uidl() {
		int messageNumber;

		// Optional argument message number unspecified
		if (arguments.length < 2) {
			// tells database.uidl to return data for all messages if no message
			// specified
			messageNumber = -1;

		} else if (arguments.length > 2) {
			return ERR_EXCESSIVEARGS;

		} else {
			messageNumber = Integer.parseInt(arguments[1]);

			if (messageNumber < 1) {
				return ERR_NEGMSGNUM;
			}
		}

		return database.uidl(messageNumber);
	}

	private String quit() {
		if (arguments.length > 1) {
			return ERR_EXCESSIVEARGS;

		} else if (state == State.AUTHORIZATION) {
			// Closes the database statement and connection
			database.timeout();
			isQuit = true;
			return "+OK POP3 server signing off";

		} else {
			// state == TRANSACTION
			setstateUpdate();
			isQuit = true;
			return database.quit();
		}
	}

	private void setstateAuthorization() {
		state = State.AUTHORIZATION;
	}

	private void setstateTransaction() {
		state = State.TRANSACTION;
	}

	private void setstateUpdate() {
		state = State.UPDATE;
	}

	public boolean isQuit() {
		return isQuit;
	}

}
