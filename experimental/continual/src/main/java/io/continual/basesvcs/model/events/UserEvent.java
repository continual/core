package io.continual.basesvcs.model.events;

import org.json.JSONObject;

import io.continual.iam.identity.Identity;

/**
 * An envelope for a user-provided event.
 * @author peter
 *
 */
public class UserEvent extends BaseJsonEvent
{
	public static final String kMetaSection = "meta";
	public static final String kUserDataSection = "userdata";

	public static final String kId = "id";
	public static final String kOperator = "operator";
	public static final String kAcctId = "acctId";
	public static final String kAcctTopic = "topic";
	public static final String kAcctEventStream = "eventStream";
	public static final String kIngestTimeMs = "ingestTimeMs";

	public UserEvent ( String id, Identity operator, String acctId, String userTopic,
		String userEventStreamName, JSONObject userData )
	{
		this ( id, operator, acctId, userTopic, userEventStreamName, userData, System.currentTimeMillis () );
	}

	public UserEvent ( String id, Identity operator, String acctId, String userTopic,
		String userEventStreamName, JSONObject userData, long ingestTimeMs )
	{
		this ( new JSONObject ()
			.put ( kMetaSection, new JSONObject ()
				.put ( kId, id )
				.put ( kOperator, operator.getId () )
				.put ( kAcctId, acctId )
				.put ( kAcctTopic, userTopic )
				.put ( kAcctEventStream, userEventStreamName )
				.put ( kIngestTimeMs, ingestTimeMs ) )
			.put ( kUserDataSection, userData )
		);
	}

	public String getId () { return getMetaField ( kId ); }
	public String getOperatorId () { return getMetaField ( kOperator ); }
	public String getAcctId () { return getMetaField ( kAcctId ); }
	public String getTopic () { return getMetaField ( kAcctTopic ); }
	public String getEventStreamName () { return getMetaField ( kAcctEventStream ); }
	public long getIngestTimeMs () { return getMetaField ( kIngestTimeMs, System.currentTimeMillis () ); }

	/**
	 * Gets the user data on this event, in writeable form
	 * @return the user data
	 */
	public JSONObject getUserData () { return super.get ( kUserDataSection ); }

	/**
	 * Rebuild a user event from stored JSON
	 * @param rawData
	 * @return a user event
	 */
	public static UserEvent rebuildFrom ( JSONObject rawData )
	{
		return new UserEvent ( rawData );
	}

	/**
	 * Get the event stream name to use on the internal bus
	 * @return an event stream name string
	 */
	public String getInternalEventStreamName ()
	{
		return getAcctId() + "/" + getTopic() + "/" + getEventStreamName();
	}

	private String getMetaField ( String field )
	{
		return super.get ( kMetaSection ).getString ( field );
	}

	private long getMetaField ( String field, long defval )
	{
		return super.get ( kMetaSection ).optLong ( field, defval );
	}

	private UserEvent ( JSONObject json )
	{
		super ( json );
	}
}
