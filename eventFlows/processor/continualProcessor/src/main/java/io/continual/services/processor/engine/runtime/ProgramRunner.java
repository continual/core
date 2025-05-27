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

package io.continual.services.processor.engine.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.Service.FailedToStart;
import io.continual.services.processor.config.readers.ConfigReadException;
import io.continual.services.processor.config.readers.JsonConfigReader;
import io.continual.services.processor.engine.model.Program;
import io.continual.util.console.ConsoleProgram;

public class ProgramRunner extends ConsoleProgram
{
	public static void runProgram ( String[] programList )
	{
		try
		{
			final JsonConfigReader reader = new JsonConfigReader ();
			final Program program = reader.read ( programList[0].split ( "," ) );

			final Engine e = new Engine ( program );
			for ( int i=1; i<programList.length; i++ )
			{
				// so we can say ${1} or ${2} to use command line items
				e.setUserData ( ""+i, programList[i] );
			}
			e.startAndWait ();
		}
		catch ( ConfigReadException | FailedToStart e )
		{
			log.error ( e.getMessage (), e );
			System.err.println ( e.getMessage () );
		}
	}

	public static void main ( String[] args )
	{
		if ( args.length != 1 )
		{
			System.err.println ( "usage: ProgramRunner <program>" );
			return;
		}
		runProgram ( args );
	}

	private static final Logger log = LoggerFactory.getLogger ( ProgramRunner.class );
}
