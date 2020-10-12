/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.util.db.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.json.CommentedJsonTokener;

/**
 * A file of JSON object records, implemented over our BlockFile.
 */
public class JsonObjectFile implements Closeable
{
	/**
	 * Initialize a JSON object file
	 * @param file
	 * @param blockSize
	 * @throws IOException
	 */
	public static void initialize ( File file, int blockSize ) throws IOException
	{
		BlockFile.initialize ( file, blockSize );
	}

	/**
	 * Open a writable JSON object file
	 * @param f
	 * @throws IOException
	 */
	public JsonObjectFile ( File f ) throws IOException
	{
		this ( f, true );
	}

	/**
	 * Open a JSON object file, optionally for writing
	 * @param f
	 * @param withWrite
	 * @throws IOException
	 */
	public JsonObjectFile ( File f, boolean withWrite ) throws IOException
	{
		this ( f, withWrite, null );
	}

	/**
	 * Open a JSON object file, optionally for writing, with the given password.
	 * @param f
	 * @param withWrite
	 * @param passwd A password, if used, or null.
	 * @throws IOException
	 */
	public JsonObjectFile ( File f, boolean withWrite, String passwd ) throws IOException
	{
		fFile = new BlockFile ( f, withWrite, passwd );
	}

	/**
	 * Get the absolute path of the underlying file.
	 * @return the file's absolute path
	 */
	public String getFilePath ()
	{
		return fFile.getFilePath ();
	}
	
	/**
	 * Close this file
	 * @throws IOException
	 */
	public void close () throws IOException
	{
		fFile.close ();
	}

	/**
	 * Read the object at the given address.
	 * @param address
	 * @return an object, or null
	 * @throws IOException
	 */
	public JSONObject read ( long address ) throws IOException
	{
		JSONObject o = null;
		final InputStream is = fFile.readToStream ( address );
		try
		{
			o = new JSONObject ( new CommentedJsonTokener ( new InputStreamReader ( is ) ) );
		}
		catch ( JSONException e )
		{
			throw new IOException ( e );
		}
		finally
		{
			is.close ();
		}
		return o;
	}

	/**
	 * Write a new object into the file and return its address.
	 * @param o
	 * @return the address of the new object
	 * @throws IOException
	 */
	public long write ( JSONObject o ) throws IOException
	{
		final byte[] b = o.toString ().getBytes ( Charset.forName ( "UTF-8" ) );
		return fFile.create ( b );
	}

	/**
	 * Overwrite an object at the given address.
	 * @param address
	 * @param o
	 * @throws IOException
	 */
	public void overwrite ( long address, JSONObject o ) throws IOException
	{
		final byte[] b = o.toString ().getBytes ( Charset.forName ( "UTF-8" ) );
		fFile.overwrite ( address, b );
	}

	/**
	 * Delete an object at the given address.
	 * @param address
	 * @throws IOException
	 */
	public void delete ( long address ) throws IOException
	{
		fFile.delete ( address );
	}

	/**
	 * @see BlockFile#indexToAddress(long)
	 * @param index
	 * @return an address
	 */
	public long indexToAddress ( long index )
	{
		return fFile.indexToAddress ( index );
	}

	private final BlockFile fFile;
}
