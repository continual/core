package io.continual.basesvcs.services.storage.util;

import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.basesvcs.services.storage.StorageSourceStream;

public class MemStorageSourceStream extends MemDataSourceStream implements StorageSourceStream
{
	public MemStorageSourceStream ( StorageInode node, byte[] bytes )
	{
		super ( bytes );
		fId = node;
	}

	@Override
	public StorageInode getINode ()
	{
		return fId;
	}

	private StorageInode fId;
}