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

package io.continual.services.processor.library.onapmr.sinks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.onap.services.publisher.OnapMsgRouterBatchPublisher;
import io.continual.onap.services.publisher.OnapMsgRouterBatchPublisher.DropPolicy;
import io.continual.onap.services.publisher.OnapMsgRouterPublisher;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class OnapMrSink implements Sink
{
	public OnapMrSink ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		try
		{
			final ExpressionEvaluator ee = clc.getServiceContainer ().getExprEval ( config );

			final OnapMsgRouterPublisher.Builder pubBuilder = OnapMsgRouterPublisher.builder ();

			// hosts can be an array of hostnames, or a single string with comma delimiter
			final Object hostsObj = config.get ( "hosts" );
			if ( hostsObj instanceof JSONArray )
			{
				log.debug ( "MR hosts: {}", hostsObj.toString () );
				JsonVisitor.forEachElement ( (JSONArray) hostsObj, new ArrayVisitor<String,JSONException>()
				{
					@Override
					public boolean visit ( String hostName ) throws JSONException
					{
						pubBuilder.withHost ( ee.evaluateText ( hostName ) );
						return true;
					}
				} );
			}
			else if ( hostsObj instanceof String )
			{
				log.debug ( "MR hosts (read): {}", hostsObj );
				final String hostList = ee.evaluateText ( (String) hostsObj );
				log.debug ( "MR hosts (eval): {}", hostList );

				for ( String host : hostList.split ( "," ) )
				{
					if ( host != null && host.length () > 0 )
					{
						log.debug ( "adding MR host: {}", host );
						pubBuilder.withHost ( host );
					}
				}
			}
			else
			{
				throw new BuildFailure ( "hosts must be an array or a string" );
			}

			pubBuilder
				.onTopic ( ee.evaluateText ( config.getString ( "topic" ) ) )
				.usingProxy ( ee.evaluateText ( config.optString ( "proxy", null ) ) )
			;

			// auth...
			String userEntry = config.optString ( "user", null );	// original
			if ( userEntry == null )
			{
				userEntry = config.optString ( "username", null ); // probably nicer choice
			}
			
			final String user = ee.evaluateText ( userEntry );
			if ( user != null && user.length () > 0 )
			{
				pubBuilder.asUser ( user, ee.evaluateText ( config.optString ( "password", null ) ) );
			}

			final String apiKey = ee.evaluateText ( config.optString ( "apiKey", null ) );
			if ( apiKey != null && apiKey.length () > 0 )
			{
				pubBuilder.withApiKey ( apiKey, ee.evaluateText (  config.optString ( "apiSecret", null ) ) );
			}

			fPub = new OnapMsgRouterBatchPublisher.Builder ()
				.usingPublisher ( pubBuilder.build () )
				.batchAtMost ( config.optInt ( "batchSizeAtMost", 1000 ) )
				.batchMaxAgeMs ( config.optInt ( "batchMaxAgeMs", 1000 ) )
				.withMaxPendingCount ( config.optInt ( "maxPendingCount", 100*1000 ), DropPolicy.fromSettingString ( config.optString ( "maxPendingDropPolicy" ) ) )
				.build ()
			;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public synchronized void init ()
	{
		fPub.start ();
	}

	@Override
	public synchronized void flush ()
	{
		// nothing to do here
	}

	@Override
	public synchronized void close ()
	{
		log.warn ( "OnapMrSink closing..." );
		fPub.close ();
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		fPub.send ( new OnapMsgRouterPublisher.Message (
			context.evalExpression ( "${eventStreamName}" ),
			context.getMessage ().toJson ().toString () )
		);
	}

	private final OnapMsgRouterBatchPublisher fPub;

	private static final Logger log = LoggerFactory.getLogger ( OnapMrSink.class );
}
