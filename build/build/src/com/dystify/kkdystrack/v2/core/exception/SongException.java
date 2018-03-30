package com.dystify.kkdystrack.v2.core.exception;


/**
 * Acts as a catch-all exception for issues pertaining to songs
 * @author Duemmer
 *
 */
public class SongException extends Exception 
{
	private static final long serialVersionUID = -1977192211038719782L;

	public SongException(String msg) { 
		super(msg);
	}
	
	public SongException(Throwable t) {
		super(t);
	}
	
	public SongException(String msg, Throwable t) {
		super(msg, t);
	}
}
