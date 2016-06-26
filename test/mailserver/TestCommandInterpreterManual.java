package mailserver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * <h1>G52APR Mail Server - Coursework Part 1</h1>
 * <p>
 * The CommandInterpreter is used by passing a string containing the client
 * request into the handleInput method. This method will first separate the
 * command keyword and any arguments into an array of strings. It will then
 * determine if the command is valid in the current state and, if so, call the
 * appropriate method dedicated to that specific command in the class.
 * <p>
 * That method will perform appropriate error checking, such as checking for the
 * correct number of arguments, whether they can be parsed as integers, etc. If
 * successful, the method may access the database for a response. Execution will
 * then return to handleInput which will append the request on the end of the
 * response (depending on the command issued) and finally return the response
 * string.
 * </p>
 * <p>
 * <p>
 * The Database interface contains methods for commands that will need to access
 * the database in order to provide a response e.g. getting message data,
 * authenticating a user, marking messages for deletion, etc. All methods apart
 * from one (getMessage) have been given the same name as the commands that
 * cause them to be called by the CommandInterpreter. The getMessage method
 * provides a response for both RETR and TOP, depending on the values of the
 * arguments.
 * </p>
 * <p>
 * <h1>Testing</h1>
 * <p>
 * I have created this class to test the CommandInterpreter by using a method to
 * read a string manually entered at the console, call the handleInput method on
 * this string and subsequently print out the response.
 * <p>
 * I have also written a JUnit test case for the CommandInterpreter called
 * TestCommandInterpreter, which runs a series of uniform tests for each command
 * (except USER and PASS).
 * <p>
 * There is also a boolean field variable 'debug' in the CommandInterpreter
 * class which, if true, will set handleInput to print out individual arguments
 * once processed and print out the stack trace to stderr when exceptions are
 * caught.
 * </p>
 *
 * @author Steven Hood
 * @version 05-11-2013
 */
public class TestCommandInterpreterManual {

    private static final BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * @param args command line arguments
     */
    public static void main(String[] args) {

        CommandInterpreter ci = new CommandInterpreter(new EmailDatabase());
        String command;
        String response;
        boolean running = true;

        while (running) {
            command = readString();
            response = ci.handleInput(command);
            System.out.println(response);

            // TODO Case insensitive comparison
            if ("+OK".contains(response) && ("QUIT".contains(response) ||
                    "quit".contains(response))) {
                running = false;
            }
        }

    }

    /**
     * @return String entered at the console
     */
    private static String readString() {
        String input = null;

        try {
            // Prompt for input
            System.out.print("> ");

            input = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return input;
    }
}
