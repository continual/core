package io.continual.services.rcvr;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.htmlForms.CHttpFormPostWrapper;
import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessagePublisher.TopicUnavailableException;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.data.csv.CsvCallbackReader;
import io.continual.util.data.csv.CsvCallbackReader.RecordHandler;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

/**
 * Handle inbound user events.
 */
public class ReceiverApi<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	public static final String kSetting_MaxSenderStreamSize = "receiver.events.io.maxInboundMessageSize";
	public static final int kDefault_MaxSenderStreamSize = 1024*1024*4;	// 4 MB

	public static final String DEFAULT_TOPIC = "";
	public static final String DEFAULT_PARTITION = "";

	public ReceiverApi ( ServiceContainer sc, JSONObject prefs ) throws BuildFailure
	{
		super ( sc, prefs );

		fNodeId = sfProcessId;

		final String pubSvcName = prefs.optString ( "publisherService", "publisher" );
		fMsgPublisher = sc.get ( pubSvcName, ContinualMessagePublisher.class );
		if ( fMsgPublisher == null ) 
		{
			throw new BuildFailure ( "ReceiverApi couldn't find publisher service (" + pubSvcName + ")" );
		}
		try
		{
			fSink = fMsgPublisher.getTopic ( NotifierTopics.USER_EVENTS.toString () );
		}
		catch ( TopicUnavailableException e )
		{
			throw new BuildFailure ( "ReceiverApi couldn't open topic " + NotifierTopics.USER_EVENTS, e );
		}

		fContentTypeHandlers = new HashMap<> ();
		fRequestReadLimit = prefs.optInt ( kSetting_MaxSenderStreamSize, kDefault_MaxSenderStreamSize );

		setupContentHandlers ();
	}

	public void usage ( CHttpRequestContext context )
	{
		sendStatusOk ( context, "Please review the API documentation for the receiver service. :-)" );
	}

	public void postEvents ( CHttpRequestContext context )
	{
		postEvents ( context, DEFAULT_TOPIC );
	}

	public void postEvents ( CHttpRequestContext context, final String topic )
	{
		postEvents ( context, topic, DEFAULT_PARTITION );
	}

	public void postEvents ( CHttpRequestContext context, final String topic, final String eventStreamName )
	{
		handleWithApiAuth ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, final UserContext<I> user )
			{
				final Counter count = new Counter ();

				try
				{
					// process the inbound payload into a JSON array of messages
					final List<JSONObject> incoming = readPayloadForMessages ( context );
					if ( incoming == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, 
							"Unsupported content type: " + context.request ().getContentType () + " or there was a problem reading the payload." );
						return;
					}

					// determine the account ID and topic for this post
					final String[] acctIdAndTopic = getAcctIdAndTopic ( topic, user );

					// FIXME: for now we don't have ACLs on topics available, so users can only post to their
					// own topics.
					if ( !acctIdAndTopic[0].equals ( user.getEffectiveUserId () ) )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "You cannot post to this stream." );
						return;
					}

					final String internalMsgStreamName = acctIdAndTopic[0] + "/" + acctIdAndTopic[1] + "/" + eventStreamName;
					final ContinualMessageStream stream = ContinualMessageStream.fromName ( internalMsgStreamName );

					// for each message, run the user's processing and send it along to the output channel 
					for ( JSONObject msgData : incoming )
					{
						final String id = makeId ();

						final ContinualMessage msg = ContinualMessage.builder ()
							.createdBy ( user.getUser () )
							.withMessageData ( msgData )
							.withMetaDataSection ( kMetadataGroup )
								.set ( kMessageId, id )
								.set ( kIntendedAccount, acctIdAndTopic[0] )
								.set ( kIntendedTopic, acctIdAndTopic[1] )
								.set ( kEventStreamName, eventStreamName )
								.set ( kSource, context.request ().getBestRemoteAddress () )
								.close ()
							.build ()
						;

						fSink.send ( stream, msg );

						count.bump ();
					}

					sendStatusOk ( context,
						new JSONObject ()
							.put ( "received", count.getCount () )
					);
				}
				catch ( IOException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, e.getMessage() );
				}
				catch ( JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, e.getMessage() );
				}
			}
		} );
	}

	private static final String kMetadataGroup = "rcvr";
	private static final String kMessageId = "msgId";
	private static final String kIntendedAccount = "account";
	private static final String kIntendedTopic = "topic";
	private static final String kEventStreamName = "eventStream";
	private static final String kSource = "source";

	private static class Counter
	{
		Counter () { fCount = 0; }
		long getCount () { return fCount; }
		void bump () { fCount++; }
		private long fCount;
	}

	private String makeId ()
	{
		return fNodeId + ":" + sfCounter.addAndGet ( 1L );
	}

	/**
	 * Use the requested topic name and the user to return an account ID and topic name
	 * for message posting. The topic name may an empty string, the name of a topic in
	 * the user's account, or an account ID and topic name separated by a colon (:) to
	 * indicate that the user wishes to post to another account's topic.
	 * 
	 * @param topic a topic name, which can be an empty string but not null
	 * @param user
	 * @return a two entry array of account ID and topic.
	 */
	private String[] getAcctIdAndTopic ( String topic, UserContext<I> user )
	{
		String acctId = user.getEffectiveUserId ();
		String topicResult = topic;

		if ( topic != null )
		{
			final int colon = topic.indexOf ( ':' );
			if ( colon > -1 )
			{
				acctId = topic.substring ( 0, colon );
				topicResult = topic.substring ( colon + 1 );
			}
		}
		return new String[] { acctId, topicResult };
	}

	/**
	 * Use the request's content type to parse the payload into messages 
	 * @param context
	 * @return a list of messages, or null
	 * @throws IOException
	 */
	private List<JSONObject> readPayloadForMessages ( CHttpRequestContext context ) throws IOException
	{
		final String contentType = context.request ().getContentType ();
		if ( contentType == null ) return null;

		String prefix = contentType;
		final int semi = contentType.indexOf ( ';' );
		if ( semi > -1 )
		{
			prefix = prefix.substring ( 0, semi );
		}

		final ContentTypeHandler cth = fContentTypeHandlers.get ( prefix );
		if ( cth == null ) return null;

		return cth.handle ( context );
	}

	private String readRequestBody ( CHttpRequestContext context ) throws IOException
	{
		final byte[] inData = StreamTools.readBytes ( context.request().getBodyStream (), 8192, fRequestReadLimit );
		return new String ( inData );
	}

	private final String fNodeId;
	
	private static String sfProcessId = UUID.randomUUID ().toString ();

	// 9,223,372,036,854,775,807 events before we need to worry about rollover
	private static AtomicLong sfCounter = new AtomicLong ( 0 );

	private final ContinualMessagePublisher fMsgPublisher;
	private final ContinualMessageSink fSink;
	private final int fRequestReadLimit;
	private final HashMap<String,ContentTypeHandler> fContentTypeHandlers;

	private interface ContentTypeHandler
	{
		/**
		 * Build a list of messages.
		 * @param ctx
		 * @return a list or null if there's an error
		 * @throws IOException
		 */
		List<JSONObject> handle ( CHttpRequestContext ctx ) throws IOException;
	}

	private static JSONObject rawJsonToMsg ( Object o )
	{
		JSONObject result = null;
		if ( o != null )
		{
			if ( o instanceof JSONObject )
			{
				result = (JSONObject) o;
			}
			else
			{
				result = new JSONObject ().put ( "message", o.toString () );
			}
		}
		return result;
	}

	private void setupContentHandlers ()
	{
		// JSON content
		fContentTypeHandlers.put ( MimeTypes.kAppJson, new ContentTypeHandler ()
		{
			@Override
			public List<JSONObject> handle ( CHttpRequestContext context ) throws IOException
			{
				try
				{
					final LinkedList<JSONObject> result = new LinkedList<> ();
					final String inDataStr = readRequestBody ( context );
					if ( inDataStr.startsWith ( "[" ) )
					{
						final JSONArray arr = JsonUtil.readJsonArray ( inDataStr );
						for ( int i=0; i<arr.length (); i++ )
						{
							final JSONObject msg = rawJsonToMsg ( arr.opt ( i ) );
							if ( msg != null )
							{
								result.add ( msg );
							}
						}
					}
					else
					{
						final JSONObject msg = rawJsonToMsg ( JsonUtil.readJsonValue ( inDataStr ) );
						if ( msg != null )
						{
							result.add ( msg );
						}
					}
					return result;
				}
				catch ( JSONException x )
				{
					return null;
				}
			}
		} );

		// plain text content
		fContentTypeHandlers.put ( MimeTypes.kPlainText, new ContentTypeHandler ()
		{
			@Override
			public List<JSONObject> handle ( CHttpRequestContext context ) throws IOException
			{
				final LinkedList<JSONObject> result = new LinkedList<> ();
				final String inDataStr = readRequestBody ( context );
				final JSONObject msg = rawJsonToMsg ( JSONObject.valueToString ( inDataStr ) );
				if ( msg != null )
				{
					result.add ( msg );
				}
				return result;
			}
		} );

		// web form content
		final ContentTypeHandler webFormHandler = new ContentTypeHandler ()
		{
			@Override
			public List<JSONObject> handle ( CHttpRequestContext context )
			{
				try
				{
					final LinkedList<JSONObject> result = new LinkedList<> ();
					result.add (
						JsonVisitor.mapOfStringsToObject (
							new CHttpFormPostWrapper ( context.request () ).getValues ()
						)
					);
					return result;
				}
				catch ( ParseException e )
				{
					return null;
				}
			}
		};
		fContentTypeHandlers.put ( MimeTypes.kAppWwwForm, webFormHandler );
		fContentTypeHandlers.put ( MimeTypes.kMultipartForm, webFormHandler );

		// CSV content
		fContentTypeHandlers.put ( MimeTypes.kCsv, new ContentTypeHandler ()
		{
			@Override
			public List<JSONObject> handle ( CHttpRequestContext context ) throws IOException
			{
				final LinkedList<JSONObject> result = new LinkedList<> ();

				final CHttpRequest req = context.request ();
				final CsvCallbackReader<IOException> reader = new CsvCallbackReader<IOException> (
					req.getCharParameter ( "quote", '"' ),
					req.getCharParameter ( "sep", ',' ),
					req.getBooleanParameter ( "header", false )
				);
				reader.read ( req.getBodyStream (), new RecordHandler<IOException> ()
				{
					@Override
					public boolean handler ( Map<String, String> fields ) throws IOException
					{
						result.add ( JsonVisitor.mapOfStringsToObject ( fields ) );
						return true;
					}
				} );
				return result;
			}
		} );
	}
}
