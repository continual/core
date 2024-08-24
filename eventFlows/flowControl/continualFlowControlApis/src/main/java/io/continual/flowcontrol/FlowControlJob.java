package io.continual.flowcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException;
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
	 * The job configuration is opaque to this service. It has a MIME type (e.g. application/json) and
	 * a content stream.
	 */
	interface FlowControlJobConfig
	{
		/**
		 * Get the configuration data type
		 * @return a MIME data type
		 */
		String getDataType ();

		/**
		 * Return a stream of configuration data.
		 * @return an input stream for this configuration
		 */
		InputStream readConfiguration ();
	}

	/**
	 * Get the configuration for this job.
	 * @return a configuration
	 */
	FlowControlJobConfig getConfiguration ();

	/**
	 * Overwrite the configuration for this job.
	 * @param config
	 */
	FlowControlJob setConfiguration ( FlowControlJobConfig config ) throws IOException;

	/**
	 * A specification of an event processing runtime. Job configs contain a runtime spec like
	 * "continualProcessor" and "0.3", which the deployment service will map to an actual runtime
	 * (e.g. container image). 
	 */
	interface FlowControlRuntimeSpec
	{
		/**
		 * The name of the runtime
		 * @return a name
		 */
		String getName ();

		/**
		 * The version of the runtime, normally a semver string.
		 * @return the version string
		 */
		String getVersion ();
	}

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
	 * Get a map of secrets and their values.
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
