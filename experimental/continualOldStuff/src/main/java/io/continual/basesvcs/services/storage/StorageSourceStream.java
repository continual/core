package io.continual.basesvcs.services.storage;

/**
 * Stream data with an associated storage node.
 * @author peter
 */
public interface StorageSourceStream extends DataSourceStream
{
	/**
	 * An storage node
	 * @return an inode record
	 */
	StorageInode getINode ();
}
