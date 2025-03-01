package io.continual.flowcontrol.model;

import java.io.IOException;
import java.security.GeneralSecurityException;

import io.continual.builder.Builder.BuildFailure;

/**
 * A builder for flow control jobs.
 */
public interface FlowControlJobBuilder
{
	/**
	 * Clone an existing job into this builder, overwriting all data currently in the builder
	 * @param existingJob
	 * @return this builder
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 */
	FlowControlJobBuilder clone ( FlowControlJob existingJob ) throws GeneralSecurityException, IOException;

	/**
	 * Update the job's name, which uniquely identifies the job
	 * @param id
	 * @return this builder
	 */
	FlowControlJobBuilder withId ( String id );

	/**
	 * Update the job's display name
	 * @param name
	 * @return this builder
	 */
	FlowControlJobBuilder withDisplayName ( String name );

	/**
	 * Use the given owner ID for this job
	 * @param ownerId
	 * @return this builder
	 */
	FlowControlJobBuilder withOwner ( String ownerId );

	/**
	 * Grant the given user the given operations
	 * @param user
	 * @param ops
	 * @return this builder
	 */
	FlowControlJobBuilder withAccess ( String user, String... ops );

	/**
	 * Overwrite the configuration for this job.
	 * @param config
	 * @throws IOException 
	 */
	FlowControlJobBuilder setConfiguration ( FlowControlJob.FlowControlJobConfig config ) throws IOException;

	/**
	 * Set the runtime spec for this job.
	 * @param runtimeSpec
	 */
	FlowControlJobBuilder setRuntimeSpec ( FlowControlJob.FlowControlRuntimeSpec runtimeSpec );

	/**
	 * Register a secret key and value. The value is encrypted and stored within the FlowControl job database.
	 * @param key
	 * @param value
	 * @throws GeneralSecurityException 
	 */
	FlowControlJobBuilder registerSecret ( String key, String value ) throws GeneralSecurityException;

	/**
	 * Remove a secret reference from this job.
	 * @param key
	 */
	FlowControlJobBuilder removeSecretRef ( String key );

	/**
	 * Build a flow control job
	 * @return a new job instance
	 * @throws BuildFailure 
	 */
	FlowControlJob build () throws BuildFailure;
}
