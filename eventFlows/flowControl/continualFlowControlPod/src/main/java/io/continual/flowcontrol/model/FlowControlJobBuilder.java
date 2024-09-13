package io.continual.flowcontrol.model;

/**
 * A builder for flow control jobs.
 */
public interface FlowControlJobBuilder
{
	/**
	 * Build a flow control job
	 * @return a new job instance
	 */
	FlowControlJob build ();
	
	/**
	 * Update the job's name
	 * @param name
	 * @return this builder
	 */
	FlowControlJobBuilder setName ( String name );
	
	/**
	 * Overwrite the configuration for this job.
	 * @param config
	 */
	FlowControlJobBuilder setConfiguration ( FlowControlJob.FlowControlJobConfig config );

	/**
	 * Set the runtime spec for this job.
	 * @param runtimeSpec
	 */
	FlowControlJobBuilder setRuntimeSpec ( FlowControlJob.FlowControlRuntimeSpec runtimeSpec );

	/**
	 * Register a secret key and value. The value is encrypted and stored within the FlowControl job database.
	 * @param key
	 * @param value
	 * @throws ServiceException 
	 */
	FlowControlJobBuilder registerSecret ( String key, String value );

	/**
	 * Remove a secret reference from this job.
	 * @param key
	 */
	FlowControlJobBuilder removeSecretRef ( String key );
}
