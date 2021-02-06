package io.continual.basesvcs.services.storage.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import io.continual.basesvcs.services.storage.DataSourceStream;

public class MemDataSourceStream implements DataSourceStream
{
	public MemDataSourceStream ( byte[] bytes )
	{
		fBytes = bytes;
	}

	@Override
	public InputStream read ()
	{
		return new ByteArrayInputStream ( fBytes );
	}

	private final byte[] fBytes;
}