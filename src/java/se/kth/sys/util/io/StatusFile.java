package se.kth.sys.util.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import se.kth.sys.util.WatchdogTimer;

/**
 * A simple implementation of StatusProxy which writes the current status to
 * the file provided by the STATUS_FILE environment variable, in the form
 * 
 * (STARTING|READY|STOPPING) <counter> <status text>
 * 
 * The reader of the file should consider a change in the content of the file as
 * an update of the watchdog timestamp.
 * 
 * @author Alexander Boström &lt;abo@kth.se&gt;
 *
 */
public class StatusFile extends StatusProxy {
	private static final String STARTING = "STARTING", READY = "READY", STOPPING = "STOPPING";
	private String serviceState = null, statusText = null;
	private int counter = 0;

	private WatchdogTimer watchdogTimer;

	private File statusFile = null;

	@Override
	public void setStarting(String text) {
		serviceState = STARTING;
		writeOldStatus(text);
	}

	@Override
	public void setRunning() {
		if (serviceState == READY) {
			writeOldStatus();
		} else {
			serviceState = READY;
			writeStatus();
		}
	}

	@Override
	public void setRunning(String text) {
		if (serviceState == READY) {
			writeOldStatus(text);
		} else {
			serviceState = READY;
			writeStatus(text);
		}
	}

	@Override
	public void setStopping(String text) {
		if (serviceState == STOPPING) {
			writeOldStatus(text);
		} else {
			serviceState = STOPPING;
			writeStatus(text);
		}
	}

	@Override
	public void setStopped(String text) {
		serviceState = STOPPING;
		writeStatus(text);
		watchdogTimer.stop();
	}

	/**
	 * Initializes and returns an instance of StatusFile if the matching
	 * environment variables are found and initialization succeeds, else returns null.
	 * 
	 * @return an instance of StatusFile or null
	 */
	protected static StatusFile createInstance() {
		if (System.getenv().containsKey("STATUS_FILE")) {
			String statusfilename = System.getenv("STATUS_FILE");
			final StatusFile instance = new StatusFile();

			if (System.getenv().containsKey("WATCHDOG_SEC")) {
				String sec = System.getenv("WATCHDOG_SEC");
				instance.watchdogTimer = new WatchdogTimer(new Long(sec) * 1000);
			}

			instance.statusFile = new File(statusfilename);
			try {
				instance.writeStatusString("");
			} catch (IOException e) {
				// TODO Automatiskt genererat catch-block
				e.printStackTrace();
				return null;
			}
			return instance;
		}
		return null;
	}

	/**
	 * Write the given string to the status file.
	 * 
	 * @param string
	 * @throws IOException
	 */
	private void writeStatusString(String string) throws IOException {
		FileWriter writer = new FileWriter(statusFile, false);
		try {
			writer.write(string);
		} finally {
			writer.close();
		}
	}

	/**
	 * Generate the content of the status file and write it.
	 */
	private void writeStatus() {
		String statusstring = serviceState + " " + Integer.toString(counter++) + " " + statusText;
		if (counter >= 1048576)
			counter = 0;
		try {
			writeStatusString(statusstring);
		} catch (IOException e) {}
	}

	/**
	 * Generate the content of the status file and write it,
	 * given a new status text.
	 * 
	 * @param s describes the current status
	 */
	private void writeStatus(String s) {
		statusText = s;
		writeStatus();
	}

	/**
	 * Call writeStatus() if a watchdog update is needed.
	 */
	private void writeOldStatus() {
		if (watchdogTimer.dequeueUpdate())
			writeStatus();
	}

	/**
	 * Call writeStatus() if a watchdog update is needed or the status
	 * text has changed.
	 * 
	 * @param s describes the current status
	 */
	private void writeOldStatus(String s) {
		if (watchdogTimer.dequeueUpdate() || statusText == null || !statusText.equals(s))
			writeStatus(s);
	}

}
