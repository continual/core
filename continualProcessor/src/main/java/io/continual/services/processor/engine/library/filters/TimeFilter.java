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
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.time.Clock;

public class TimeFilter implements Filter
{
	public TimeFilter ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fExpr = config.getString ( "expr" );
		fMin = readBoundary ( config, "lowerLimit" );
		fMax = readBoundary ( config, "upperLimit" );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "expr", fExpr )
			.put ( "lowerLimit", fMin.toJson () )
			.put ( "upperLimit", fMax.toJson () )
		;
		return result;
	}

	@Override
	public boolean passes ( MessageProcessingContext ctx )
	{
		final Long timeValObj = ctx.evalExpression ( fExpr, Long.class );
		final long timeVal = timeValObj == null ? 0L : timeValObj;
		return
			( fMin == null || fMin.isLessThan ( timeVal ) ) &&
			( fMax == null || fMax.isGreaterThan ( timeVal ) )
		;
	}

	private final String fExpr;
	private final Boundary fMin;
	private final Boundary fMax;

	private static Boundary readBoundary ( JSONObject config, String label ) throws BuildFailure
	{
		final String spec = config.optString ( label, null );
		if ( spec == null ) return null;
		return new Boundary ( spec );
	}
	
	private static class Boundary implements JsonSerialized
	{
		/**
		 * Forms:
		 * 
		 * 		+3h -- three hours later than current time
		 * 		-4m -- four minutes earlier than current time
		 * 		12345 -- exactly time 12345 (in epoch ms)
		 * 
		 * @param spec
		 */
		public Boundary ( String spec ) throws BuildFailure
		{
			try
			{
				spec = spec.trim ();
	
				final char first = spec.charAt ( 0 );
				if ( first == '+' )
				{
					fIsAbsolute = false;
					fLimit = readDiff ( spec.substring ( 1 ) );
				}
				else if ( first == '-' )
				{
					fIsAbsolute = false;
					fLimit = -1L * readDiff ( spec.substring ( 1 ) );
				}
				else
				{
					fIsAbsolute = true;
					fLimit = Long.parseLong ( spec );
				}
			}
			catch ( NumberFormatException x )
			{
				throw new BuildFailure ( x );
			}
		}

		@Override
		public JSONObject toJson ()
		{
			return new JSONObject ();
		}

		public boolean isGreaterThan ( long timeValMs )
		{
			return timeValMs < getTime ();
		}

		@SuppressWarnings("unused")
		public boolean isGreaterOrEqual ( long timeValMs )
		{
			return timeValMs <= getTime ();
		}

		public boolean isLessThan ( long timeValMs )
		{
			return timeValMs > getTime ();
		}

		@SuppressWarnings("unused")
		public boolean isLessThanOrEqual ( long timeValMs )
		{
			return timeValMs >= getTime ();
		}

		private long getTime ()
		{
			return fIsAbsolute ? fLimit : Clock.now () + fLimit;
		}

		private final long fLimit;
		private final boolean fIsAbsolute;
	}

	private static long readDiff ( String diff ) throws BuildFailure
	{
		final int len = diff.length ();

		if ( len < 1 ) throw new BuildFailure ( "Empty time difference value." );

		final char lastChar = diff.charAt ( len - 1 );
		final String allButLast = diff.substring ( 0, len - 1 );
		switch ( lastChar )
		{
			case 'h':
				return Long.parseLong ( allButLast ) * ( 1000L * 60 * 60 );

			case 'm':
				return Long.parseLong ( allButLast ) * ( 1000L * 60 );

			case 's':
				return Long.parseLong ( allButLast ) * ( 1000L );

			default:
				return Long.parseLong ( diff );
		}
	}
}
