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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class Base64OutputStream extends OutputStream
{
	public Base64OutputStream ( OutputStream downstream )
	{
		this ( downstream, kMaxLine );
	}

	public Base64OutputStream ( OutputStream downstream, int maxPerLine )
	{
		fDownstream = downstream;
		fPendings = new LinkedList<Byte> ();
		fWrittenToLine = 0;
		fMaxLine = maxPerLine > 0 ? maxPerLine : kMaxLine;
	}

	@Override
	public void write ( int b )
		throws IOException
	{
		fPendings.add ( (byte) b );
		writePendings ( false );
	}

	@Override
	public void close () throws IOException
	{
		writePendings ( true );
		fDownstream.close ();
	}

	private int writeNow ()
	{
		int result = 0;
		int pending = fPendings.size ();
		if ( pending > kBufferSize )
		{
			result = kBufferSize;
		}
		return result;
	}
	
	private void writePendings ( boolean pad ) throws IOException
	{
		int thisWrite = fPendings.size ();
		if ( !pad )
		{
			thisWrite = writeNow ();
		}

		if ( thisWrite > 0 )
		{
			byte[] bb = new byte [ thisWrite ];
			for ( int i=0; i<thisWrite; i++ )
			{
				bb[i] = fPendings.remove ();
			}
	
			char[] cc = encode ( bb );
			for ( char c : cc )
			{
				fDownstream.write ( c );
				if ( ++fWrittenToLine == fMaxLine )
				{
					fDownstream.write ( Base64Constants.kNewline );
					fWrittenToLine = 0;
				}
			}
		}
	}

	private char[] encode ( byte[] in )
	{
		int iLen = in.length;
		int oDataLen = ( iLen * 4 + 2 ) / 3; // output length without padding
		int oLen = ( ( iLen + 2 ) / 3 ) * 4; // output length including padding
		char[] out = new char [oLen];
		int ip = 0;
		int op = 0;
		while ( ip < iLen )
		{
			int i0 = in[ip++] & 0xff;
			int i1 = ip < iLen ? in[ip++] & 0xff : 0;
			int i2 = ip < iLen ? in[ip++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ( ( i0 & 3 ) << 4 ) | ( i1 >>> 4 );
			int o2 = ( ( i1 & 0xf ) << 2 ) | ( i2 >>> 6 );
			int o3 = i2 & 0x3F;
			out[op++] = Base64Constants.nibblesToB64[o0];
			out[op++] = Base64Constants.nibblesToB64[o1];
			out[op] = op < oDataLen ? Base64Constants.nibblesToB64[o2] : '=';
			op++;
			out[op] = op < oDataLen ? Base64Constants.nibblesToB64[o3] : '=';
			op++;
		}
		return out;
	}

	private static final int kMaxLine = 80;
	private static final int kBufferSize = 3*64;	// multiple of 3 for no padding
	
	private OutputStream fDownstream;
	private int fWrittenToLine;
	private final LinkedList<Byte> fPendings;
	private final int fMaxLine;
}
