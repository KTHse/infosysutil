package se.kth.sys.util.io;

import java.io.IOException;

import se.kth.sys.util.RateLimitTimer;
import se.kth.sys.util.WatchdogTimer;

/**
 * The sd_notify compatible implementation of StatusProxy.
 * 
 * @author Alexander Boström &lt;abo@kth.se&gt;
 *
 */
public class SystemdNotify extends StatusProxy {
	private boolean sendReady = false, sentReady = false;
	private boolean sendStopping = false, sentStopping = false;
	private String statusText = null, sentStatusText = null;

	private WatchdogTimer watchdogTimer;

	private Thread senderThread;
	private boolean closed = false;

	private RateLimitTimer rateLimitTimer;

	private String watchdogargs = null;
	private UnixSocket socket;

	/**
	 * Initializes and returns an instance of SystemdNotify if the matching
	 * environment variables are found, else returns null.
	 * 
	 * @return an instance of SystemdNotify or null
	 */
	protected static SystemdNotify createInstance() {
		if (System.getenv().containsKey("NOTIFY_SOCKET")) {
			String socket = System.getenv("NOTIFY_SOCKET");
			final SystemdNotify systemdNotify = new SystemdNotify();

			if (System.getenv().containsKey("WATCHDOG_USEC")) {
				String usec = System.getenv("WATCHDOG_USEC");
				systemdNotify.watchdogargs = "WATCHDOG=1\n";
				if (System.getenv().containsKey("WATCHDOG_PID")) {
					String pid = System.getenv("WATCHDOG_PID");
					String mypid = System.getProperty("watchdog.pid");
					if (mypid != null && mypid.equals(pid))
						systemdNotify.watchdogargs = "MAINPID=" + mypid + "\n" + systemdNotify.watchdogargs;
				}
				systemdNotify.watchdogTimer = new WatchdogTimer(new Long(usec) / 1000);
			}

			try {
				systemdNotify.connect(socket);
			} catch (IOException e) {
				// TODO Automatically generated catch block
				e.printStackTrace();
				return null;
			}
			systemdNotify.rateLimitTimer = new RateLimitTimer(1000);
			systemdNotify.startSenderThread();
			return systemdNotify;
		}
		return null;
	}

	/**
	 * Generates a notification string if the state (ready or stopping) has changed.
	 * 
	 * @return a String in sd_notify format or null
	 */
	private String dequeueState() {
		if (dequeueStoppingStateNotification())
			return "STOPPING=1\n";
		if (dequeueRunningStateNotification())
			return "READY=1\n";
		return null;
	}

	/**
	 * Generates a notification string if the status text has changed.
	 * 
	 * @return a String in sd_notify format or null
	 */
	private String dequeueStatus() {
		if (dequeueStatusNotification())
			return "STATUS=" + sentStatusText + "\n";
		return null;
	}

	/**
	 * Generates a notification string if any state has changed or if watchdogargs is set.
	 * 
	 * @return a String in sd_notify format or null
	 */
	private String dequeueNotification() {
		String newStatus = dequeueStatus();
		String newState = dequeueState();

		// Send a single message with the given information and a watchdog notification (if enabled).
		if (newStatus != null || newState != null || watchdogargs != null)
			return (newStatus != null ? newStatus : "") + (newState != null ? newState : "") + (watchdogargs != null ? watchdogargs : "");

		return null;
	}

	/**
	 * Connect to the named socket and sets is as the socket used to report status.
	 * 
	 * @param socketfile path to the socket file
	 * @throws IOException
	 */
	private void connect(String socketfile) throws IOException {
		socket = new UnixSocket();
		socket.connect(socketfile);
	}

	/**
	 * Send a string to the status socket.
	 * 
	 * @param s string to send
	 */
	private void send(String s) {
		socket.send(s);
	}

	/**
	 * Close the status socket.
	 */
	private void close() {
		socket.close();
	}

	/**
	 * Waits for changes in state or an update from the service after the watchdog timer has expired.
	 * Builds a notification string and returns it. If the current state has been sent and notifications
	 * have ended, null is returned.
	 * 
	 * @return a String in sd_notify format, or null
	 */
	private String waitForNotification() {
		while (true) {
			// A Java object notification should come only as a result of a status update from the service.
			try {
				senderThread.wait();
			} catch (InterruptedException e) {}
			
			// Possibly wait a bit longer, while releasing the lock so notifications can be sent to senderThread.
			rateLimitTimer.waitOn(senderThread);

			// We have a notification to senderThread, return the latest status notification.
			String toSend = dequeueNotification();
			if (toSend != null || closed)
				return toSend;
		}
	}

	/**
	 * Waits for changes in state or an update from the service after the watchdog timer has expired.
	 * Builds a notification string and sends it. If the current state has been sent and notifications
	 * have ended, false is returned, otherwise true.
	 * 
	 * @return true if the method should be called again, otherwise false
	 */
	private boolean waitAndSendNotification() {
		String notificationString = waitForNotification();
		if (notificationString == null)
			return false;
	
		// Actually send the string to the socket.
		send(notificationString);
		rateLimitTimer.register();
		return true;
	}

	/**
	 * Starts a thread which sends notifications when required and allowed.
	 */
	private void startSenderThread() {
		senderThread = new Thread() {
			@Override
			public void run() {
				synchronized (senderThread) {
					try {
						while (waitAndSendNotification())
							{};
					} catch (Exception e) {
						// The watchdog will receive no more notifications after this.
						e.printStackTrace();
					} finally {
						closed = true;
						close();
					}
				}
			}
		};
		senderThread.start();
	}

	/**
	 * Signals the notification thread started by startSenderThread() to stop and waits the given
	 * amount of time until all notifications have been sent.
	 * 
	 * @param timeout number of milliseconds to wait
	 */
	private void stopSenderThread(long timeout) {
		closed = true;
		synchronized (senderThread) {
			senderThread.notifyAll();
		}
		try {
			senderThread.join(timeout);
		} catch (InterruptedException e) {}
	}

	/**
	 * Generate a notification if there are updates needed to be sent.
	 * 
	 * This method must return quickly.
	 */
	private void queueNotification() {
		if (watchdogTimer.dequeueUpdate() || hasQueuedNotifications()) {
			synchronized (senderThread) {
				senderThread.notifyAll();
			}
		}
	}

	/**
	 * Signals notifications should cease, and waits a small amount of time for the last one to be sent.
	 */
	private void endNotifications() {
		watchdogTimer.stop();
		stopSenderThread(rateLimitTimer.getLimit() * 2);
	}

	/**
	 * Query of changed state needs to be sent.
	 * 
	 * @return true if there are updates needed to be sent, otherwise false
	 */
	private boolean hasQueuedNotifications() {
		return sentReady != sendReady || sentStopping != sendStopping || (statusText != null && (sentStatusText == null || !statusText.equals(sentStatusText)));
	}

	/**
	 * Check if the current running state has been sent, and acknowledges sending it if not.
	 * 
	 * @return true if the running state has not been sent, otherwise false
	 */
	private boolean dequeueRunningStateNotification() {
		if (sentReady != sendReady) {
			sentReady = true;
			return true;
		}
		return false;
	}

	/**
	 * Check if the current stopping state has been sent, and acknowledges sending it if not.
	 * 
	 * @return true if the stopping state has not been sent, otherwise false
	 */
	private boolean dequeueStoppingStateNotification() {
		if (sentStopping != sendStopping) {
			sendReady = true; sentReady = true; // or it's too late anyway
			sentStopping = true;
			return true;
		}
		return false;
	}

	/**
	 * Check if the current status text has been sent, and acknowledges sending it if not.
	 * 
	 * @return true if the status text has not been sent, otherwise false
	 */
	private boolean dequeueStatusNotification() {
		if (statusText != null && (sentStatusText == null || !statusText.equals(sentStatusText))) {
			sentStatusText = statusText;
			return true;
		}
		return false;
	}

	@Override
	public void setStarting(String string) {
		statusText = string;
		queueNotification();
	}

	@Override
	public void setRunning() {
		sendReady = true;
		queueNotification();
	}

	@Override
	public void setRunning(String string) {
		sendReady = true;
		statusText = string;
		queueNotification();
	}

	@Override
	public void setStopping(String string) {
		sendStopping = true;
		statusText = string;
		queueNotification();
	}

	@Override
	public void setStopped(String string) {
		setStopping(string);
		endNotifications();
	}

}
