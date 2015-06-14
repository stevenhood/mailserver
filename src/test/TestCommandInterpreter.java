package test;

import static org.junit.Assert.*;
import mailserver.CommandInterpreter;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

public class TestCommandInterpreter {

	static final String OK = "+OK";
	static final String ERR = "-ERR";

	CommandInterpreter ci;
	String response;

	@Before
	public void setUp() {
		ci = new CommandInterpreter();
		ci.handleInput("USER test");
		ci.handleInput("PASS password");
	}

	@After
	public void tearDown() {
		System.out.print(response);
	}

	// DELE
	@Test
	public void testDELE_Valid() {
		response = ci.handleInput("DELE 1");
		// Valid request but still returns an error because there are no
		// messages in the dummy database
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testDELE_NoArgs() {
		response = ci.handleInput("DELE");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testDELE_ExcessiveArgs() {
		response = ci.handleInput("DELE 1 2");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testDELE_NonInt() {
		response = ci.handleInput("DELE abc");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testDELE_MessagenumLessThanOne() {
		response = ci.handleInput("DELE 0");
		assertTrue(response.contains(ERR));
	}

	// LIST
	@Test
	public void testLIST_ValidArg() {
		response = ci.handleInput("LIST 1");
		// Valid request but still returns an error because there are no
		// messages in the dummy database
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testLIST_ValidNoArg() {
		response = ci.handleInput("LIST");
		assertTrue(response.contains(OK));
	}

	@Test
	public void testLIST_ExcessiveArgs() {
		response = ci.handleInput("LIST 1 2");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testLIST_NonInt() {
		response = ci.handleInput("LIST xyz");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testLIST_MessagenumLessThanOne() {
		response = ci.handleInput("LIST 0");
		assertTrue(response.contains(ERR));
	}

	// NOOP
	@Test
	public void testNOOP_Valid() {
		response = ci.handleInput("NOOP");
		assertTrue(response.contains(OK));
	}

	@Test
	public void testNOOP_ExcessiveArgs() {
		response = ci.handleInput("NOOP x");
		assertTrue(response.contains(ERR));
	}

	// RETR
	@Test
	public void testRETR_Valid() {
		response = ci.handleInput("RETR 1");
		// Valid request but still returns an error because there are no
		// messages in the dummy database
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testRETR_NoArgs() {
		response = ci.handleInput("RETR");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testRETR_ExcessiveArgs() {
		response = ci.handleInput("RETR 1 2");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testRETR_NonInt() {
		response = ci.handleInput("RETR efg");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testRETR_MessagenumLessThanOne() {
		response = ci.handleInput("RETR 0");
		assertTrue(response.contains(ERR));
	}

	// TOP
	@Test
	public void testTOP_ValidArgs() {
		response = ci.handleInput("TOP 1 2");
		// Valid request but still returns an error because there are no
		// messages in the dummy database
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_NoArgs() {
		response = ci.handleInput("TOP");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_ExcessiveArgs() {
		response = ci.handleInput("TOP 3 4 5");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_NonIntArg1() {
		response = ci.handleInput("TOP x 2");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_NonIntArg2() {
		response = ci.handleInput("TOP 1 x");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_MessagenumLessThanOne() {
		response = ci.handleInput("TOP 0 2");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testTOP_LinecountLessThanZero() {
		response = ci.handleInput("TOP 1 -1");
		assertTrue(response.contains(ERR));
	}

	// RSET
	@Test
	public void testRSET_Valid() {
		response = ci.handleInput("RSET");
		assertTrue(response.contains(OK));
	}

	@Test
	public void testRSET_ExcessiveArgs() {
		response = ci.handleInput("RSET abc");
		assertTrue(response.contains(ERR));
	}

	// STAT
	@Test
	public void testSTAT_Valid() {
		response = ci.handleInput("STAT");
		assertTrue(response.contains(OK));
	}

	@Test
	public void testSTAT_ExcessiveArgs() {
		response = ci.handleInput("STAT def");
		assertTrue(response.contains(ERR));
	}

	// UIDL
	@Test
	public void testUIDL_ValidArg() {
		response = ci.handleInput("UIDL 1");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testUIDL_ValidNoArg() {
		response = ci.handleInput("UIDL");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testUIDL_ExcessiveArgs() {
		response = ci.handleInput("UIDL 3 4");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testUIDL_NonInt() {
		response = ci.handleInput("UIDL ghi");
		assertTrue(response.contains(ERR));
	}

	@Test
	public void testUIDL_MessagenumLessThanOne() {
		response = ci.handleInput("UIDL 0");
		assertTrue(response.contains(ERR));
	}

	// QUIT
	@Test
	public void testQUIT_Valid() {
		response = ci.handleInput("QUIT");
		assertTrue(response.contains(OK));
	}

	@Test
	public void testQUIT_ExcessiveArgs() {
		response = ci.handleInput("QUIT jkl");
		assertTrue(response.contains(ERR));
	}
}
