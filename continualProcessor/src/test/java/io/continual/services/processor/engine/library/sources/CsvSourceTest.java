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

package io.continual.services.processor.engine.library.sources;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.engine.runtime.Engine;
import junit.framework.TestCase;

public class CsvSourceTest extends TestCase
{
	@Test
	public void testBasicRead () throws BuildFailure, IOException, InterruptedException
	{
		try ( final CsvSource src = new CsvSource (
			new JSONObject()
				.put ( "pipeline", "default" )
				.put ( "data", "io/continual/services/processor/engine/library/sources/basicData.csv" )
			)
		)
		{
			assertFalse ( src.isEof () );

			final Engine e = new Engine ( new Program () );
			final StreamProcessingContext spc = e.new StdStreamProcessingContext ( src );
			
			MessageAndRouting msg;
			do
			{
				msg = src.getNextMessage ( spc, 100, TimeUnit.MILLISECONDS );
			}
			while ( msg != null );

			assertTrue ( src.isEof () );
			assertNull ( src.getNextMessage ( spc, 100, TimeUnit.MILLISECONDS ) );
		}
		
	}
}
