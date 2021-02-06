package io.continual.basesvcs.services.naming.std.memAndDisk;

import java.io.IOException;

import org.junit.Test;

import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.util.naming.Path;
import io.continual.util.nv.impl.nvWriteableTable;
import junit.framework.TestCase;

public class MemAndDiskNamingTest extends TestCase
{
	@Test
	public void testStorage () throws IOException
	{
		final MemAndDiskNameService fss = getNewStorage ();
		fss.store ( Path.fromString ( "/foo/bar" ), StorageInode.fromName ( "mySvc", "12345" ) );
		fss.store ( Path.fromString ( "/foo/baz" ), StorageInode.fromName ( "mySvc", "12311" ) );

		fss.requestFinish ();
	}

	private MemAndDiskNameService getNewStorage () throws IOException
	{
		final nvWriteableTable settings = new nvWriteableTable ();
		settings.set ( MemAndDiskNameService.kSetting_BackingFileName, "/tmp/cioNameSvcTest.txt" );
		settings.set ( MemAndDiskNameService.kSetting_FlushPeriod, 1 );
		return new MemAndDiskNameService ( settings );
	}
}
