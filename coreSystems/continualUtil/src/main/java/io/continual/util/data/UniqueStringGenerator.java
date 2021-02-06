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

import java.util.UUID;

/**
 * create a string that's unlikely to be guessed
 */
public class UniqueStringGenerator
{
	public static String create ( String nonsense )
	{
		final byte[] val = createValue ( nonsense );
		return TypeConvertor.bytesToHexString ( val );
	}

	/**
	 * Create a key string using the given alphabet of characters. This is meant
	 * to help create license-key style strings, where the alphabet is restricted
	 * to all upper case Latin letter and numbers, for example.
	 * 
	 * @param nonsense text added to the "seed" for value generation
	 * @param alphabet the set of characters allowed in the output string
	 * @return a likely unique string using the given alphabet.
	 */
	public static String createKeyUsingAlphabet ( String nonsense, String alphabet )
	{
		final int alphabetLength = alphabet.length ();
		final byte[] bytes = createValue ( nonsense );
		final StringBuffer sb = new StringBuffer ();
		for ( byte b : bytes )
		{
			final int letterIndex = Math.abs ( b ) % alphabetLength;
			final char letter = alphabet.charAt ( letterIndex );
			sb.append ( letter );
		}
		return sb.toString ();
	}

	/**
	 * Create a key string using the given alphabet of characters, with the requested length.
	 * @param nonsense text added to the "seed" for value generation
	 * @param alphabet the set of characters allowed in the output string
	 * @param length the length of the output string
	 * @return a likely unique string of the given length using the given alphabet.
	 */
	public static String createKeyUsingAlphabet ( String nonsense, String alphabet, int length )
	{
		String result = createKeyUsingAlphabet ( nonsense, alphabet );
		while ( result.length () < length )
		{
			result += createKeyUsingAlphabet ( nonsense, alphabet );
		}
		return result.substring ( 0, length );
	}

	/**
	 * Create a URL compatible unique key
	 * @param nonsense text added to the "seed" for value generation
	 * @return a likely unique string that works easily in URLs
	 */
	public static String createUrlKey ( String nonsense )
	{
		return createKeyUsingAlphabet ( nonsense, kUrlKeyAlphabet );
	}

	/**
	 * Create a key string that uses a Microsoft style license key alphabet. 
	 * @param nonsense text added to the "seed" for value generation
	 * @return a Microsoft style license key string
	 */
	public static String createMsStyleKeyString ( String nonsense )
	{
		final String original = createKeyUsingAlphabet ( nonsense, kLicenseKeyAlphabet );

		final StringBuffer sb = new StringBuffer ();
		int position = -1;
		for ( int i=0; i<original.length (); i++ )
		{
			final char letter = original.charAt ( i );
			position++;
			if ( position > 0 && position % 5 == 0 )
			{
				sb.append ( " " );
			}
			sb.append ( letter );
		}
		return sb.toString ();
	}

	private static final String kLicenseKeyAlphabet = "123456789BCDFGHJKLMNPQRTVWXYZ";
	private static final String kUrlKeyAlphabet = "0123456789ABCDFGHJKLMNPQRTVWXYZabcdefhigjklmnopqrstuvwxyz";

	private static byte[] createValue ( String nonsense )
	{
		final StringBuffer sb = new StringBuffer ();
		sb.append ( UUID.randomUUID ().toString () );
		if ( nonsense != null )
		{
			sb.append ( nonsense );
		}
		sb.append ( System.currentTimeMillis () );

		return OneWayHasher.pbkdf2Hash ( UUID.randomUUID ().toString (), sb.toString() );
	}
}
