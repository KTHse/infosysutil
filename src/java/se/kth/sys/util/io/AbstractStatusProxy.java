package se.kth.sys.util.io;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractStatusProxy extends StatusProxy {

	protected abstract void notifyStatus(String string);
	protected abstract void notifyReady(String string);
	protected abstract void notifyReady();
	protected abstract void notifyStopping(String string);
	protected abstract void notifyWatchdog();

	private boolean ready = false;
	private boolean stopping = false;
	private String previousStatus = null;

	private boolean need_watchdog_update = false;
	private Timer watchdogExpirerTimer = null;

	@Override
	public void setStarting(String string) {
		if (need_watchdog_update || previousStatus == null || previousStatus.equals(string)) {
			notifyStatus(string);
			previousStatus = string;
			need_watchdog_update = false;
		}
	}

	@Override
	public void setRunning() {
		if (ready) {
			if (need_watchdog_update) {
				notifyWatchdog();
				need_watchdog_update = false;
			}
		} else {
			ready = true;
			notifyReady();
			need_watchdog_update = false;
		}
	}

	@Override
	public void setRunning(String string) {
		if (ready) {
			if (need_watchdog_update || previousStatus == null || !previousStatus.equals(string)) {
				notifyStatus(string);
				need_watchdog_update = false;
			}
		} else {
			ready = true;
			notifyReady(string);
			need_watchdog_update = false;
		}
		if (previousStatus != string)
			previousStatus = string;
	}

	@Override
	public void setStopping(String string) {
		if (stopping) {
			if (need_watchdog_update || previousStatus == null || !previousStatus.equals(string)) {
				notifyStatus(string);
				need_watchdog_update = false;
			}
		} else {
			stopping = true;
			notifyStopping(string);
			need_watchdog_update = false;
		}
		if (previousStatus != string)
			previousStatus = string;
	}

	@Override
	public void setStopped(String string) {
		notifyStopping(string);
		need_watchdog_update = false;
		previousStatus = string;
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
}
