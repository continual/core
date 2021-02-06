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

import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class OneOf implements Filter
{
	public OneOf ( String key, String[] vals )
	{
		fKey = key;
		fValues = new TreeSet<String> ();
		for ( String val : vals )
		{
			fValues.add ( val );
		}
	}

	public OneOf ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public OneOf ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fKey = config.getString ( "key" );
		fValues = new TreeSet<String> ();
		
		JsonVisitor.forEachElement ( config.getJSONArray ( "values" ), new ArrayVisitor<Object,JSONException> () {

			@Override
			public boolean visit ( Object val ) throws JSONException
			{
				fValues.add ( val.toString () );
				return true;
			}
			
		} );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "key", fKey )
			.put ( "values", JsonVisitor.collectionToArray ( fValues ) ) 
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		final String val = ctx.getMessage ().getValueAsString ( fKey );
		return val != null && fValues.contains ( val );
	}

	private final String fKey;
	private final TreeSet<String> fValues;
}
