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

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonUtil;

public class ShiftUp implements Processor
{
	public ShiftUp ( String fieldExpr )
	{
		fFieldExpr = fieldExpr;
	}

	public ShiftUp ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fFieldExpr = config.getString ( "to" );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String field = context.evalExpression ( fFieldExpr );
		final JSONObject msgJson = context.getMessage ().accessRawJson ();

		final JSONObject inner = JsonUtil.clone ( msgJson );

		final TreeSet<String> keys = new TreeSet<String> ( msgJson.keySet () );
		for ( String key : keys )
		{
			msgJson.remove ( key );
		}

		msgJson.put ( field, inner );
	}

	private final String fFieldExpr;
}
