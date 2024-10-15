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
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;

/**
 * Returns true if the key is missing or the value is an empty string
 */
public class IsEmpty implements Filter
{
	public IsEmpty ( ConfigLoadContext sc, JSONObject config )
	{
		fFieldName = config.getString ( "key" );
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
		final Message msg = ctx.getMessage ();
		final JSONObject data = msg.accessRawJson ();
		return !data.has ( fFieldName ) || data.get ( fFieldName ).toString ().length () == 0;
	}

	private final String fFieldName;
}
