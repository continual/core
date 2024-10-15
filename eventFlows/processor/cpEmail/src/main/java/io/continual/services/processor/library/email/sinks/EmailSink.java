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

package io.continual.services.processor.library.email.sinks;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.email.EmailService.MailStatus;
import io.continual.email.impl.SimpleEmailService;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;

public class EmailSink implements Sink
{
	public EmailSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public EmailSink ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public EmailSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fMailer = new SimpleEmailService ( sc.getServiceContainer (), config );
	}

	@Override
	public void init ()
	{
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		try
		{
			final Future<MailStatus> status = fMailer.mail (
				context.getMessage ().getString ( "to" ),
				context.getMessage ().getString ( "subject" ),
				context.getMessage ().getString ( "body" )
			);
			final MailStatus ms = status.get ( 30, TimeUnit.SECONDS );
			if ( ms.didFail () )
			{
				context.warn ( "Mail send problem: " + ms.getErrorMsg () );
			}
		}
		catch ( InterruptedException | ExecutionException | TimeoutException e )
		{
			context.warn ( "Mail send problem: " + e.getMessage () );
		}
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void close () throws IOException
	{
	}

	private final SimpleEmailService fMailer;
}
