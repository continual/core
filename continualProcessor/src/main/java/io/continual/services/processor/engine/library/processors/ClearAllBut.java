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

package io.continual.services.processor.engine.library.processors;

import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonVisitor;

public class ClearAllBut implements Processor
{
	public ClearAllBut ( JSONObject config )
	{
		fKeys = new TreeSet<> ();
		fKeys.addAll ( JsonVisitor.arrayToList ( config.optJSONArray ( "keys" ) ) );
	}

	public ClearAllBut ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		this ( config );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final Message msg = context.getMessage ();
		final TreeSet<String> allKeys = new TreeSet<> ();
		allKeys.addAll ( msg.accessRawJson ().keySet () );
		for ( String key : allKeys )
		{
			if ( !fKeys.contains ( key ) )
			{
				msg.clearValue ( key );
			}
		}
	}

	private TreeSet<String> fKeys;
}
