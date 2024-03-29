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

package io.continual.services.processor.engine.library.services.dedupe.filters;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.services.dedupe.services.DedupeService;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;

public class IsDupe implements Filter
{
	public IsDupe ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( config );
	}

	public IsDupe ( JSONObject config ) throws BuildFailure
	{
		fSvcName = config.getString ( "service" );
		fKey = config.getString ( "key" );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "service", fSvcName )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		final DedupeService ds = ctx.getStreamProcessingContext().getNamedObject ( fSvcName, DedupeService.class );
		if ( ds != null )
		{
			return ds.exists ( ctx.evalExpression ( fKey ) );
		}
		return false;
	}

	private final String fSvcName;
	private final String fKey;
}
