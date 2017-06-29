package se.kth.sys.util.lang;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import se.kth.sys.util.io.LineReceiver;
import se.kth.sys.util.io.ReadLineThread;

public class SystemCommandHandler implements LineReceiver {

    protected LinkedList<String> commandline;
    private int passwordindex = 0;
    private String passwordval;
    private List<String> stdOutStore = null;
    private List<String> stdErrStore = null;
    private int exitCode = -1;
    private Process process;
    protected ReadLineThread outlog;
    protected ReadLineThread errlog;
    private Map<String, String> env = null;
    private File dir = null;

    protected enum StreamId {
        STDOUT,
        STDERR;
    }

    public SystemCommandHandler(LinkedList<String> commandline) {
        this.commandline = commandline;
    }

    public SystemCommandHandler(String[] commandline) {
        this(new LinkedList<String>(Arrays.asList(commandline)));
    }

    public void prepend(String string) {
        passwordindex++;
        commandline.addFirst(string);
    }

    public void prepend(String[] strings) {
        for (int i = strings.length - 1; i >= 0; i--) {
            prepend(strings[i]);
        }
    }

    public void append(String string) {
        commandline.add(string);
    }

    public void setPassword(int index, String value) {
        passwordindex = index;
        passwordval = value;
    }

    /**
     * Executes the command without any input and waits for it to complete.
     * Afterwards you need to call getExitCode(), getStdOutIOException() and getStdErrIOException().
     * @throws IOException
     * @throws InterruptedException
     */
    public void executeAndWait() throws IOException, InterruptedException {
        execute();
        getOutputStream().close();
        joinProcess();
    }

    /**
     * Executes the command.
     * Afterwards you need to call getOutputStream().close(), joinProcess(), getExitCode(), getStdOutIOException() and getStdErrIOException().
     * @throws IOException
     */
    public void execute() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(getPwCommandLine(), getEnvP(), dir);

        outlog = new ReadLineThread(process.getInputStream(), StreamId.STDOUT, this);
        errlog = new ReadLineThread(process.getErrorStream(), StreamId.STDERR, this);

        outlog.start();
        errlog.start();
    }

    /**
     * Returns the stream used to provide input to the process.
     * You need to close() it and then call joinProcess().
     * Afterwards you need to call getExitCode(), getStdOutIOException() and getStdErrIOException().
     */
    public OutputStream getOutputStream() {
        return process.getOutputStream();
    }

    /**
     * Waits for the process to complete.
     * Afterwards you need to call getExitCode(), getStdOutIOException() and getStdErrIOException().
     * @throws IOException
     * @throws InterruptedException
     */
    public void joinProcess() throws InterruptedException, IOException {
        exitCode = process.waitFor();

        // Wait for all output to be logged.
        outlog.join();
        errlog.join();

        process.getInputStream().close();
        process.getErrorStream().close();
    }

    public int getExitCode() {
        return exitCode;
    }

    /**
     * @return
     */
    public IOException getStdOutIOException() {
        return outlog.getIOException();
    }

    /**
     * @return
     */
    public IOException getStdErrIOException() {
        return errlog.getIOException();
    }

    public void receiveLine(Object bufferid, String line) {
        storeStdioLine(bufferid, line);
    }

    public void enableStdOutStore() {
        if (stdOutStore == null) {
            stdOutStore = new LinkedList<String>();
        }
    }

    public void enableStdErrStore() {
        if (stdErrStore == null) {
            stdErrStore = new LinkedList<String>();
        }
    }

    public List<String> getStdOutStore() {
        return stdOutStore;
    }

    public List<String> getStdErrStore() {
        return stdErrStore;
    }

    /**
     * @param streamid
     * @param s
     */
    protected void storeStdioLine(Object streamid, String s) {
        if (StreamId.STDOUT == streamid) {
            if (stdOutStore != null) {
                stdOutStore.add(s);
            }
        }
        if (StreamId.STDERR == streamid) {
            if (stdErrStore != null) {
                stdErrStore.add(s);
            }
        }
    }

    public List<String> testingGetCmdLineStringList() {
        return commandline;
    }

    @Override
    public String toString() {
        return "Command line: " + asEscapedString();
    }

    public String asEscapedString() {
        return arrayToEscapedCommandLineString(commandline);
    }

    /**
     * Returns an escaped string representing the given array of string or null
     * if the array is empty.
     */
    private static String arrayToEscapedCommandLineString(Iterable<String> array) {
        // this is lazy and probably wrong
        String ls = null;
        for (String s : array) {
            s = java.util.regex.Matcher.quoteReplacement(s);
            s = s.replaceAll(";", java.util.regex.Matcher.quoteReplacement("\\;"));
            if (s.indexOf(" ") >= 0) {
                s = "\"" + s + "\"";
            }
            if (ls == null) {
                ls = s;
            } else {
                ls += " " + s;
            }
        }
        return ls;
    }

    public void setDirectory(File directory) {
        this.dir = directory;
    }

    public Map<String, String> environment() {
        if (env == null) {
            env = new HashMap<String, String>(System.getenv());
        }
        return env;
    }

    /**
     * @return
     */
    private String[] getEnvP() {
        String[] envp = null;
        if (env != null) {
            envp = new String[env.size()];
            int i = 0;
            for (Entry<String, String> entry : env.entrySet()) {
                envp[i++] = entry.getKey() + "=" + entry.getValue();
            }
        }
        return envp;
    }

    /**
     * @return
     */
    private String[] getPwCommandLine() {
        String[] argv = commandline.toArray(new String[commandline.size()]);
        if (passwordval != null) {
            argv[passwordindex] = passwordval;
        }
        return argv;
    }

}
