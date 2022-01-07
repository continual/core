package io.continual.flowcontrol.jobapi;

import java.io.InputStream;

public interface FlowControlJobConfig
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
