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

package io.continual.util.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamTools
{
	public static class StreamCopier
	{
		public StreamCopier from ( InputStream is )
		{
			fFrom = is;
			return this;
		}

		public StreamCopier to ( OutputStream os )
		{
			fTo = os;
			return this;
		}

		public StreamCopier withBufferSize ( int bufSize )
		{
			fBufferSize = bufSize;
			return this;
		}
		
		public StreamCopier closeStream () { return closeStream ( true ); }
		public StreamCopier leaveStreamOpen () { return closeStream ( false ); }

		public StreamCopier closeStream ( boolean cs )
		{
			fCloseStream = cs;
			return this;
		}

		public void copy () throws IOException
		{
			final byte[] buffer = new byte [fBufferSize];
			int len;
			while ( ( len = fFrom.read ( buffer ) ) != -1 )
			{
				fTo.write ( buffer, 0, len );
			}
			if ( fCloseStream ) fTo.close ();
		}

		private InputStream fFrom;
		private OutputStream fTo;
		private int fBufferSize = kBufferLength;
		private boolean fCloseStream = true;
	}
	
	protected static final int kBufferLength = 4096;

	/**
	 * Reads the stream into a byte array using a default-sized array for each read,
	 * then closes the input stream.
	 * 
	 * @param is an input stream
	 * @return a byte array
	 * @throws IOException any I/O exception from the underlying read calls
	 */
	public static byte[] readBytes ( InputStream is ) throws IOException
	{
		return readBytes ( is, kBufferLength );
	}

	/**
	 * Reads the stream into a byte array using a bufferSize array for each read,
	 * then closes the input stream.
	 * 
	 * @param is an input stream
	 * @param bufferSize number of bytes to read at a time
	 * @return a byte array
	 * @throws IOException any I/O exception from the underlying read calls
	 */
	public static byte[] readBytes ( InputStream is, int bufferSize ) throws IOException
	{
		return readBytes ( is, bufferSize, -1 );
	}

	/**
	 * Reads the stream into a byte array using a bufferSize array for each read,
	 * then closes the input stream. If limit &gt;= 0, at most limit bytes are read.<br>
	 * Note: even with a negative limit, 2GB is the limit.
	 * 
	 * @param is an input stream
	 * @param bufferSize number of bytes to read at a time
	 * @param limit max byte count to read. If negative, no limit other than hard 2GB limit
	 * @return a byte array
	 * @throws IOException any I/O exception from the underlying read calls
	 */
	public static byte[] readBytes ( InputStream is, int bufferSize, int limit ) throws IOException
	{
		int counter = 0;
		final int atMost = limit < 0 ? Integer.MAX_VALUE : Math.min ( limit, Integer.MAX_VALUE );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		if ( is != null )
		{
			byte[] b = new byte [ bufferSize ];
			int len = 0;
			do
			{
				len = is.read ( b );
				if ( -1 != len )
				{
					final int readNow = Math.min ( len, atMost - counter );
					baos.write ( b, 0, readNow );
					counter += readNow;
				}
			}
			while ( len != -1 && counter < atMost );
			is.close ();
		}

		return baos.toByteArray ();
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in the input stream
	 * @param out the target output stream
	 * @throws IOException if an underlying file operation throws
	 */
	public static void copyStream ( InputStream in, OutputStream out ) throws IOException
	{
		new StreamCopier().from ( in ).to ( out ).copy ();
	}

	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in the input stream
	 * @param out the target output stream
	 * @param bufferSize the number of bytes to transfer at a time
	 * @throws IOException if an underlying file operation throws
	 */
	public static void copyStream ( InputStream in, OutputStream out, int bufferSize ) throws IOException
	{
		new StreamCopier().from ( in ).to ( out ).withBufferSize ( bufferSize ).copy ();
	}


	/**
	 * Copy from the input stream to the output stream, then close the output stream.
	 * @param in the input stream
	 * @param out the target output stream
	 * @param bufferSize the number of bytes to transfer at a time
	 * @param closeOutputStream if true, close the output stream at the end of the call
	 * @throws IOException if an underlying file operation throws
	 */
	public static void copyStream ( InputStream in, OutputStream out, int bufferSize, boolean closeOutputStream ) throws IOException
	{
		new StreamCopier().from ( in ).to ( out ).withBufferSize ( bufferSize ).closeStream ( closeOutputStream ).copy ();
	}
}
