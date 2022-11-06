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

package io.continual.services.processor.library.onapmr.sources;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.onap.services.subscriber.OnapMrFetchResponse;
import io.continual.onap.services.subscriber.OnapMsgRouterSubscriber;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class OnapMrSource extends BasicSource 
{
	public OnapMrSource ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		super ( config );
		try
		{
			final ExpressionEvaluator ee = clc.getServiceContainer ().getExprEval ( config );
			
			final OnapMsgRouterSubscriber.Builder subBuilder = OnapMsgRouterSubscriber.builder ();

			// hosts can be an array of hostnames, or a single string with comma delimiter
			final Object hostsObj = config.get ( "hosts" );
			if ( hostsObj instanceof JSONArray )
			{
				JsonVisitor.forEachElement ( (JSONArray) hostsObj, new ArrayVisitor<String,JSONException>()
				{
					@Override
					public boolean visit ( String hostName ) throws JSONException
					{
						subBuilder.withHost ( ee.evaluateText ( hostName ) );
						return true;
					}
				} );
			}
			else if ( hostsObj instanceof String )
			{
				final String hostList = ee.evaluateText ( (String) hostsObj );
				for ( String host : hostList.split ( "," ) )
				{
					if ( host != null && host.length () > 0 )
					{
						subBuilder.withHost ( host );
					}
				}
			}
			else
			{
				throw new BuildFailure ( "hosts must be an array or a string" );
			}

			subBuilder
				.onTopic ( ee.evaluateText ( config.getString ( "topic" ) ) )
				.usingProxy ( ee.evaluateText ( config.optString ( "proxy", null ) ) )
				.inGroup ( ee.evaluateText ( config.getString ( "subGroup" ) ) )
				.withSubscriberId ( ee.evaluateText ( config.optString ( "subId", null ) ) )
			;

			// auth...
			final String user = ee.evaluateText ( config.optString ( "username", null ) );
			if ( user != null && user.length () > 0 )
			{
				subBuilder.asUser ( user, ee.evaluateText ( config.optString ( "password", null ) ) );
			}

			final String apiKey = ee.evaluateText ( config.optString ( "apiKey", null ) );
			if ( apiKey != null && apiKey.length () > 0 )
			{
				subBuilder.withApiKey ( apiKey, ee.evaluateText (  config.optString ( "apiSecret", null ) ) );
			}

			
			fSub = subBuilder.build ();
			fPending = new LinkedList<> ();
			fRefillSize = Math.max ( 1, config.optInt ( "refillBelow", 1 ) );

			final boolean background = config.optBoolean ( "backgroundFetch", true );
			if ( background )
			{
				fExecutorService = Executors.newSingleThreadExecutor ();
			}
			else
			{
				fExecutorService = null;
			}
			fPendingRequest = null;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	private final OnapMsgRouterSubscriber fSub;
	private final LinkedList<String> fPending;
	private final int fRefillSize;

	private final ExecutorService fExecutorService;
	private Future<OnapMrFetchResponse> fPendingRequest;

	private static final Logger log = LoggerFactory.getLogger ( OnapMrSource.class );
	
	@Override
	protected synchronized MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		// if we don't have a pending request and we're ready to reload, fire up a request
		if ( ( fPendingRequest == null || fPendingRequest.isDone () ) && fPending.size () < fRefillSize )
		{
			log.debug ( "pendings: {}, refill at {}. Time to fetch.", fPending.size (), fRefillSize );
			refill ();
		}

		// meanwhile if we have a message, return it
		if ( fPending.size () > 0 )
		{
			final String str = fPending.remove ();

			JSONObject data;
			try
			{
				data = new JSONObject ( new CommentedJsonTokener ( str ) );
			}
			catch ( JSONException x )
			{
				data = new JSONObject ()
					.put ( "data", str )
				;
			}
			return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( data ) );
		}

		// otherwise nothing available now
		return null;
	}

	protected synchronized void addMessageToPending ( String msg )
	{
		fPending.add ( msg );
	}

	private OnapMrFetchResponse runFetch ()
	{
		final OnapMrFetchResponse response = fSub.fetch ();
		if ( response.isSuccess () )
		{
			try
			{
				int count=0;
				while ( !response.isEof () )
				{
					final String msg = response.consumeNext ( 500 );
					if ( msg != null )
					{
						addMessageToPending ( msg );
						count++;
					}
				}
				log.info ( "OnapMrSource fetch complete with {} msgs", count );
			}
			catch ( InterruptedException e )
			{
				log.warn ( "OnapMrSource fetch interrupted." );
				Thread.currentThread ().interrupt ();
			}
		}
		return response;
	}
	
	private synchronized void refill ( )
	{
		if ( fExecutorService != null )
		{
			if ( fPendingRequest != null && !fPendingRequest.isDone () ) 
			{
				throw new IllegalStateException ( "Already have a running fetch." );
			}

			fPendingRequest = fExecutorService.submit ( new Callable<OnapMrFetchResponse> ()
			{
				@Override
				public OnapMrFetchResponse call () throws Exception
				{
					return runFetch ();
				}
			} );
		}
		else
		{
			runFetch ();
		}
	}
}
