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

package io.continual.util.data.base64;

public class Base64Constants
{
	public static final char kNewline = 10;

	public static char[] nibblesToB64 = new char [64];
	public static byte[] b64ToNibbles = new byte [128];

	static
	{
		int j = 0;
		for ( char c = 'A'; c <= 'Z'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		for ( char c = 'a'; c <= 'z'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		for ( char c = '0'; c <= '9'; c++ )
		{
			nibblesToB64[j++] = c;
		}
		nibblesToB64[j++] = '+';
		nibblesToB64[j++] = '/';

		for ( int i = 0; i < b64ToNibbles.length; i++ )
		{
			b64ToNibbles[i] = -1;
		}
		for ( int i = 0; i < 64; i++ )
		{
			b64ToNibbles[nibblesToB64[i]] = (byte) i;
		}
	}
}
