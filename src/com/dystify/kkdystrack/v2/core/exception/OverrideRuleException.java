package com.dystify.kkdystrack.v2.core.exception;


/**
 * Indicates an exception pertaining to {@link OverrideRule}, such as when trying to
 * remove the root override
 * @author Duemmer
 *
 */
public class OverrideRuleException extends Exception 
{
	private static final long serialVersionUID = -3752404341835034375L;
	
	public OverrideRuleException(String msg) { 
		super(msg);
	}
	
	public OverrideRuleException(Throwable t) {
		super(t);
	}
	
	public OverrideRuleException(String msg, Throwable t) {
		super(msg, t);
	}
}
