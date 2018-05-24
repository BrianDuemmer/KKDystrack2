package com.dystify.kkdystrack.v2.service;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The task queue systemmManages tasks designated to be executed on the database. 
 * The need for this arises from the issue of keeping DB operations 
 * synchronized. They generally need to follow a predefined order; if not, 
 * either database or UI elements could become clobbered. the original solution
 * to this was to simply run database operations in the javaFX trhead.
 * This is bad for obvious reasons. This should allow to move these tasks out
 * of the UI thread, and therefore vastly improve user experience
 * 
 * <p>Serves as a task to run in the {@link DBEventScheduler}. Permits task 
 * naming, as well as the ability to categorize as async or not, to permit
 * rudimentary parallelism
 * @author Duemmer
 *
 */
public class DBTask implements Runnable 
{
	private boolean async;
	private String taskName;
	private Runnable task;
	private long timeQueued;
	private static Logger log = LogManager.getLogger(DBTask.class);

	

	/**
	 * Initializes a new task to run. Will formate the task for execution, but will 
	 * not start it.
	 * @param taskName the name of this task
	 * @param task the task code that will run. Note that exceptions need not be checked in this task.
	 */
	public DBTask(String taskName, ThrowingRunnable taskBase) {
		this(false, taskName, taskBase);
	}




	/**
	 * Initializes a new task to run. Will formate the task for execution, but will 
	 * not start it.
	 * @param async if true, will run this immediately, instead of posting it to the event queue
	 * @param taskName the name of this task
	 * @param task the task code that will run. Note that exceptions need not be checked in this task.
	 */
	public DBTask(boolean async, String taskName, ThrowingRunnable taskBase) {
		this.async = async;
		this.taskName = taskName;
		this.timeQueued = System.currentTimeMillis(); // default to time constructed
		
		// Runnable that will wrap the base task. This is what will actually run
		this.task = () -> {
			long start = System.currentTimeMillis();
			double timeScheduled = ((double)(start - timeQueued)) / 1000;
			log.debug(String.format("Starting task \"%s\" after being scheduled for %.3fs, async=%b", taskName, timeScheduled, async));
			
			try { taskBase.run(); } catch (Exception e) { 
				log.error("Task \"" +taskName+ "\" failed with exception", e); 
			}
			
			double time = ((double)(System.currentTimeMillis() - start)) / 1000;
			log.debug(String.format("Finished task \"%s\" in %.3fs", taskName, time));
		};
	}




	@Override public void run() {	
		if(!async) { // run normal
			task.run();
		} else { // just throw it in a thread and run with it
			Thread t = new Thread(task);
			t.setDaemon(true);
			t.setName("Task " +taskName);
			t.start();
		}
	}
	
	
	
	
	/**
	 * Call this method when scheduling the task to run. This is used
	 * to determine how long the task was waiting to run before it 
	 * got the chance
	 */
	protected void registerForExec() {
		log.debug("Scheduled task \"" +taskName+ "\" for execution");
		this.timeQueued = System.currentTimeMillis();
	}

}
