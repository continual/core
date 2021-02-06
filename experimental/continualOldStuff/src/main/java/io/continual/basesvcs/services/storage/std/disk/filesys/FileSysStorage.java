package io.continual.basesvcs.services.storage.std.disk.filesys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.storage.DataSourceStream;
import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.basesvcs.services.storage.StorageIoException;
import io.continual.basesvcs.services.storage.StorageSourceStream;
import io.continual.basesvcs.services.storage.std.BaseStorageService;
import io.continual.util.data.StreamTools;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.nv.NvReadable;

public class FileSysStorage extends BaseStorageService
{
	public static final String kSetting_DataDir = "storage.filesys.dataDir";
	public static final String kDefault_DataDir = "./data";

	public static final String kSetting_Name = "name";
	public static final String kDefault_Name = "anonymousFileStorage";

	public FileSysStorage ( NvReadable p )
	{
		fBaseDir = new File ( p.getString ( kSetting_DataDir, kDefault_DataDir ) );
		fName = p.getString ( kSetting_Name, kDefault_Name );
	}

	@Override
	public StorageInode create ( DataSourceStream object ) throws StorageIoException
	{
		try
		{
			final StorageInode inode = createInode ();

			// write the data
			store ( inode, object );

			return inode;
		}
		catch ( IOException e )
		{
			throw new StorageIoException ( e );
		}
	}

	@Override
	public StorageSourceStream load ( StorageInode nodeId )
	{
		final File f = makeNodeFile ( nodeId );
		return new FileLoadedData ( nodeId, f );
	}

	@Override
	public void store ( StorageInode inode, DataSourceStream object ) throws StorageIoException
	{
		try
		{
			final File f = makeNodeFile ( inode );
			final FileOutputStream fos = new FileOutputStream ( f );
			try
			{
				StreamTools.copyStream ( object.read(), fos );
			}
			finally
			{
				fos.close ();
			}
		}
		catch ( IOException e )
		{
			throw new StorageIoException ( e );
		}
	}

	@Override
	public StorageInode copy ( StorageInode fromNode ) throws StorageIoException
	{
		try
		{
			final StorageInode newNode = createInode ();
			Files.copy (
				makeNodeFile ( fromNode ).toPath (),
				makeNodeFile ( newNode ).toPath (),
				StandardCopyOption.REPLACE_EXISTING
			);
			return newNode;
		}
		catch ( IOException e )
		{
			throw new StorageIoException ( e );
		}
	}

	@Override
	public void remove ( StorageInode nodeId ) throws StorageIoException
	{
		makeNodeFile ( nodeId ).delete ();
	}

	private final File fBaseDir;
	private final String fName;

	/**
	 * Get the file for a given inode. This only uses the ID and ignores the
	 * user's name for the file.
	 * @param inode
	 * @return a file
	 */
	private File makeNodeFile ( StorageInode inode )
	{
		if ( !inode.getServiceName ().equals ( fName ) )
		{
			throw new IllegalArgumentException ( "The storage inode's service name does not match this service." );
		}

		final String id = inode.getId ();
		if ( id.length () < 2 )
		{
			throw new IllegalArgumentException ( "Node id is too short." );
		}
		final String firstChunk = id.substring ( 0, 2 );
		return new File ( new File ( fBaseDir, firstChunk ), inode.getId () );
	}

	/**
	 * Create a new inode file
	 * @return
	 */
	private StorageInode createInode ()
	{
		String id = UniqueStringGenerator.createKeyUsingAlphabet ( "filesysstorage", kFileNameAlphabet, 24 );
		StorageInode inode = StorageInode.fromName ( fName, id );
		while ( makeNodeFile ( inode ).exists () )
		{
			log.warn ( "Oddity: unique string " + id + " is already in use. Generating a new one." );
			id = UniqueStringGenerator.createKeyUsingAlphabet ( "filesysstorage", kFileNameAlphabet, 24 );
			inode = StorageInode.fromName ( fName, id );
		}

		// make sure the node file's data directories exist
		final File nodeFile = makeNodeFile ( inode );
		nodeFile.getParentFile ().mkdirs ();

		return inode;
	}

	private final String kFileNameAlphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
	private static final Logger log = LoggerFactory.getLogger ( FileSysStorage.class );
}
