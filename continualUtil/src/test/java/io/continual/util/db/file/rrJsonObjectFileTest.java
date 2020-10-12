
package io.continual.util.db.file;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class rrJsonObjectFileTest extends TestCase
{
	private static File getTestFile () throws IOException
	{
		final File tmp = File.createTempFile ( "rrbfTest.", ".rrbf" );
		tmp.deleteOnExit ();
		return tmp;
	}

	@Test
	public void testWriteAndRead () throws IOException, JSONException
	{
		final File tmp = getTestFile ();
		JsonObjectFile.initialize ( tmp, 1024 );

		try ( final JsonObjectFile f = new JsonObjectFile ( tmp ) )
		{
			for ( int i=0; i<1024; i++ )
			{
				final JSONObject o = new JSONObject ();
				o.put ( "index", i );
				f.write ( o );
			}
		}

		try ( final JsonObjectFile f = new JsonObjectFile ( tmp ) )
		{
			for ( int i=0; i<1024; i++ )
			{
				final long blockAddr = f.indexToAddress ( i );
				final JSONObject o = f.read ( blockAddr );
				final int ii = (Integer) o.get ( "index" );
				assertEquals ( i, ii );
			}
		}
	}

	@Test
	public void testWriteAndReadWithPassword () throws IOException, JSONException
	{
		final File tmp = getTestFile ();
		JsonObjectFile.initialize ( tmp, 1024 );

		final int iters = 1024;
		JsonObjectFile f = new JsonObjectFile ( tmp, true, "a password" );
		for ( int i=0; i<iters; i++ )
		{
			final JSONObject o = new JSONObject ();
			o.put ( "index", i );
			f.write ( o );
		}
		f.close ();

		f = new JsonObjectFile ( tmp, false, "a password" );
		for ( int i=0; i<iters; i++ )
		{
			final long blockAddr = f.indexToAddress ( i );
			final JSONObject o = f.read ( blockAddr );
			final int ii = (Integer) o.get ( "index" );
			assertEquals ( "at iter " + i, i, ii );
		}
		f.close ();
	}
}
