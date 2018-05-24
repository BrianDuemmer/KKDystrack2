package com.dystify.kkdystrack.v2.service;

/**
 * Like a regular {@link java.lang.Runnable}, but allows exceptions to be thrown
 * @author Duemmer
 *
 */
@FunctionalInterface
public interface ThrowingRunnable {
	public void run() throws Exception;
}
