package io.continual.onap.services.mrCommon;

/**
 * A clock for timing
 */
public interface Clock
{
	/**
	 * Get the current time in epoch millis.
	 * @return the current time
	 */
	long nowMs ();
}
