package se.kth.sys.util.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class StatusFile extends StatusProxy {
	private static final String STARTING = "STARTING", READY = "READY", STOPPING = "STOPPING";
	private String serviceState = null, statusText = null;
	private int counter = 0;

	private File statusFile = null;

	private boolean need_watchdog_update = false;
	private Timer watchdogExpirerTimer = null;

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
		watchdogExpirerTimer.cancel();
	}

	protected void startWatchdogUpdateTimer(final long watchdog_msec) {
		need_watchdog_update = true;
		watchdogExpirerTimer = new Timer();
		watchdogExpirerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				need_watchdog_update = true;
			}
		}, 0, watchdog_msec / 3);
	}

	protected static StatusFile createInstance() {
		if (System.getenv().containsKey("STATUS_FILE")) {
			String statusfilename = System.getenv("STATUS_FILE");
			final StatusFile instance = new StatusFile();

			if (System.getenv().containsKey("WATCHDOG_SEC")) {
				String sec = System.getenv("WATCHDOG_SEC");
				instance.startWatchdogUpdateTimer(new Long(sec) * 1000);
			}

			instance.statusFile = new File(statusfilename);
			try {
				instance.writeStatusString("");
			} catch (IOException e) {
				// TODO Automatiskt genererat catch-block
				e.printStackTrace();
			}
			return instance;
		}
		return null;
	}

	private void writeStatusString(String string) throws IOException {
		FileWriter writer = new FileWriter(statusFile, false);
		try {
			writer.write(string);
		} finally {
			writer.close();
		}
	}

	private void writeStatus() {
		String statusstring = serviceState + " " + Integer.toString(counter++) + " " + statusText;
		if (counter >= 1048576)
			counter = 0;
		try {
			writeStatusString(statusstring);
		} catch (IOException e) {}
		need_watchdog_update = false;
	}

	private void writeStatus(String s) {
		statusText = s;
		writeStatus();
	}

	private void writeOldStatus() {
		if (need_watchdog_update)
			writeStatus();
	}

	private void writeOldStatus(String s) {
		if (need_watchdog_update || statusText == null || !statusText.equals(s))
			writeStatus(s);
	}

}
