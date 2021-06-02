package io.continual.onap.services.mrCommon;

import java.io.IOException;
import java.io.StringReader;

import io.continual.onap.services.mrCommon.SimplifiedJsonTokener.SimplifiedJsonException;
import io.continual.onap.services.subscriber.OnapMrFetchResponse;

/**
 * This is a basic implementation that parses a response JSON array of strings
 * into a list of strings.
 */
public class SimpleJsonResponseParser implements JsonResponseParser
{
	@Override
	public void parseResponseBody ( String s, OnapMrFetchResponse resp ) throws IOException
	{
		// allow null and empty strings by returning an empty list
		if ( s == null || s.length () == 0 )
		{
			resp.markEof ();
			return;
		}

		try ( final StringReader sr = new StringReader ( s ) )
		{
			final SimplifiedJsonTokener t = new SimplifiedJsonTokener ( sr );
	
			// read start bracket
			if ( t.nextClean () != '[' )
			{
				throw new IOException ( "An array text must start with '['" );
			}
	
			// if we have content, read that
			if ( t.nextClean () != ']' )
			{
				t.back ();
				for ( ;; )
				{
					resp.push ( t.nextValue () );
	
					final char next = t.nextClean ();
					if ( next == ']' )
					{
						break;
					}
					else if ( next == ',' )
					{
						// read another string
					}
					else
					{
						throw new IOException ( "Expected a ',' or ']'" );
					}
				}
			}
			// else: no strings in the array
	
			resp.markEof ();
		}
		catch ( SimplifiedJsonException e )
		{
			throw new IOException ( e );
		}
	}
}
