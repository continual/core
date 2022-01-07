package io.continual.flowcontrol.jobapi;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import io.continual.flowcontrol.controlapi.FlowControlRuntimeSpec;
import io.continual.flowcontrol.jobapi.FlowControlJobDb.ServiceException;
import io.continual.iam.access.ProtectedResource;

/**
 * A flow control job is a spec that includes configuration data, secrets, and a 
 * runtime system selection.
 */
public interface FlowControlJob extends ProtectedResource
{
	/**
	 * Get this job's name.
	 * @return the job's name
	 */
	String getName ();

	/**
	 * Get the configuration for this job.
	 * @return a configuration
	 */
	FlowControlJobConfig getConfiguration ();

	/**
	 * Set the configuration for this job.
	 * @param config
	 */
	FlowControlJob setConfiguration ( FlowControlJobConfig config ) throws IOException;

	/**
	 * Get the runtime spec for this job.
	 * @return a runtime spec
	 */
	FlowControlRuntimeSpec getRuntimeSpec ();

	/**
	 * Set the runtime spec for this job.
	 * @param runtimeSpec
	 */
	FlowControlJob setRuntimeSpec ( FlowControlRuntimeSpec runtimeSpec );

	/**
	 * Get a map of secrets and their values
	 * @return a map of secrets
	 * @throws ServiceException 
	 */
	Map<String,String> getSecrets () throws ServiceException;

	/**
	 * Get a set of secret references used in this job's deployment. 
	 * @return a list of 0 or more secret references
	 * @throws ServiceException 
	 */
	default Set<String> getSecretRefs () throws ServiceException
	{
		return getSecrets().keySet ();
	}

	/**
	 * Register a secret key and value. The value is encrypted and stored within the FlowControl job database.
	 * @param key
	 * @param value
	 * @throws ServiceException 
	 */
	FlowControlJob registerSecret ( String key, String value ) throws ServiceException;

	/**
	 * Remove a secret reference from this job.
	 * @param key
	 */
	FlowControlJob removeSecretRef ( String key );
}
