package io.continual.basesvcs.services.naming;

import java.util.Set;

import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.services.CliControlledService;
import io.continual.services.Service;
import io.continual.util.naming.Path;


/**
 * The naming service maps paths into storage inodes
 * @author peter
 */
public interface NameService extends Service, CliControlledService
{
	/**
	 * Given a node name path, return the storage inode
	 * that contains its data, or null if it does not exist
	 * @param nodeId
	 * @return a storage inode or null
	 * @throws NamingIoException
	 */
	StorageInode lookup ( Path nodeId ) throws NamingIoException;

	/**
	 * Given a path, find direct children in the name system.
	 * @param path
	 * @return a set of 0 or more children
	 * @throws NamingIoException
	 */
	Set<Path> getChildren ( Path path ) throws NamingIoException;
	
	/**
	 * Associate a node ID at a given path.
	 * @param nodeId
	 * @param inode
	 * @throws NamingIoException
	 */
	void store ( Path nodeId, StorageInode inode ) throws NamingIoException;

	/**
	 * Remove the entry for a given path.
	 * @param nodeId
	 * @throws NamingIoException
	 */
	void remove ( Path nodeId ) throws NamingIoException;
}
