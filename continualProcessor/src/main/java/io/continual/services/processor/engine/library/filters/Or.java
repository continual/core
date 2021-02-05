/*
 *	Copyright 2021, Continual.io
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

import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class Or implements Filter
{
	public Or ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		fFilters = new LinkedList<> ();

		JsonVisitor.forEachElement ( config.getJSONArray ( "filters" ), new ArrayVisitor<JSONObject,BuildFailure> ()
		{
			@Override
			public boolean visit ( JSONObject filterData ) throws JSONException, BuildFailure
			{
				fFilters.add ( Builder.withBaseClass ( Filter.class )
					.withClassNameInData ()
					.searchingPaths ( clc.getSearchPathPackages () )
					.providingContext ( clc )
					.usingData ( filterData )
					.build ()
				);
				return true;
			}
		} );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONArray filters = new JSONArray ();
		for ( Filter f : fFilters )
		{
			filters.put ( f.toJson () );
		}
		
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "filters", filters )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		for ( Filter f : fFilters )
		{
			if ( f.passes ( ctx ) )
			{
				return true;
			}
		}
		return false;
	}

	private final LinkedList<Filter> fFilters;
}
