package se.kth.sys.util.io;

/**
 * Framework for reporting the status of a running service to a supervisor.
 * 
 * This is intended to be used by services, especially non-web-services, to report
 * their current status to a service provided by the environment the service
 * runs in, to acknowledge startup completion, send information about progress and
 * errors and to periodically update watchdog timestamps.
 * 
 *  Currently implemented:
 *   SystemdNotify: sd_notify support
 *   StatusFile: Write the current status to a file.
 *
 * Services should call getStatusProxy() and
 * then call setStarting(s) one or more times,
 * followed by setRunning(s) at least once,
 * followed by setRunning(s) or setRunning() periodically,
 * until setStopping(s) and setStopped(s) are called.
 * 
 * @author Alexander Boström &lt;abo@kth.se&gt;
 *
 */
public class StatusProxy {

	private static StatusProxy singletonStatusProxy = null;

	/**
	 * The entry point for this framework.
	 * If run in an sd_notify compatible environment, this
	 * returns a SystemdNotify object.
	 * If STATUS_FILE is set, this returns a StatusFile.
	 * Otherwise a no-op StatusProxy object is returned.
	 *
	 * @return a StatusProxy matching the current environment
	 */
	public static synchronized StatusProxy getStatusProxy() {
		if (singletonStatusProxy == null)
			singletonStatusProxy = SystemdNotify.createInstance();
		if (singletonStatusProxy == null)
			singletonStatusProxy = StatusFile.createInstance();
		if (singletonStatusProxy == null)
			singletonStatusProxy = new StatusProxy();
		return singletonStatusProxy;
	}

	/**
	 * Update the status during the startup sequence,
	 * but do not acknowledge startup completion.
	 *
	 * @param string Describes the current status
	 */
	public void setStarting(String string) {}

	/**
	 * Acknowledge startup completion (if not already done)
	 * and update the status text.
	 *
	 * @param string Describes the current status
	 */
	public void setRunning(String string) {}

	/**
	 * Acknowledge startup completion (if not already done).
	 */
	public void setRunning() {}

	/**
	 * Inform progress on service shutdown.
	 * 
	 * @param string Describes the current status
	 */
	public void setStopping(String string) {}

	/**
	 * Acknowledge completion of service shutdown.
	 * 
	 * @param string Describes the current status
	 */
	public void setStopped(String string) {}

}
