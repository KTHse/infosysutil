package se.kth.sys.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tool to generate a monitor response for diagnostics.
 * 
 * This is intended to be used by multiple applications following
 * the convention used by monitoring tools at KTH. The end result 
 * is a report like
 * <pre>
 * APPLICATION_STATUS: ERROR Sub-components are broken. 2
 * SCHEMA_BACKEND: ERROR Timeout executing test java.util.concurrent.TimeoutException
 * LOCAL_DB_CONNECTION: OK The sysdate is 2010-09-10.
 * LADOK_DB_CONNECTION: ERROR Exception in Ladok database test: java.sql.SQLException: Failed to initialize db connection pool to jdbc:mysql://ture.umdc.umu.se/kth?useSSL=true&requireSSL=true for the MimerDataLayer as ita_pelube: java.sql.SQLException: Access denied for user 'ita_pelube'@'pelu-mabop.ite.kth.se' (using password: YES)
 * </pre>
 * This is created by adding a number of checks that return a <code>Status</code>. In the above
 * example there are three checks provided, this is then summarized in to the total
 * application status. Each check is run independently and with a timeout.
 * 
 * @author peter.lundberg
 */
public class ApplicationMonitor {

	/**
	 * Represents the result of a test, basically a immutable tuple of boolean, string.
	 */
	public static final class Status {
        private final boolean isOk;
        private final String message;

        public static Status OK(String message) {
            return new Status(true, message);
        }

        public static Status ERROR(String message) {
            return new Status(false, message);
        }

        private Status(boolean isOk, String message) {
            this.isOk = isOk;
            this.message = message;
        }

        public String toString() {
            return (isOk ? "OK" : "ERROR") + " " + message;
        }
    }

	private ExecutorService executorService = Executors.newCachedThreadPool(); //unbounded
	private Map<String, Future<Status>> testFutures = new HashMap<String, Future<Status>>();
	private List<String> checkOrder = new ArrayList<String>();
	private int maxReportTimeSecs;
	
	public ApplicationMonitor() {
		this(15); // defaults to 15 seconds
	}	
	
	public ApplicationMonitor(int maxReportTimeSecs) {
		this.maxReportTimeSecs = maxReportTimeSecs;
	}
	
	public void addCheck(String statusName, Callable<Status> callable) {
		if (testFutures.containsKey(statusName)) {
			throw new IllegalArgumentException("Implicit redefintion of exiting key '" + 
					statusName + "' not allowed.");
			// if this should really be needed, create a seperate explicit remove method
			// this will catch simple errors
		}
		checkOrder.add(statusName);
		testFutures.put(statusName, executorService.submit(callable));
	}

	/**
	 * get
	 * @return
	 * @throws IOException
	 */
	public String createMonitorReport() throws IOException {
		
		// get the results from all tests (with timeouts)
		StringWriter detailedResults = new StringWriter();
		int errors = checkAllFutures(detailedResults);

		// Output
		StringWriter totalResult = new StringWriter();
		totalResult.append("APPLICATION_STATUS: ");
		if (errors > 0) {
			String message = "Sub-components are broken. " + errors;
	        totalResult.append(Status.ERROR(message).toString());
		} else if (testFutures.size() > 0) {
			String message = "Every component is working";
	        totalResult.append(Status.OK(message).toString());
		} else {
			String message = "No checks configured";
	        totalResult.append(Status.ERROR(message).toString());
		}
		totalResult.append("\n");
		totalResult.append(detailedResults.getBuffer());
		
		//Cleanup
		executorService.shutdown();
		
		return totalResult.toString();
	}

	private int checkAllFutures(Writer sw) throws IOException {
		final long startTS = System.currentTimeMillis();
		int errors = 0; 
		// For each entered check
		for (String checkName : checkOrder) {
			// Get the result, with a time out
			// Note the timeout is per test, so max total time is timeout * nr checks
        	Status result;
			try {
				Future<Status> future = this.testFutures.get(checkName);
				long timeout = startTS + maxReportTimeSecs*1000L - System.currentTimeMillis();
				if (timeout < 0) timeout = 0;
				result = future.get(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
        		result = Status.ERROR("Interupted executing test " + e);
			} catch (ExecutionException e) {
        		result = Status.ERROR("Exception executing test " + e);
			} catch (TimeoutException e) {
				long elapsed = System.currentTimeMillis() - startTS;
        		result = Status.ERROR("Timeout executing test after " + elapsed);
			}
			// remember total nr errors
            if (!result.isOk) {
            	errors++;
            }
            // Format human and script readable output message
            sw.append(checkName);
            sw.append(": ");
            sw.append(result.toString().replace("\n", "\\n"));
            sw.append("\n");
        }
		return errors;
	}
	

}
