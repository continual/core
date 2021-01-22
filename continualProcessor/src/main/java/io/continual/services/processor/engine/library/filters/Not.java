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

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;

public class Not implements Filter
{
	public Not ( Filter f )
	{
		fFilter = f;
	}

	public Not ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		fFilter = Builder.withBaseClass ( Filter.class )
			.withClassNameInData ()
			.searchingPaths ( clc.getSearchPathPackages () )
			.providingContext ( clc )
			.usingData ( config.optJSONObject ( "filter" ) )
			.build ()
		;
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "filter", fFilter.toJson () )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		return !fFilter.passes ( ctx );
	}

	private final Filter fFilter;
}
