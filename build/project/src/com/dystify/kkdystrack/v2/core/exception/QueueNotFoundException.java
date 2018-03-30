package com.dystify.kkdystrack.v2.core.exception;


/**
 * Indicates that a specified queue was not found in the database. Note that it
 * does not necissarily matter whether or not the queue in question is listed in 
 * the <code>queues</code> table, but rather the existence of the queue itself.
 * @author Duemmer
 *
 */
public class QueueNotFoundException extends Exception 
{
	private static final long serialVersionUID = -3752404341835034375L;
	
	public QueueNotFoundException(String msg) { 
		super(msg);
	}
	
	public QueueNotFoundException(Throwable t) {
		super(t);
	}
	
	public QueueNotFoundException(String msg, Throwable t) {
		super(msg, t);
	}
}
