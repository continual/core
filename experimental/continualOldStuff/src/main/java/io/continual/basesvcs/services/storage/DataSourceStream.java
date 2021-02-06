package io.continual.basesvcs.services.storage;

import java.io.InputStream;

/**
 * Data that can be streamed in.
 * @author peter
 */
public interface DataSourceStream
{
	/**
	 * Get the data via input stream
	 * @return an input stream
	 * @throws StorageIoException 
	 */
	InputStream read () throws StorageIoException;
}
