package mailserver;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mockito;

public class TestCommandInterpreter {

    private static final String OK = CommandInterpreter.OK;
    private static final String CRLF = "\r\n";
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

    //////////////////////////////////////////////////////////////////////////
    //// USER
    /////////////////////////////////////////////////////////////////////////

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

    //////////////////////////////////////////////////////////////////////////
    //// PASS
    /////////////////////////////////////////////////////////////////////////

    private void executeValidUser() {
        String uname = "test";
        String request = "USER " + uname;
        Mockito.doReturn(OK).when(mDatabase).user(uname);
        mCi.handleInput(request);
    }

    private void executeValidPassword() {
        executeValidPassword("password");
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
    public void testPassMultipleArgsValid() {
        executeValidPassword("pas sw ord");
    }

    @Test
    public void testPassFailsWithoutSuccessfulUserExecuted() {
        String password = "password";
        String request = "PASS " + password;

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).pass(password);
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testPassMissingArgsReturnsError() {
        executeValidUser();
        String request = "PASS";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).pass(Mockito.anyString());
        Assert.assertTrue(response.contains(ERR));
    }

    //////////////////////////////////////////////////////////////////////////
    //// DELE
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testDeleValid() {
        executeValidPassword();

        int messageNum = 1;
        String request = "DELE " + messageNum;
        Mockito.doReturn(OK).when(mDatabase).dele(messageNum);

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).dele(messageNum);
        Assert.assertEquals(concat(OK, request), response);
    }

    @Test
    public void testDeleNegativeMessageNumberReturnsError() {
        executeValidPassword();

        int messageNum = -2;
        String request = "DELE " + messageNum;

        String expected = concat(CommandInterpreter.ERR_NEGMSGNUM, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).dele(messageNum);
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testDeleMissingArgsReturnsError() {
        executeValidPassword();
        String request = "DELE";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).dele(Mockito.anyInt());
        String expected = concat(CommandInterpreter.ERR_MISSINGARGS, request);
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testDeleExcessiveArgsReturnsError() {
        executeValidPassword();
        String request = "DELE 1 2";

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).dele(Mockito.anyInt());
        String expected = concat(CommandInterpreter.ERR_EXCESSIVEARGS, request);
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testDeleNonIntArgReturnsError() {
        executeValidPassword();
        String request = "DELE abc";

        String expected = concat(CommandInterpreter.ERR_ARGS_NON_INT, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).dele(Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    //////////////////////////////////////////////////////////////////////////
    //// LIST
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testListValidArg() {
        executeValidPassword();
        int messageNum = 1;
        String request = "LIST " + messageNum;
        Mockito.doReturn(OK).when(mDatabase).list(messageNum);

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).list(messageNum);
        Assert.assertEquals(OK + CRLF, response);
    }

    @Test
    public void testListNoArg() {
        executeValidPassword();
        String request = "LIST";
        Mockito.doReturn(OK).when(mDatabase).list(Mockito.anyInt());

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).list(Mockito.anyInt());
        Assert.assertEquals(OK + CRLF, response);
    }

    @Test
    public void testListExcessiveArgsReturnsError() {
        executeValidPassword();
        String request = "LIST 1 2";

        String expected = CommandInterpreter.ERR_EXCESSIVEARGS + CRLF;
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).list(Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testListMessagenumLessThanOneReturnsError() {

        executeValidPassword();
        int messageNum = 0;
        String request = "LIST " + messageNum;

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0)).list(messageNum);
        Assert.assertEquals(CommandInterpreter.ERR_NEGMSGNUM + CRLF, response);
    }

    @Test
    public void testListNonIntArgReturnsError() {
        executeValidPassword();

        String expected = CommandInterpreter.ERR_ARGS_NON_INT + CRLF;
        String response = mCi.handleInput("LIST xyz");

        Mockito.verify(mDatabase, Mockito.times(0)).list(Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    //////////////////////////////////////////////////////////////////////////
    //// NOOP
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testNoopValid() {
        executeValidPassword();
        String request = "NOOP";
        String response = mCi.handleInput(request);
        Assert.assertEquals(concat(OK, request), response);
    }

    @Test
    public void testNoopExcessiveArgsReturnsError() {
        executeValidPassword();
        String request = "NOOP x";

        String expected = concat(CommandInterpreter.ERR_EXCESSIVEARGS, request);
        String response = mCi.handleInput(request);

        Assert.assertEquals(expected, response);
    }

    //////////////////////////////////////////////////////////////////////////
    //// RETR
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testRetrValid() {
        executeValidPassword();
        int messageNum = 1;
        String request = "RETR " + messageNum;
        Mockito.doReturn(OK).when(mDatabase).getMessage(messageNum, -1);

        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(1)).getMessage(messageNum, -1);
        Assert.assertEquals(concat(OK, request), response);
    }

    @Test
    public void testRetrMissingArgsReturnsError() {
        executeValidPassword();
        String request = "RETR";

        String expected = concat(CommandInterpreter.ERR_MISSINGARGS, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0))
                .getMessage(Mockito.anyInt(), Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testRetrExcessiveArgsReturnsError() {
        executeValidPassword();
        String request = "RETR 1 2";

        String expected = concat(CommandInterpreter.ERR_EXCESSIVEARGS, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0))
                .getMessage(Mockito.anyInt(), Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testRetrNonIntArgReturnsError() {
        executeValidPassword();
        String request = "RETR efg";

        String expected = concat(CommandInterpreter.ERR_ARGS_NON_INT, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0))
                .getMessage(Mockito.anyInt(), Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testRetrMessagenumLessThanOneReturnsError() {
        executeValidPassword();
        String request = "RETR 0";

        String expected = concat(CommandInterpreter.ERR_NEGMSGNUM, request);
        String response = mCi.handleInput(request);

        Mockito.verify(mDatabase, Mockito.times(0))
                .getMessage(Mockito.anyInt(), Mockito.anyInt());
        Assert.assertEquals(expected, response);
    }

    //////////////////////////////////////////////////////////////////////////
    //// TOP
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testTOP_ValidArgs() {
        String response = mCi.handleInput("TOP 1 2");
        // Valid request but still returns an error because there are no
        // messages in the dummy database
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NoArgs() {
        String response = mCi.handleInput("TOP");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_ExcessiveArgs() {
        String response = mCi.handleInput("TOP 3 4 5");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NonIntArg1() {
        String response = mCi.handleInput("TOP x 2");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_NonIntArg2() {
        String response = mCi.handleInput("TOP 1 x");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_MessagenumLessThanOne() {
        String response = mCi.handleInput("TOP 0 2");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testTOP_LinecountLessThanZero() {
        String response = mCi.handleInput("TOP 1 -1");
        Assert.assertTrue(response.contains(ERR));
    }

    //////////////////////////////////////////////////////////////////////////
    //// RSET
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testRSET_Valid() {
        String response = mCi.handleInput("RSET");
        Assert.assertTrue(response.contains(OK));
    }

    @Test
    public void testRSET_ExcessiveArgs() {
        String response = mCi.handleInput("RSET abc");
        Assert.assertTrue(response.contains(ERR));
    }

    //////////////////////////////////////////////////////////////////////////
    //// STAT
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testSTAT_Valid() {
        String response = mCi.handleInput("STAT");
        Assert.assertTrue(response.contains(OK));
    }

    @Test
    public void testSTAT_ExcessiveArgs() {
        String response = mCi.handleInput("STAT def");
        Assert.assertTrue(response.contains(ERR));
    }

    //////////////////////////////////////////////////////////////////////////
    //// UIDL
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testUIDL_ValidArg() {
        String response = mCi.handleInput("UIDL 1");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_ValidNoArg() {
        String response = mCi.handleInput("UIDL");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_ExcessiveArgs() {
        String response = mCi.handleInput("UIDL 3 4");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_NonInt() {
        String response = mCi.handleInput("UIDL ghi");
        Assert.assertTrue(response.contains(ERR));
    }

    @Test
    public void testUIDL_MessagenumLessThanOne() {
        String response = mCi.handleInput("UIDL 0");
        Assert.assertTrue(response.contains(ERR));
    }

    //////////////////////////////////////////////////////////////////////////
    //// QUIT
    /////////////////////////////////////////////////////////////////////////

    @Test
    public void testQUIT_Valid() {
        String response = mCi.handleInput("QUIT");
        Assert.assertTrue(response.contains(OK));
    }

    @Test
    public void testQUIT_ExcessiveArgs() {
        String response = mCi.handleInput("QUIT jkl");
        Assert.assertTrue(response.contains(ERR));
    }
}
