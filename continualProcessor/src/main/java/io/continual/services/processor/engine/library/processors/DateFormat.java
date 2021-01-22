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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class DateFormat implements Processor
{
	private enum DateFormatType
	{
		EPOCH_SECONDS,
		EPOCH_MILLIS,
		TEXT
	}

	public DateFormat ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( config );
	}

	public DateFormat ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fFromField = config.getString ( "from" );
			fFromFormat = config.getString ( "fromFormat" );
			if ( fFromFormat.startsWith ( "#sec" ) )
			{
				fFromType = DateFormatType.EPOCH_SECONDS;
				fFromFormatter = null;
			}
			else if ( fFromFormat.startsWith ( "#milli" ) || fFromFormat.equals ( "#ms" ) )
			{
				fFromType = DateFormatType.EPOCH_MILLIS;
				fFromFormatter = null;
			}
			else
			{
				fFromType = DateFormatType.TEXT;
				fFromFormatter = new SimpleDateFormat ( fFromFormat );
			}

			fToField = config.optString ( "to", null );
			fTargetFormat = config.getString ( "toFormat" );
			if ( fTargetFormat.startsWith ( "#sec" ) )
			{
				fToType = DateFormatType.EPOCH_SECONDS;
				fToFormatter = null;
			}
			else if ( fTargetFormat.startsWith ( "#milli" ) || fFromFormat.equals ( "#ms" ) )
			{
				fToType = DateFormatType.EPOCH_MILLIS;
				fToFormatter = null;
			}
			else
			{
				fToType = DateFormatType.TEXT;
				fToFormatter = new SimpleDateFormat ( fTargetFormat );
			}
		}
		catch ( IllegalArgumentException | JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		try
		{
			final Message msg = context.getMessage ();
			final String fromVal = msg.getValueAsString ( fFromField );
	
			long epochMs = 0L;
			switch ( fFromType )
			{
				case EPOCH_SECONDS:
					epochMs = Long.parseLong ( fromVal ) * 1000L;
					break;
	
				case EPOCH_MILLIS:
					epochMs = Long.parseLong ( fromVal );
					break;
	
				case TEXT:
				{
					try
					{
						epochMs = fFromFormatter.parse ( fromVal ).getTime ();
					}
					catch ( ParseException e )
					{
						context.warn ( "DateFormat could parse [" + fromVal + "] using [" + fFromFormatter + "]." );
						return;
					}
				}
				break;
			}
	
			String result = "";
			switch ( fToType )
			{
				case EPOCH_SECONDS:
					result = Long.toString ( epochMs / 1000L );
					break;
	
				case EPOCH_MILLIS:
					result = Long.toString ( epochMs );
					break;
	
				case TEXT:
					result = fToFormatter.format ( new Date ( epochMs ) );
					break;
			}
	
			final String to = fToField == null ? fFromField : fToField;
			msg.putValue ( to, result );
		}
		catch ( NumberFormatException x )
		{
			context.warn ( "Number format problem: " + x.getMessage () );
		}
	}

	private final String fFromField;
	private final DateFormatType fFromType;
	private final String fFromFormat;
	private final SimpleDateFormat fFromFormatter;

	private final String fToField;
	private final DateFormatType fToType;
	private final String fTargetFormat;
	private final SimpleDateFormat fToFormatter;
}
