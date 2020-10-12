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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A collection of utility functions for one-way hashing.
 */
public class OneWayHasher
{
	/**
	 * Create a digest of a message string.
	 * @param input
	 * @return a digest string.
	 */
	public static String digest ( String input )
	{
		return pbkdf2HashToString ( input, "(non-empty salt)" );
	}

	/**
	 * Create a hash of an input string and a salt string.
	 * @param input
	 * @param salt
	 * @return a hash
	 */
	public static String hash ( String input, String salt )
	{
		return pbkdf2HashToString ( input, salt );
	}

	/**
	 * Create a hash using pbkd2Hash and return the result as a string of hex characters.
	 * @param input
	 * @param salt
	 * @return a string of hex characters.
	 */
	public static String pbkdf2HashToString ( String input, String salt )
	{
		final byte[] bytes = pbkdf2Hash ( input, salt );
		return TypeConvertor.bytesToHexString ( bytes );
	}

	/**
	 * Create a hash using the PBKDF2WithHmacSHA1 algorithm given an input string and salt string.
	 * @param input
	 * @param salt
	 * @return a hash in a byte array
	 */
	public static byte[] pbkdf2Hash ( String input, String salt )
	{
		try
		{
			final String algorithm = "PBKDF2WithHmacSHA1";
			final int derivedKeyLength = 160;
			final int iterations = 20000;

			final KeySpec spec = new PBEKeySpec ( input.toCharArray (), salt.getBytes (), iterations, derivedKeyLength );
			final SecretKeyFactory f = SecretKeyFactory.getInstance ( algorithm );
			return f.generateSecret ( spec ).getEncoded ();
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( InvalidKeySpecException e )
		{
			throw new RuntimeException ( e );
		}
	}

	/**
	 * Run this utility as a program.
	 * @param args
	 */
	static public void main ( String args[] )
	{
		if ( args.length != 1 && args.length != 2 )
		{
			System.err.println ( "usage: OneWayHasher <input> [<extraSalt>]" );
		}
		else if ( args.length == 1 )
		{
			System.out.println ( pbkdf2HashToString ( args[0], "" ) );
		}
		else if ( args.length == 2 )
		{
			System.out.println ( pbkdf2HashToString ( args[0], args[1] ) );
		}
	}
}
