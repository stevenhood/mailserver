package mailserver;

/**
 * Represents the state of the server for a session.
 */
public enum State {
    AUTHORIZATION,
    TRANSACTION,
    UPDATE
}
