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

import java.util.regex.Pattern;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;

public class Matches implements Filter
{
	public Matches ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( config );
	}

	public Matches ( JSONObject config ) throws BuildFailure
	{
		fValueExpr = config.getString ( "value" );

		fPatternText = config.getString ( "pattern" );
		fPattern = Pattern.compile ( fPatternText );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "pattern", fPatternText )
			.put ( "value", fValueExpr )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		final String value = ctx.evalExpression ( fValueExpr );
		return fPattern.matcher ( value ).matches ();
	}

	private final String fPatternText;
	private final Pattern fPattern;
	private final String fValueExpr;
}
