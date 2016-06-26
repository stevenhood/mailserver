package mailserver;

/**
 * Interprets POP3 commands and executes the appropriate database action.
 */
public class CommandInterpreter {

    private static final String TAG = CommandInterpreter.class.getSimpleName();

    /** The OK response. */
    public static final String OK = "+OK";
    /** Error message indicating missing command arguments. */
    public static final String ERR_MISSINGARGS =
            "-ERR required arguments missing";
    /** Error message indicating too many command arguments. */
    public static final String ERR_EXCESSIVEARGS =
            "-ERR excessive number of arguments";
    /** Error message indicating a negative message number. */
    public static final String ERR_NEGMSGNUM =
            "-ERR message number must be greater than zero";
    /** Error message indicating a command issued in the UPDATE state. */
    public static final String ERR_CMD_UPDATE =
            "-ERR cannot issue commands in UPDATE state";

    /** Maintains the current server {@link State}. */
    private State mState;

    /**
     * Maintains whether or not the client has successfully issued a USER
     * command. Prevents the PASS command from being issued prior.
     */
    private boolean mIsUserIssued;

    /**
     * Whether a QUIT command has been successfully executed in this session
     * or not.
     */
    private boolean mIsQuit;

    /**
     * An implementation of the IDatabase interface which allows the commands to
     * be executed on the backend and return responses to the client.
     */
    private IDatabase mDatabase;

    /**
     * Construct a CommandInterpreter in the <code>AUTHORIZATION</code> state.
     * See {@link State}.
     *
     * @param database the IDatabase implementation for executing commands on
     *                 the backend
     */
    public CommandInterpreter(IDatabase database) {
        mState = State.AUTHORIZATION;
        mDatabase = database;
        mIsUserIssued = false;
        mIsQuit = false;
    }

    /**
     * Interpret a request from a client and return a response.
     *
     * @param input POP3 command to be interpreted.
     * @return the server's response string.
     */
    public String handleInput(String input) {
        String command = "";
        String response = "";

        try {
            String[] arguments = input.split(" ", -1);
            // Command is case-insensitive
            command = arguments[0].toUpperCase();

            switch (mState) {
                case AUTHORIZATION:
                    response = authorization(command, arguments);
                    break;
                case TRANSACTION:
                    response = transaction(command, arguments);
                    break;
                case UPDATE:
                    response = ERR_CMD_UPDATE;
                    break;
            }

        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "handleInput: Command is too short", e);
            response = "-ERR command too short";

        } catch (NumberFormatException e) {
            Log.e(TAG, "handleInput: Arguments of invalid type, must be int", e);
            response = "-ERR one or more arguments must be integer values";
        }

        // Include the request at the end of the response where the response
        // from the command interpreter is flexible
        if (!("LIST".equals(command) || "STAT".equals(command) ||
                "UIDL".equals(command))) {
            response += (" " + input);
        }

        // Append CRLF and return
        return response + "\r\n";
    }

    /**
     * Indicate that the connection has timed out. Causes this object to clean
     * up resources and cascade this message to its child objects.
     */
    public void timeout() {
        mDatabase.timeout();
    }

    /**
     * Handle a command when in the AUTHORIZATION state. Only commands that are
     * valid in the AUTHORIZATION state will be successfully executed.
     *
     * @param command   the full command to execute
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String authorization(String command, String[] arguments) {
        String response;

        switch (command) {
            case "USER":
                response = user(arguments);
                break;
            case "PASS":
                response = pass(arguments);
                break;
            case "QUIT":
                response = quit(arguments);
                break;
            default:
                response = "-ERR command " + command +
                        " invalid in AUTHORIZATION state";
                break;
        }

        return response;
    }

    /**
     * Handle a command when in the TRANSACTION state. Only commands that are
     * valid in the TRANSACTION state will be successfully executed.
     *
     * @param command   the full command to execute
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String transaction(String command, String[] arguments) {
        String response;

        switch (command) {
            case "DELE":
                response = dele(arguments);
                break;
            case "LIST":
                response = list(arguments);
                break;
            case "NOOP":
                response = noop(arguments);
                break;
            case "RETR":
                response = retr(arguments);
                break;
            case "TOP":
                response = top(arguments);
                break;
            case "RSET":
                response = rset(arguments);
                break;
            case "STAT":
                response = stat(arguments);
                break;
            case "UIDL":
                response = uidl(arguments);
                break;
            case "QUIT":
                response = quit(arguments);
                break;
            default:
                response = "-ERR command " + command +
                        " invalid in TRANSACTION state";
                break;
        }

        return response;
    }

    /**
     * Execute the USER command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String user(String[] arguments) {
        // Required argument mailbox name
        if (mIsUserIssued) {
            return "-ERR USER command already issued";

        } else if (arguments.length < 2) {
            return ERR_MISSINGARGS;

        } else if (arguments.length > 2) {
            return ERR_EXCESSIVEARGS;

        } else {
            String response = mDatabase.user(arguments[1]);

            // Allow a PASS command to be issued if verification is successful
            if (response.contains(OK)) {
                mIsUserIssued = true;
            }

            return response;
        }
    }

    /**
     * Execute the PASS command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String pass(String[] arguments) {
        // Required argument password and USER command issued prior
        if (!mIsUserIssued) {
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

            String response = mDatabase.pass(password.toString());

            if (response.contains(OK)) {
                // Enter the TRANSACTION state if authentication is successful
                mState = State.TRANSACTION;
            }

            return response;
        }
    }

    /**
     * Execute the DELE command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String dele(String[] arguments) {
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

            return mDatabase.dele(messageNumber);
        }
    }

    /**
     * Execute the LIST command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String list(String[] arguments) {
        int messageNumber;

        // Optional argument message number unspecified
        if (arguments.length < 2) {
            // tells mDatabase.list to return data for all messages
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
        return mDatabase.list(messageNumber);
    }

    /**
     * Execute the NOOP command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String noop(String[] arguments) {
        if (arguments.length > 1) {
            return ERR_EXCESSIVEARGS;
        } else {
            return OK;
        }
    }

    /**
     * Execute the RETR command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String retr(String[] arguments) {
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

            return mDatabase.getMessage(messageNumber, -1);
        }
    }

    /**
     * Execute the TOP command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String top(String[] arguments) {
        // Two required arguments (message number and positive line count)
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

            return mDatabase.getMessage(messageNumber, lineCount);
        }
    }

    /**
     * Execute the RSET command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String rset(String[] arguments) {
        // Unmark all messages marked as deleted
        if (arguments.length > 1) {
            return ERR_EXCESSIVEARGS;
        } else {
            return mDatabase.rset();
        }
    }

    /**
     * Execute the STAT command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String stat(String[] arguments) {
        if (arguments.length > 1) {
            return ERR_EXCESSIVEARGS;
        } else {
            return mDatabase.stat();
        }
    }

    /**
     * Execute the UIDL command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String uidl(String[] arguments) {
        int messageNumber;

        // Optional argument message number unspecified
        if (arguments.length < 2) {
            // Negative to tell mDatabase.uidl to return data for all messages
            // if no specific message is specified
            messageNumber = -1;

        } else if (arguments.length > 2) {
            return ERR_EXCESSIVEARGS;

        } else {
            messageNumber = Integer.parseInt(arguments[1]);

            if (messageNumber < 1) {
                return ERR_NEGMSGNUM;
            }
        }

        return mDatabase.uidl(messageNumber);
    }

    /**
     * Execute the QUIT command.
     *
     * @param arguments the command split into its individual arguments
     * @return the response to the command from the server
     */
    private String quit(String[] arguments) {
        if (arguments.length > 1) {
            return ERR_EXCESSIVEARGS;

        } else if (State.AUTHORIZATION == mState) {
            // Close the database connection
            mDatabase.timeout();
            mIsQuit = true;
            return "+OK POP3 server signing off";

        } else if (State.TRANSACTION == mState) {
            mState = State.UPDATE;
            mIsQuit = true;
            return mDatabase.quit();

        } else {
            Log.e(TAG, "quit: Invalid state");
            return ERR_CMD_UPDATE;
        }
    }

    /**
     * Whether a QUIT command has been successfully executed in this session or
     * not.
     */
    public boolean isQuit() {
        return mIsQuit;
    }

}
