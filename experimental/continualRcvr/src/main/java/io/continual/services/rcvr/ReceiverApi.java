package io.continual.services.rcvr;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.htmlForms.CHttpFormPostWrapper;
import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.IamService;
import io.continual.iam.identity.UserContext;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessagePublisher.TopicUnavailableException;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.restHttp.ApiContextHelper;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;

/**
 * Handle inbound user events.
 */
public class ReceiverApi extends ApiContextHelper
{
	public static final String kSetting_MaxSenderStreamSize = "receiver.events.io.maxInboundMessageSize";
	public static final int kDefault_MaxSenderStreamSize = 1024*1024*4;	// 4 MB

	public static final String DEFAULT_TOPIC = "";
	public static final String DEFAULT_PARTITION = "";

	public ReceiverApi ( ServiceContainer sc, JSONObject prefs ) throws BuildFailure
	{
		fNodeId = sfProcessId;

		final String acctSvcName = prefs.optString ( "accountsService", "accounts" );
		fAccts = sc.get ( acctSvcName, IamService.class );
		if ( fAccts == null ) 
		{
			throw new BuildFailure ( "ReceiverApi couldn't find accounts service (" + acctSvcName + ")" );
		}

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

	}

	public void usage ( CHttpRequestContext context )
	{
		ApiContextHelper.sendStatusOk ( context, "Please review the API documentation for the receiver service. :-)" );
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
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, final UserContext user )
			{
				final Counter count = new Counter ();

				try
				{
					// process the inbound payload into a JSON array of messages
					final List<JSONObject> incoming = readPayloadForMessages ( context );
					if ( incoming == null )
					{
						ApiContextHelper.sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, 
							"Unsupported content type: " + context.request ().getContentType () + " or there was a problem reading the payload." );
						return null;
					}

					// determine the account ID and topic for this post
					final String[] acctIdAndTopic = getAcctIdAndTopic ( topic, user );

					// FIXME: for now we don't have ACLs on topics available, so users can only post to their
					// own topics.
					if ( !acctIdAndTopic[0].equals ( user.getEffectiveUserId () ) )
					{
						ApiContextHelper.sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "You cannot post to this stream." );
						return null;
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
					ApiContextHelper.sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, e.getMessage() );
				}
				catch ( JSONException e )
				{
					ApiContextHelper.sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, e.getMessage() );
				}
				return null;
			}
		} );
	}

	private static final String kMetadataGroup = "rcvr";
	private static final String kMessageId = "msgId";
	private static final String kIntendedAccount = "account";
	private static final String kIntendedTopic = "topic";
	private static final String kEventStreamName = "eventStream";

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
	private String[] getAcctIdAndTopic ( String topic, UserContext user )
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

		final ContentTypeHandler cth = fContentTypeHandlers.get ( contentType );
		if ( cth == null ) return null;

		return cth.handle ( context );
	}

	private static String readRequestBody ( CHttpRequestContext context ) throws IOException
	{
		final byte[] inData = StreamTools.readBytes ( context.request().getBodyStream (), 8192,
			context.systemSettings ().getInt ( kSetting_MaxSenderStreamSize, kDefault_MaxSenderStreamSize ) );
		return new String ( inData );
	}

	private final String fNodeId;
	
	private static String sfProcessId = UUID.randomUUID ().toString ();

	// 9,223,372,036,854,775,807 events before we need to worry about rollover
	private static AtomicLong sfCounter = new AtomicLong ( 0 );

	private final IamService<?,?> fAccts;
	private final ContinualMessagePublisher fMsgPublisher;
	private final ContinualMessageSink fSink;

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
	
	private static HashMap<String,ContentTypeHandler> fContentTypeHandlers = new HashMap<> ();
	static
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
	}
}
