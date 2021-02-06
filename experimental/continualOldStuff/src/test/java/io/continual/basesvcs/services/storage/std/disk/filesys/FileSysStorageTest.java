package io.continual.basesvcs.services.storage.std.disk.filesys;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import io.continual.basesvcs.services.storage.DataSourceStream;
import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.basesvcs.services.storage.util.StringDataSourceStream;
import io.continual.util.nv.impl.nvWriteableTable;
import junit.framework.TestCase;

public class FileSysStorageTest extends TestCase
{
	@Test
	public void testStorage () throws IOException
	{
		final String inStr = "foo";

		final FileSysStorage fss = getNewStorage ();
		final StorageInode node = fss.create ( new StringDataSourceStream ( inStr ) );

		final DataSourceStream sd = fss.load ( node );
		final String s = IOUtils.toString ( sd.read(), "UTF-8" );
		assertEquals ( inStr, s );

		final StorageInode newNode = fss.copy ( node );
		fss.remove ( node );

		final DataSourceStream sd2 = fss.load ( newNode );
		final String s2 = IOUtils.toString ( sd2.read(), "UTF-8" );
		assertEquals ( inStr, s2 );
		
		fss.requestFinish ();
	}

	private FileSysStorage getNewStorage () throws IOException
	{
		// make a tmp dir
		final File tmp = File.createTempFile ( "cioFssTest", "" );
		tmp.delete ();
		tmp.mkdirs ();

		final nvWriteableTable settings = new nvWriteableTable ();
		settings.set ( FileSysStorage.kSetting_DataDir, tmp.getAbsolutePath () );
		return new FileSysStorage ( settings );
	}
}
