package io.continual.basesvcs.services.storage.util;

import java.io.UnsupportedEncodingException;

/**
 * A convenience stored data class for strings
 * @author peter
 */
public class StringDataSourceStream extends MemDataSourceStream
{
	public StringDataSourceStream ( String s )
	{
		super ( stringToBytes ( s ) );
	}

	private static byte[] stringToBytes ( String s )
	{
		try
		{
			return s.getBytes ( "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( "Missing UTF-8 character encoding", e );
		}
	}
}