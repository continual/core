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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Sha256HmacSigner
{
	private static final String kHmacSha256Algo = "HmacSHA256";

	public static String sign ( String message, String key )
	{
		try
		{
			final byte[] rawHmac = signToBytes ( message, key );
			return TypeConvertor.base64Encode ( rawHmac, Integer.MAX_VALUE );
		}
		catch ( IllegalStateException e )
		{
			throw new RuntimeException ( e );
		}
	}

	public static byte[] signToBytes ( String message, String key )
	{
		try
		{
			final SecretKey secretKey = new SecretKeySpec ( key.getBytes (), kHmacSha256Algo );
			final Mac mac = Mac.getInstance ( kHmacSha256Algo );
			mac.init ( secretKey );
			return mac.doFinal ( message.getBytes () );
		}
		catch ( InvalidKeyException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( IllegalStateException e )
		{
			throw new RuntimeException ( e );
		}
	}

	static public void main ( String args[] )
	{
		if ( args.length != 2 )
		{
			System.err.println ( "usage: Sha256HmacSigner <message> <key>" );
		}
		else if ( args.length == 2 )
		{
			System.out.println ( sign ( args[0], args[1] ) );
		}
	}
}
