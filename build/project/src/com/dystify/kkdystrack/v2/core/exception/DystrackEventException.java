package com.dystify.kkdystrack.v2.core.exception;

import com.dystify.kkdystrack.v2.core.event.types.GenericDystrackEvent;

/**
 * Indicates an exception pertaining to {@link GenericDystrackEvent}, such as when trying to
 * post an externally generated event to the database, which would be redundant and could cause an
 * event loop where the same event keeps getting circulated
 * @author Duemmer
 *
 */
public class DystrackEventException extends Exception 
{
	private static final long serialVersionUID = -7984057122417976751L;

	public DystrackEventException(String msg) { 
		super(msg);
	}
	
	public DystrackEventException(Throwable t) {
		super(t);
	}
	
	public DystrackEventException(String msg, Throwable t) {
		super(msg, t);
	}
}
