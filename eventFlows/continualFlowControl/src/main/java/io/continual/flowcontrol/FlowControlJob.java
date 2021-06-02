package io.continual.flowcontrol;

/**
 * A flow control job represents a deployment of a named event processor.
 */
public interface FlowControlJob
{
	/**
	 * Get this job's name.
	 * @return the job's name
	 */
	String getName ();
}
