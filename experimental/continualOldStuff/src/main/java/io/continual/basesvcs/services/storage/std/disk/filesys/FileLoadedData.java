package io.continual.basesvcs.services.storage.std.disk.filesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.basesvcs.services.storage.StorageIoException;
import io.continual.basesvcs.services.storage.StorageSourceStream;

class FileLoadedData implements StorageSourceStream
{
	public FileLoadedData ( StorageInode node, File dataFile )
	{
		fId = node;
		fFile = dataFile;
	}

	@Override
	public InputStream read () throws StorageIoException
	{
		try
		{
			return new FileInputStream ( fFile );
		}
		catch ( FileNotFoundException e )
		{
			throw new StorageIoException ( e );
		}
	}

	@Override
	public StorageInode getINode ()
	{
		return fId;
	}

	private StorageInode fId;
	private File fFile;
}
