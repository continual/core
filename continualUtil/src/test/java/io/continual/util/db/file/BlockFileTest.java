
package io.continual.util.db.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import org.junit.Test;

public class BlockFileTest extends TestCase
{
	private static File getTestFile () throws IOException
	{
		final File tmp = File.createTempFile ( "rrbfTest.", ".rrbf" );
		tmp.deleteOnExit ();
		return tmp;
	}

	private static byte[] getBytes ( long len )
	{
		char current = 'a';
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		while ( len-- > 0 )
		{
			baos.write ( current++ );
			if ( current > 'z' )
			{
				current = 'a';
			}
		}
		return baos.toByteArray ();
	}

	private static void assertArrayEq ( byte[] expect, byte[] actual )
	{
		assertArrayEq ( expect, actual, 0, actual.length );
	}

	private static void assertArrayEq ( byte[] expect, byte[] actual, int offset, int len )
	{
		assertEquals ( expect.length, len );
		for ( int i=offset; i<(offset + len); i++ )
		{
			assertEquals ( expect[i-offset], actual[i] );
		}
	}

	@Test
	public void testInit () throws IOException
	{
		final File tmp = getTestFile ();
		BlockFile.initialize ( tmp, 1024 );

		final BlockFile bf = new BlockFile ( tmp );
		bf.close ();
	}

	@Test
	public void testOneBlockWriteAndRead () throws IOException
	{
		final File tmp = getTestFile ();
		BlockFile.initialize ( tmp, 1024 );

		final BlockFile bf = new BlockFile ( tmp );
		final byte[] ba = "test".getBytes ( Charset.forName ( "UTF-8" ) );
		final long address = bf.create ( ba );
		final byte[] bb = bf.read ( address );
		bf.close ();

		assertEquals ( new String ( ba ), new String ( bb ) );
	}

	@Test
	public void testMultiBlockWriteAndRead () throws IOException
	{
		final File tmp = getTestFile ();
		BlockFile.initialize ( tmp, 32 );

		BlockFile bf = new BlockFile ( tmp );
		final byte[] in = getBytes ( 48 );	// given 8 byte overhead, that's 2 blocks exactly
		final long address = bf.create ( in );
		bf.close ();

		bf = new BlockFile ( tmp );
		final byte[] bb = bf.read ( address );
		bf.close ();

		assertArrayEq ( in, bb );
	}

	@Test
	public void testOverwrite () throws IOException
	{
		final File tmp = getTestFile ();
		BlockFile.initialize ( tmp, 32 );

		BlockFile bf = new BlockFile ( tmp );
		final byte[] in = getBytes ( 48 );	// given 8 byte overhead, that's 2 blocks exactly
		final long address = bf.create ( in );
		bf.close ();

		bf = new BlockFile ( tmp );
		final byte[] bb = bf.read ( address );
		assertArrayEq ( in, bb );
		final byte[] in2 = getBytes ( 32 );
		bf.overwrite ( address, in2 );
		bf.close ();
		
		bf = new BlockFile ( tmp );
		final byte[] bc = bf.read ( address );
		assertArrayEq ( in2, bc );
		bf.close ();
	}

	@Test
	public void testAppend () throws IOException
	{
		final File tmp = getTestFile ();
		BlockFile.initialize ( tmp, 32 );

		BlockFile bf = new BlockFile ( tmp );
		final byte[] in = getBytes ( 24 );
		final long address = bf.create ( in );
		bf.append ( address, in );
		bf.close ();

		bf = new BlockFile ( tmp );
		final byte[] bb = bf.read ( address );
		assertEquals ( 48, bb.length );
		assertArrayEq ( in, bb, 0, 24 );
		assertArrayEq ( in, bb, 24, 24 );
		bf.close ();
	}
}
