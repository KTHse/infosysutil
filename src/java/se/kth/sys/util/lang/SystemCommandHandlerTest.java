package se.kth.sys.util.lang;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class SystemCommandHandlerTest {

	@Test
	public void testPrependString() {
		SystemCommandHandler c1 = new SystemCommandHandler(new String[]{"foo", "bar"});
		SystemCommandHandler c2 = new SystemCommandHandler(new String[]{"bar"});
		c2.prepend("foo");
		if (! c1.testingGetCmdLineStringList().equals(c2.testingGetCmdLineStringList()))
			fail("prepend(String) did the wrong thing");
	}

	@Test
	public void testPrependStringArray() {
		SystemCommandHandler c1 = new SystemCommandHandler(new String[]{"foo", "bar", "baz"});
		SystemCommandHandler c2 = new SystemCommandHandler(new String[]{"baz"});
		c2.prepend(new String[]{"foo", "bar"});
		if (! c1.commandline.equals(c2.commandline))
			fail("prepend(String[]) did the wrong thing");
	}

	@Test
	public void testAppend() {
		SystemCommandHandler c1 = new SystemCommandHandler(new String[]{"foo", "bar", "baz"});
		SystemCommandHandler c2 = new SystemCommandHandler(new String[]{"foo"});
		c2.append("bar");
		c2.append("baz");
		if (! c1.testingGetCmdLineStringList().equals(c2.testingGetCmdLineStringList()))
			fail("append() did the wrong thing");
	}

	@Test
	public void testExecuteAndWait() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"echo", "foo"});
		try {
			c.executeAndWait();
		} catch (IOException e) {
			fail("IOException caught: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("InterruptedException caught: " + e.getMessage());
		}
	}

	@Test
	public void testSetPassword() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"foo", "XXX"});
		c.setPassword(1, "gazonk");
		c.prepend("echo");
		c.append("bar");
		c.enableStdOutStore();
		try {
			c.executeAndWait();
		} catch (IOException e) {
			fail("IOException caught: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("InterruptedException caught: " + e.getMessage());
		}
		if (! c.getStdOutStore().get(0).equals("foo gazonk bar"))
			fail("setPassword() handled wrong");
	}

	@Test
	public void testGetExitCodeZero() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"echo", "foo"});
		try {
			c.executeAndWait();
		} catch (IOException e) {
			fail("IOException caught: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("InterruptedException caught: " + e.getMessage());
		}
		if (c.getExitCode() != 0)
			fail("Executing 'echo foo' did not return exit code 0.");
	}

	@Test
	public void testGetExitCodeNonZero() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"false"});
		try {
			c.executeAndWait();
		} catch (IOException e) {
			fail("IOException caught: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("InterruptedException caught: " + e.getMessage());
		}
		if (c.getExitCode() != 1)
			fail("Executing 'false' did not return exit code 1.");
	}

	/* don't know how to generate these */
//	@Test
//	public void testGetStdOutIOException() {
//		fail("Not yet implemented"); // TODO
//	}
//	@Test
//	public void testGetStdErrIOException() {
//		fail("Not yet implemented"); // TODO
//	}

	@Test
	public void testStdIO() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"false"});
		c.enableStdOutStore();
		c.enableStdErrStore();
		c.receiveLine(SystemCommandHandler.STDOUT, "out");
		c.receiveLine(SystemCommandHandler.STDERR, "err");
		if (! c.getStdOutStore().get(0).equals("out"))
			fail("did not catch stdout");
		if (! c.getStdErrStore().get(0).equals("err"))
			fail("did not catch stderr");
	}

	@Test
	public void testToString() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"cat", "my file"});
		if (! c.toString().equals("Command line: cat \"my file\""))
			fail("command was escaped the wrong way");
	}

	@Test
	public void testAsEscapedString() {
		SystemCommandHandler c = new SystemCommandHandler(new String[]{"sed", "-e", "s/\\\\/\\//g;", "somefile"});
		if (! c.asEscapedString().equals("sed -e s/\\\\\\\\/\\\\//g\\; somefile"))
			fail("command was escaped the wrong way");
	}

}
