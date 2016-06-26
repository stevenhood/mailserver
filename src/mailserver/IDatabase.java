package mailserver;

/**
 * The interface for interacting with a POP3 mail database.
 */
public interface IDatabase {

    /**
     * Verifies a username given as an argument of a USER command.
     *
     * @param user the name of the mailbox to access.
     * @return a positive response if the mailbox exists, otherwise negative.
     */
    String user(String user);

    /**
     * Authenticates a password given as an argument of a PASS command in
     * conjunction with the user of a previously issued USER command. The
     * mailbox is locked and the mailbox's iMailIDs are retrieved from the
     * database and added to the ArrayList.
     *
     * @param pass the corresponding password of the last issued USER command
     *             (case sensitive).
     * @return a positive response if the password exactly matches that of the
     * user of the previously issued USER command.
     */
    String pass(String pass);

    /**
     * Used for responding to a DELE command. Marks a specified message in the
     * maildrop as deleted and decrements the number of undeleted messages.
     *
     * @param messageNumber number of the message to mark as deleted.
     * @return a positive response if message exists and is not already marked
     * for deletion, otherwise negative.
     */
    String dele(int messageNumber);

    /**
     * Used for responding to a LIST command. Returns size in octets for a
     * specified message or total size of all messages if unspecified.
     *
     * @param messageNumber number of the message in the mailbox. If negative, return
     *                      size of all messages.
     * @return a positive response that is multi-line if the size of all
     * messages is requested, otherwise returns a single line containing
     * the message number and size of the individual message. A negative
     * response if the message does not exist or has been marked as
     * deleted.
     */
    String list(int messageNumber);

    /**
     * Used for responding to a RETR or TOP command. Returns all lines or
     * lineCount lines of the body of a particular message (excluding messages
     * marked deleted).
     *
     * @param messageNumber number of message in mailbox.
     * @param lineCount     number of lines of message to return, starting from the
     *                      body. If negative, return all lines (RETR).
     * @return if the message exists, a positive response with the first
     * lineCount lines of the message body, or all lines, depending on
     * the value of lineCount. A negative response if the message does
     * not exist or is marked as deleted.
     */
    String getMessage(int messageNumber, int lineCount);

    /**
     * Used for responding to an RSET command. Unsets all messages marked for
     * deletion.
     *
     * @return a positive response
     */
    String rset();

    /**
     * Used for responding to a STAT command with the number of messages in the
     * maildrop and its total size (excluding messages marked deleted in both
     * values).
     *
     * @return a positive response with a drop listing for the maildrop e.g. +OK
     * [# of messages in maildrop] [size of maildrop in octets]
     */
    String stat();

    /**
     * Used for responding to a UIDL command with the unique-id listing for all
     * or an individual message.
     *
     * @param messageNumber number of the message in the mailbox. If negative, return
     *                      for all messages.
     * @return a positive response with a unique-id listing for all messages
     * (excluding messages marked deleted) or the specified message if
     * it exists, otherwise negative.
     */
    String uidl(int messageNumber);

    /**
     * Used for responding to a QUIT command when the server is in the
     * TRANSACTION state. All messages marked for deletion are deleted and the
     * session is ended.
     *
     * @return a response based on the success of any message deletions.
     */
    String quit();

    /**
     * Unlocks the open mailbox (if applicable) and closes the statement and
     * database connection. The changes to the database (i.e. messages marked as
     * deleted) are not applied.
     */
    void timeout();
}
