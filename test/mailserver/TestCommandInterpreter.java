package mailserver;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mockito;

public class TestCommandInterpreter {

    private static final String OK = CommandInterpreter.OK;
    private static final String ERR = "-ERR";

    /** Mocked IDatabase */
    private IDatabase mDatabase;

    /** Instance under test */
    private CommandInterpreter mCi;

    @Before
    public void setUp() {

        mDatabase = Mockito.mock(IDatabase.class);

        mCi = new CommandInterpreter(mDatabase);
        //mCi.handleInput("USER test");
        //mCi.handleInput("PASS password");
    }

    @After
    public void tearDown() {
        Mockito.validateMockitoUsage();
    }

    private String concat(String code, String request) {
        return code + " " + request + "\r\n";
    }

    @Test
    public void testUserValid() {
        String uname = "test";
        String request = "USER " + uname;
        Mockito.doReturn(OK).when(mDatabase).user(uname);

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).user(uname);
        Assert.assertEquals(concat(OK, request), response);
    }

    @Test
    public void testUserCanOnlySuccessfullyExecuteOnce() {
        String uname = "test";
        String request = "USER " + uname;
        Mockito.doReturn(OK).when(mDatabase).user(uname);

        mCi.handleInput(request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).user(uname);
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testUserMissingArgsReturnsError() {
        String request = "USER";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).user(Mockito.anyString());
        String expected = concat(CommandInterpreter.ERR_MISSINGARGS, request);
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testUserExcessiveArgsReturnsError() {
        String request = "USER a b";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).user(Mockito.anyString());
        String expected = concat(CommandInterpreter.ERR_EXCESSIVEARGS, request);
        Assert.assertEquals(expected, response);
    }

    private void executeValidUser() {
        String uname = "test";
        String request = "USER " + uname;
        Mockito.doReturn(OK).when(mDatabase).user(uname);
        mCi.handleInput(request);
    }

    private void executeValidPassword(String password) {
        executeValidUser();
        Mockito.doReturn(OK).when(mDatabase).pass(password);
        String request = "PASS " + password;

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).pass(password);
        Assert.assertEquals(concat(OK, request), response);
    }

    @Test
    public void testPassValid() {
        executeValidPassword("password");
    }

    @Test
    public void testPassAsMultipleArgsValid() {
        executeValidPassword("pas sw ord");
    }

    @Test
    public void testPassFailsWithoutSuccessfulUser() {
        String password = "password";
        String request = "PASS " + password;

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).pass(password);
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testPassFailsWithMissingArgs() {
        executeValidUser();
        String request = "PASS";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).pass(Mockito.anyString());
        Assert.assertTrue(response.contains(ERR));
    }

    // DELE
    @Test
    public void testDELE_Valid() {
        String response = mCi.handleInput("DELE 1");
        // Valid request but still returns an error because there are no
        // messages in the dummy database
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testDELE_NoArgs() {
        String response = mCi.handleInput("DELE");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testDELE_ExcessiveArgs() {
        String response = mCi.handleInput("DELE 1 2");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testDELE_NonInt() {
        String response = mCi.handleInput("DELE abc");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testDELE_MessagenumLessThanOne() {
        String response = mCi.handleInput("DELE 0");
        assertTrue(response.contains(ERR));
    }

    // LIST
    @Test
    public void testLIST_ValidArg() {
        String response = mCi.handleInput("LIST 1");
        // Valid request but still returns an error because there are no
        // messages in the dummy database
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testLIST_ValidNoArg() {
        String response = mCi.handleInput("LIST");
        assertTrue(response.contains(OK));
    }

    @Test
    public void testLIST_ExcessiveArgs() {
        String response = mCi.handleInput("LIST 1 2");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testLIST_NonInt() {
        String response = mCi.handleInput("LIST xyz");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testLIST_MessagenumLessThanOne() {
        String response = mCi.handleInput("LIST 0");
        assertTrue(response.contains(ERR));
    }

    // NOOP
    @Test
    public void testNOOP_Valid() {
        String response = mCi.handleInput("NOOP");
        assertTrue(response.contains(OK));
    }

    @Test
    public void testNOOP_ExcessiveArgs() {
        String response = mCi.handleInput("NOOP x");
        assertTrue(response.contains(ERR));
    }

    // RETR
    @Test
    public void testRETR_Valid() {
        String response = mCi.handleInput("RETR 1");
        // Valid request but still returns an error because there are no
        // messages in the dummy database
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testRETR_NoArgs() {
        String response = mCi.handleInput("RETR");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testRETR_ExcessiveArgs() {
        String response = mCi.handleInput("RETR 1 2");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testRETR_NonInt() {
        String response = mCi.handleInput("RETR efg");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testRETR_MessagenumLessThanOne() {
        String response = mCi.handleInput("RETR 0");
        assertTrue(response.contains(ERR));
    }

    // TOP
    @Test
    public void testTOP_ValidArgs() {
        String response = mCi.handleInput("TOP 1 2");
        // Valid request but still returns an error because there are no
        // messages in the dummy database
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NoArgs() {
        String response = mCi.handleInput("TOP");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_ExcessiveArgs() {
        String response = mCi.handleInput("TOP 3 4 5");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NonIntArg1() {
        String response = mCi.handleInput("TOP x 2");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NonIntArg2() {
        String response = mCi.handleInput("TOP 1 x");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_MessagenumLessThanOne() {
        String response = mCi.handleInput("TOP 0 2");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_LinecountLessThanZero() {
        String response = mCi.handleInput("TOP 1 -1");
        assertTrue(response.contains(ERR));
    }

    // RSET
    @Test
    public void testRSET_Valid() {
        String response = mCi.handleInput("RSET");
        assertTrue(response.contains(OK));
    }

    @Test
    public void testRSET_ExcessiveArgs() {
        String response = mCi.handleInput("RSET abc");
        assertTrue(response.contains(ERR));
    }

    // STAT
    @Test
    public void testSTAT_Valid() {
        String response = mCi.handleInput("STAT");
        assertTrue(response.contains(OK));
    }

    @Test
    public void testSTAT_ExcessiveArgs() {
        String response = mCi.handleInput("STAT def");
        assertTrue(response.contains(ERR));
    }

    // UIDL
    @Test
    public void testUIDL_ValidArg() {
        String response = mCi.handleInput("UIDL 1");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_ValidNoArg() {
        String response = mCi.handleInput("UIDL");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_ExcessiveArgs() {
        String response = mCi.handleInput("UIDL 3 4");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_NonInt() {
        String response = mCi.handleInput("UIDL ghi");
        assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_MessagenumLessThanOne() {
        String response = mCi.handleInput("UIDL 0");
        assertTrue(response.contains(ERR));
    }

    // QUIT
    @Test
    public void testQUIT_Valid() {
        String response = mCi.handleInput("QUIT");
        assertTrue(response.contains(OK));
    }

    @Test
    public void testQUIT_ExcessiveArgs() {
        String response = mCi.handleInput("QUIT jkl");
        assertTrue(response.contains(ERR));
    }
}
