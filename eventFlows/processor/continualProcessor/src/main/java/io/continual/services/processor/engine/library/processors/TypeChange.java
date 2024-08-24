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

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.TypeConvertor;

public class TypeChange implements Processor
{
	public enum Type
	{
		STRING,
		NUMBER,
		BOOLEAN
	}

	public TypeChange ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( config );
	}

	public TypeChange ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fFromField = config.getString ( "from" );
			fTargetType = Type.valueOf ( config.getString ( "toType" ).toUpperCase () );

			fToField = config.optString ( "to", null );
		}
		catch ( IllegalArgumentException e )
		{
			throw new BuildFailure ( e );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final Message msg = context.getMessage ();
		final String fromVal = msg.getString ( fFromField );
		final String to = fToField == null ? fFromField : fToField;
		switch ( fTargetType )
		{
			case BOOLEAN:
				msg.putValue ( to, TypeConvertor.convertToBooleanBroad ( fromVal ) );
				break;

			case NUMBER:
				msg.putValue ( to, TypeConvertor.convertToDouble ( fromVal, 0.0 ) );
				break;

			default:
			case STRING:
				msg.putValue ( to, fromVal );
				break;
		}
	}

	private final String fFromField;
	private final String fToField;
	private final Type fTargetType;
}
