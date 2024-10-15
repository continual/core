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

package io.continual.services.processor.engine.library.filters;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.json.JsonEval;

public class HasField implements Filter
{
	public HasField ( ConfigLoadContext sc, JSONObject config )
	{
		this ( config.getString ( "key" ) );
	}

	public HasField ( String fieldName )
	{
		fFieldName = fieldName;
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "key", fFieldName )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		return JsonEval.hasKey ( ctx.getMessage ().accessRawJson (), fFieldName );
	}

	private final String fFieldName;
}
