package se.kth.sys.util.io;

public class StatusProxy {

	private static StatusProxy singletonStatusProxy = null;

	public static synchronized StatusProxy getStatusProxy() {
		if (singletonStatusProxy == null)
			singletonStatusProxy = SystemdNotify.createInstance();
		if (singletonStatusProxy == null)
			singletonStatusProxy = new StatusProxy();
		return singletonStatusProxy;
	}

	public void setStarting(String string) {}
	public void setRunning(String string) {}
	public void setRunning() {}
	public void setStopping(String string) {}
	public void setStopped(String string) {}

}
