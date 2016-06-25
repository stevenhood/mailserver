package mailserver;

/**
 * Class for logging messages to console.
 */
public class Log {

    private Log() {
        // Prevent the class from being instantiated
    }

    /**
     * Log a debug message.
     *
     * @param tag     the class name
     * @param message the debug message
     */
    public static void d(String tag, String message) {
        System.out.println(tag + ": " + message);
    }

    /**
     * Log an error message.
     *
     * @param tag     the class name
     * @param message the error message
     */
    public static void e(String tag, String message) {
        System.err.println(tag + ": " + message);
    }

    /**
     * Log an error message and stack trace.
     *
     * @param tag     the class name
     * @param message the error message
     * @param e       the Exception to log
     */
    public static void e(String tag, String message, Exception e) {
        e(tag, message);
        e.printStackTrace();
    }

}
