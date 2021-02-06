package io.continual.basesvcs.services.storage;

import io.continual.services.Service;

/**
 * A storage service moves raw data in and out of a storage system. Note that this
 * service layer is not concerned with item naming. It simply uses its own manufactured
 * IDs and expects the calling code to persist them accordingly.
 * 
 * @author peter
 */
public interface StorageService extends Service
{
	/**
	 * Create a new data item in the storage system.
	 * @param object
	 */
	StorageInode create ( DataSourceStream object ) throws StorageIoException;

	/**
	 * Load a data item from the storage system.
	 * @param node
	 * @return a stored item or null if it does not exist
	 */
	StorageSourceStream load ( StorageInode node ) throws StorageIoException;

	/**
	 * Store a data item into the storage system.
	 * @param nodeId
	 * @param object
	 */
	void store ( StorageInode nodeId, DataSourceStream object ) throws StorageIoException;

	/**
	 * Copy a data item to a new location in the storage system.
	 * @param nodeId
	 */
	StorageInode copy ( StorageInode nodeId ) throws StorageIoException;

	/**
	 * Remove a stored item from the storage system.
	 * @param nodeId
	 */
	void remove ( StorageInode nodeId ) throws StorageIoException;
}
