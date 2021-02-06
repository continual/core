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
package io.continual.util.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.LoggerFactory;

public class ConsoleLineReader
{
	public static String getLine ( String prompt ) throws IOException
	{
		return sfReader.getLine ( prompt );
	}

	private interface reader 
	{
		String getLine ( String prompt ) throws IOException;
	}

	static
	{
		jline.console.ConsoleReader cr = null;
		if ( Boolean.parseBoolean ( System.getProperty ( "rrJline", "true" ) ) )
		{
			try
			{
				cr = new jline.console.ConsoleReader ();
			}
			catch ( IOException e )
			{
				LoggerFactory.getLogger ( ConsoleLineReader.class ).warn ( "IOException initializing JLine. Falling back to standard Java I/O." );
				cr = null;
			}
		}

		if ( cr != null )
		{
			final jline.console.ConsoleReader crf = cr;
			sfReader = new reader ()
			{
				@Override
				public String getLine ( String prompt ) throws IOException
				{
					return crf.readLine ( prompt );
				}
			};
		}
		else
		{
			final BufferedReader br = new BufferedReader ( new InputStreamReader ( System.in ) );
			sfReader = new reader ()
			{
				@Override
				public String getLine ( String prompt ) throws IOException
				{
					System.out.print ( prompt );
					System.out.flush ();
					return br.readLine ();
				}
			};
		}
	}

	private static final reader sfReader;
}
