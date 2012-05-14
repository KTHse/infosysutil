package se.kth.sys.util.lang;

import static org.junit.Assert.*;
import static se.kth.sys.util.TestUtil.assertContains;

import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
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
        if (! c1.testingGetCmdLineStringList().equals(c2.testingGetCmdLineStringList()))
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
    public void testExecuteAndWait() throws IOException, InterruptedException {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"echo", "foo"});
        c.executeAndWait();
        // TODO: Check that we actually got "foo" echoed!
    }

    @Test
    public void testSetPassword() throws IOException, InterruptedException {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"foo", "XXX"});
        c.setPassword(1, "gazonk");
        c.prepend("echo");
        c.append("bar");
        c.enableStdOutStore();
        c.executeAndWait();

        assertEquals("foo gazonk bar", c.getStdOutStore().get(0));
    }

    @Test
    public void testGetExitCodeZero() throws IOException, InterruptedException {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"echo", "foo"});
        c.executeAndWait();

        assertEquals(0, c.getExitCode());
    }

    @Test
    public void testGetExitCodeNonZero() throws IOException, InterruptedException {
      SystemCommandHandler c = new SystemCommandHandler(new String[]{"false"});
      c.executeAndWait();

      assertEquals(1, c.getExitCode());
    }

    /* don't know how to generate these */
//  @Test
//  public void testGetStdOutIOException() {
//  fail("Not yet implemented"); // TODO
//  }
//  @Test
//  public void testGetStdErrIOException() {
//  fail("Not yet implemented"); // TODO
//  }

    @Test
    public void testStdIO() {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"false"});
        c.enableStdOutStore();
        c.enableStdErrStore();
        c.receiveLine(SystemCommandHandler.StreamId.STDOUT, "out");
        c.receiveLine(SystemCommandHandler.StreamId.STDERR, "err");
        if (! c.getStdOutStore().get(0).equals("out"))
            fail("did not catch stdout");
        if (! c.getStdErrStore().get(0).equals("err"))
            fail("did not catch stderr");
    }

    @Test
    public void testToString() {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"cat", "my file"});
        // The output should be quoted just like so:
        assertEquals("Command line: cat \"my file\"", c.toString());
    }

    @Ignore("Fails to load classes in hudson") @Test
    public void testAsEscapedString() {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"sed", "-e", "s/\\\\/\\//g;", "somefile"});
        assertEquals("sed -e s/\\\\\\\\/\\\\//g\\; somefile", c.asEscapedString());
    }

    @Test
    public void testSetDirectory() throws IOException, InterruptedException {
      SystemCommandHandler c = new SystemCommandHandler(new String[]{"pwd"});
      c.setDirectory(new File("/tmp"));
      c.enableStdOutStore();
      c.executeAndWait();

      assertEquals("/tmp", c.getStdOutStore().get(0));
    }

    @Ignore("Fails to load classes in hudson") @Test
    public void testEnvironment() throws IOException, InterruptedException {
        SystemCommandHandler c = new SystemCommandHandler(new String[]{"env"});
        c.environment().put("FOO", "bar");
        c.enableStdOutStore();
        c.executeAndWait();

        assertContains("FOO=bar", c.getStdOutStore());
    }

}
