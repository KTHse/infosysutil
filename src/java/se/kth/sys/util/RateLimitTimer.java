package se.kth.sys.util;

/**
 * A timer for limiting the frequency of an operation.
 * 
 * register() and waitFor() can be called separately if other work is done in between.
 * 
 * delay() simply sleeps the required amount of time.
 * 
 * @author abo
 *
 */
public class RateLimitTimer {
	private SimpleTimer lastRegister = null;
	private long limit;

	public RateLimitTimer(long millis) {
		limit = millis;
	}

	public long getLimit() {
		return limit;
	}

	/**
	 * Sets a timestamp used by waitFor().
	 */
	public void register() {
		lastRegister = new SimpleTimer();
	}

	/**
	 * obj.wait() until at least one second has passed since register() was called.
	 */
	public void waitOn(Object obj) {
		if (lastRegister != null) {
			do {
				long sleeptime = limit - lastRegister.elapsed();
				if (sleeptime > 0)
					try {
						obj.wait(sleeptime);
					} catch (InterruptedException e) {}
				else
					break;
			} while (true);
		}
	}

}
