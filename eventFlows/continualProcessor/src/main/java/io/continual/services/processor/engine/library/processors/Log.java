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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class Log implements Processor
{
	public Log ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( config.optString ( "logName", "" ), config.optString ( "format", "{}" ), config.optString ( "expression", null ) );
	}

	public Log ( ) throws BuildFailure
	{
		this ( "" );
	}

	public Log ( String logName ) throws BuildFailure
	{
		this ( logName, "{}" );
	}

	public Log ( String logName, String format ) throws BuildFailure
	{
		this ( logName, format, null );
	}

	public Log ( String logName, String format, String eval ) throws BuildFailure
	{
		fLog = LoggerFactory.getLogger ( logName );
		fFormat = format;
		fEval = eval;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String line = fEval == null ?
			context.getMessage ().toLine () :
			context.evalExpression ( fEval )
		;
		if ( fFormat != null )
		{
			fLog.info ( fFormat, line );
		}
		else
		{
			fLog.info ( line );
		}
	}

	private final Logger fLog;
	private final String fFormat;
	private final String fEval;
}
