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

import io.continual.services.Service.FailedToStart;
import io.continual.util.console.ConsoleProgram;

import io.continual.services.processor.config.readers.ConfigReadException;
import io.continual.services.processor.config.readers.JsonConfigReader;

public class ProgramRunner extends ConsoleProgram
{
	public static void main ( String[] args )
	{
		if ( args.length != 1 )
		{
			System.err.println ( "usage: ProgramRunner <program>" );
			return;
		}

		try
		{
			new Engine ( new JsonConfigReader ().read ( args[0] ) )
				.startAndWait ()
			;
		}
		catch ( ConfigReadException e )
		{
			System.err.println ( e.getMessage () );
		}
		catch ( FailedToStart e )
		{
			System.err.println ( e.getMessage () );
		}
	}
}
