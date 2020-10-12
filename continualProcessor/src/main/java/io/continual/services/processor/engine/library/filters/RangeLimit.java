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

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;

import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;

public class RangeLimit implements Filter
{
	public RangeLimit ( String key, double min, double max )
	{
		fKey = key;
		fMin = min;
		fMax = max;
	}

	public RangeLimit ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fKey = config.getString ( "key" );
		fMin = config.optDouble ( "min" );
		fMax = config.optDouble ( "max" );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "key", fKey )
		;
		if ( !Double.isNaN ( fMin ) ) result.put ( "min", fMin );
		if ( !Double.isNaN ( fMax ) ) result.put ( "max", fMax );
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		final double val = ctx.getMessage ().getDouble ( fKey, Double.NaN );

		if ( Double.isNaN ( val ) ) return false;
		if ( !Double.isNaN ( fMin ) && val < fMin ) return false;
		if ( !Double.isNaN ( fMax ) && val > fMax ) return false;

		return true;
	}

	private final String fKey;
	private final double fMin;
	private final double fMax;
}
