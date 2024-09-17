package io.continual.flowcontrol.model;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;

import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.access.ProtectedResource;

/**
 * A flow control job is a spec that includes configuration data, secrets, and a 
 * runtime system selection.
 */
public interface FlowControlJob extends ProtectedResource
{
	/**
	 * Get this job's unique ID.
	 * @return a unique ID
	 */
	String getId ();

	/**
	 * Get this job's name, expected to be suitable for display.
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
	 * Get a map of secrets and their values.
	 * @param enc the encryption tool to decrypt values
	 * @return a map of secrets
	 * @throws GeneralSecurityException 
	 */
	Map<String,String> getSecrets ( Encryptor enc ) throws GeneralSecurityException;

	/**
	 * Get a set of secret references used in this job's deployment. 
	 * @return a list of 0 or more secret references
	 */
	Set<String> getSecretRefs ();
}
