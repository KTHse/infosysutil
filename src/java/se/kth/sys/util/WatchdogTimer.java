package se.kth.sys.util;

import java.util.Timer;
import java.util.TimerTask;

public class WatchdogTimer {

	private boolean need_watchdog_update = false;
	private Timer watchdogExpirerTimer = null;

	public WatchdogTimer(final long watchdog_msec) {
		start(watchdog_msec);
	}

	/**
	 * Enable watchdog updates on status updates.
	 * 
	 * The service must provide status updates at least every watchdog_msec milliseconds
	 * by calling any of the setStarting(), setRunning() or setStopping() methods every so often.
	 * 
	 * Not every status update will result in a watchdog update.
	 * Only when state, status text or a third of the time specified by
	 * the parameter has passed will a message be sent to the watchdog.
	 * 
	 * @param watchdog_msec Maximum amount of time between each watchdog update.
	 */
	private void start(final long watchdog_msec) {
		need_watchdog_update = true;
		watchdogExpirerTimer = new Timer();
		watchdogExpirerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				need_watchdog_update = true;
			}
		}, 0, watchdog_msec / 3);
	}

	/**
	 * Check if the watchdog timer has expired, and acknowledge.
	 * 
	 * This must only be called as a result of an explicit status update from the application.
	 * 
	 * The caller promises to ensure a watchdog update is sent promptly if this method returns true.
	 * 
	 * @return true if a watchdog update was needed, otherwise false
	 */
	public boolean dequeueUpdate() {
		if (need_watchdog_update) {
			need_watchdog_update = false;
			return true;
		}
		return false;
	}

	/**
	 * Stop the watchdog timer.
	 */
	public void stop() {
		watchdogExpirerTimer.cancel();
		watchdogExpirerTimer = null;
	}

}