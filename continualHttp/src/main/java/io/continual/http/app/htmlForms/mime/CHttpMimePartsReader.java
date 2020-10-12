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


package io.continual.http.app.htmlForms.mime;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.util.data.HumanReadableHelper;
import io.continual.util.data.StringUtils;

import io.continual.util.collections.MultiMap;

/**
 * A multipart MIME reader.
 */
public class CHttpMimePartsReader
{
	/**
	 * Construct a multipart MIME reader with a boundary string and a part factory.
	 * @param boundary
	 * @param mpf
	 */
	public CHttpMimePartsReader ( String boundary, CHttpMimePartFactory mpf )
	{
		fBoundaryLine = "--" + boundary;
		fBoundaryEndMarker = fBoundaryLine + "--";
		fPartIndex = -1;
		fInPartHeader = false;
		fPartHeaders = null;
		fPartFactory = mpf;
		fCurrentPart = null;
		fAllParts = new ArrayList<>();
	}

	/**
	 * Read the given input stream. The stream is read to the end, but
	 * left open for the caller to close.
	 * 
	 * @param in
	 * @throws IOException
	 */
	public void read ( InputStream in ) throws IOException
	{
		final BufferedInputStream bis = new BufferedInputStream ( in );

		String line;
		while ( ( line = readLine ( bis ) ) != null )
		{
			if ( line.equals ( fBoundaryLine ) )
			{
				onPartBoundary ( ++fPartIndex );
				fInPartHeader = true;
				fPartHeaders = new MultiMap<>();
			}
			else if ( line.equals ( fBoundaryEndMarker ) )
			{
				onPartBoundary ( ++fPartIndex );
				onStreamEnd ();
				break;
			}
			else //noinspection StatementWithEmptyBody
                if ( fPartIndex == -1 )
			{
				// header info, discard
			}
			else if ( fInPartHeader && line.length() == 0 )
			{
				// switch from header info to body
				fInPartHeader = false;
				onPartHeaders ( fPartHeaders );

				// now read until the next part boundary...
				readPartBytes ( bis );

				// here, we expect CRLF prior to the next part boundary
				line = readLine ( bis );
				if ( line == null || line.length() > 0 )
				{
					log.warn ( "Unexpected state in MIME reader. After MIME part, found line [" + line + "]." );
				}
			}
			else if ( fInPartHeader )
			{
				// part header line
				final int colon = line.indexOf ( ':' );
                //noinspection StatementWithEmptyBody
                if ( colon == -1 )
				{
					// weird. ignore.
				}
				else
				{
					final String key = line.substring ( 0, colon ).trim ().toLowerCase ();
					final String val = line.substring ( colon + 1 ).trim ();
					fPartHeaders.put ( key, val );
				}
			}
			else
			{
				// hmm
				log.warn ( "Unexpected state in MIME reader." );
			}
		}
	}

	/**
	 * Get the MIME parts read by this reader.
	 * @return a list of 0 or more MIME parts
	 */
	public List<CHttpMimePart> getParts ()
	{
		return fAllParts;
	}

	/**
	 * Parse a content disposition string into a multimap.
	 * @param cd
	 * @return a multimap wth entries from the disposition string
	 */
	public static MultiMap<String,String> parseContentDisposition ( String cd )
	{
		// e.g. Content-Disposition: form-data; name="image1"; filename="GrandCanyon.jpg"
		final MultiMap<String,String> result = new MultiMap<>();
		final String[] parts = cd.split ( ";" );
		if ( parts.length > 0 )
		{
			// first part is special -- it's the disposition (e.g. "attachment")
			result.put ( "disposition", parts[0] );
			for ( int i=1; i<parts.length; i++ )
			{
				// these are name="value"
				final int eq = parts[i].indexOf ( '=' );
				if ( eq > -1 )
				{
					final String name = parts[i].substring ( 0, eq ).trim ();
					final String val = StringUtils.dequote ( parts[i].substring ( eq + 1 ).trim () );
					result.put ( name, val );
				}
				else
				{
					// just dump it in as name and value
					result.put ( parts[i], parts[i] );
				}
			}
		}
		return result;
	}

	/**
	 * Called on each part boundary. They occur after the pre-part heading and before the first part,
	 * then after each part (including the last one).
	 * @param i
	 * @throws IOException 
	 */
	protected void onPartBoundary ( int i ) throws IOException
	{
		closeCurrentPart ();
	}

	/**
	 * Called when finished reading a part's header section
	 * @param headers
	 * @throws IOException 
	 */
	protected void onPartHeaders ( MultiMap<String,String> headers ) throws IOException
	{
		closeCurrentPart ();
		fCurrentPart = fPartFactory.createPart ( headers );
	}

	/**
	 * Called multiple times during the read of a part's body.
	 * @param line
	 * @throws IOException 
	 */
	protected void onPartBytes ( byte[] line, int offset, int length ) throws IOException
	{
		if ( fCurrentPart != null )
		{
			fCurrentPart.write ( line, offset, length );
		}
	}

	/**
	 * Called when the multipart stream is complete
	 */
	protected void onStreamEnd ()
	{
	}

	private final String fBoundaryLine;
	private final String fBoundaryEndMarker;
	private int fPartIndex;
	private boolean fInPartHeader;
	private MultiMap<String,String> fPartHeaders;
	private final CHttpMimePartFactory fPartFactory;
	private CHttpMimePart fCurrentPart;
	private final ArrayList<CHttpMimePart> fAllParts;

	private static final int kPartBytesBufferSize = 2048;

	private void closeCurrentPart () throws IOException
	{
		if ( fCurrentPart != null )
		{
			fCurrentPart.close ();
			fAllParts.add ( fCurrentPart );
			fCurrentPart = null;
		}
	}
	
	private static String readLine ( BufferedInputStream bis ) throws IOException
	{
		// in this mode, we're looking for a line ending
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		boolean eol = false;
		while ( !eol )
		{
			int b = bis.read ();
			if ( b == -1 )
			{
				break;
			}
			else
			{
				if ( b == '\r' || b == '\n' )
				{
					eol = true;
					bis.mark ( 1 );
	
					// eat a \r\n just like \r or \n
					if ( b == '\r' )
					{
						b = bis.read ();
						if ( b != '\n' )
						{
							bis.reset ();
						}
					}
				}
				else
				{
					baos.write ( b );
				}
			}
		}
		
		String result = "";
		if ( baos.size () > 0 )
		{
			result = new String ( baos.toByteArray () );
		}
		return result;
	}

	private void readPartBytes ( BufferedInputStream bis ) throws IOException
	{
		final byte[] buffer = new byte [ kPartBytesBufferSize ];
		int readSoFar = 0;
		long readTotal = 0L;

		final int boundaryTagLen = fBoundaryLine.length () + 2;	// 2 for preceding CRLF
		final byte[] boundaryLineBytes = ("\r\n" + fBoundaryLine).getBytes();

		// read until the boundary line is found. we have to inspect each byte
		// so just read them one at a time to keep the code simple
		while ( true )
		{
			bis.mark ( 2 );

			final int b = bis.read ();
			if ( b == '\r' )
			{
				// this could start a part boundary. see if we can read it.

				// first deliver the current buffer
				onPartBytes ( buffer, 0, readSoFar );
				readSoFar = 0;

				// now see what's here.
				bis.reset ();
				bis.mark ( boundaryTagLen );
				final int read = bis.read ( buffer, 0, boundaryTagLen );
				if ( read == boundaryTagLen && startsWith ( buffer, boundaryLineBytes ) )
				{
					bis.reset ();
					return;
				}
				else
				{
					// nevermind, continue, but only consume the hyphen so that 
					// the following bytes are processed properly.
					bis.reset ();
					buffer[ readSoFar++ ] = (byte)((bis.read()) & 0xff);
					readTotal++;
				}
			}
			else if ( b == -1 )
			{
				// this is a mulitpart stream read. it's required to end with a part boundary before
				// the stream is complete.
				throw new IOException ( "Stream ended without part boundary." );
			}
			else
			{
				buffer[ readSoFar++ ] = (byte)(b & 0xff);
				readTotal++;
				if ( readSoFar == 2048 )
				{
					onPartBytes ( buffer, 0, readSoFar );
					readSoFar = 0;
				}
			}

			if ( readTotal % (1024*1024) == 0 )
			{
				log.info ( HumanReadableHelper.byteCountValue ( readTotal ) + " read" );
			}
		}
	}

	/**
	 * Check if one byte array starts with another. Equivalent to startsWith(source,0,match);
	 * @param source
	 * @param match
	 * @return true if source starts with match
	 */
	public static boolean startsWith ( byte[] source, byte[] match )
	{
		return startsWith ( source, 0, match );
	}

	/**
	 * Check if one byte array contains another at the given offset.
	 * @param source
	 * @param offset
	 * @param match
	 * @return true if source contains match at the offset
	 */
	public static boolean startsWith ( byte[] source, int offset, byte[] match )
	{
		if ( match.length > ( source.length - offset ) )
		{
			return false;
		}

		for ( int i = 0; i < match.length; i++ )
		{
			if ( source[offset + i] != match[i] )
			{
				return false;
			}
		}
		return true;
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpMimePartsReader.class );
}
