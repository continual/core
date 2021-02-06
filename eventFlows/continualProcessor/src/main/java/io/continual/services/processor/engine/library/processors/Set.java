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
import io.continual.services.processor.engine.library.util.Setter;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class Set implements Processor
{
	public Set ( JSONObject updates )
	{
		fUpdates = JsonUtil.clone ( updates );
		fAppendArray = true;
		fEval = true;
	}

	public Set ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fUpdates = JsonUtil.clone ( config.optJSONObject ( "updates" ) );
		fAppendArray = config.optBoolean ( "appendArray", true );
		fEval = config.optBoolean ( "eval", true );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( fUpdates == null )
		{
			context.warn ( "No updates provided to Set." );
			return;
		}

		final Message msg = context.getMessage ();
		JsonVisitor.forEachElement ( fUpdates, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String key, Object t ) throws JSONException
			{
				final String keyToUse = fEval ? Setter.evaluateToString ( context, key, msg ) : key;
				final Object data = fEval ? Setter.evaluate ( context, t, msg ) : t;
				JsonEval.setValue ( msg.accessRawJson(), keyToUse, data, fAppendArray );
				return true;
			}
		} );
	}

	private final JSONObject fUpdates;
	private final boolean fAppendArray;
	private final boolean fEval;
}
