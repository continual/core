package io.continual.messaging;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.iam.identity.Identity;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;

/**
 * A message is structured as a JSON document
 */
public class ContinualMessage implements JsonSerialized
{
	public static class MetadataSectionBuilder
	{
		public MetadataSectionBuilder ( Builder b, JSONObject data )
		{
			fBuilder = b;
			fData = data;
		}

		public MetadataSectionBuilder set ( String key, Object val )
		{
			try
			{
				fData.put ( key, val );
			}
			catch ( JSONException x )
			{
				throw new IllegalArgumentException ( x );
			}
			return this;
		}

		public Builder close ()
		{
			return fBuilder;
		}

		private final Builder fBuilder;
		private final JSONObject fData;
	}

	public static class Builder
	{
		/**
		 * Add the creation time to this message
		 * @param timeMs timestamp
		 * @return this builder
		 */
		public Builder createdAt ( long timeMs )
		{
			return this
				.withMetaDataSection ( kStdMetadata )
				.set ( kStdMetata_CreateTime, timeMs )
				.close ()
			;
		}

		/**
		 * Add the message creator to this message
		 * @param user a user
		 * @return this builder
		 */
		public Builder createdBy ( Identity user )
		{
			return this
				.withMetaDataSection ( kStdMetadata )
				.set ( kStdMetata_CreatedBy, user.getId () )
				.close ()
			;
		}

		/**
		 * Add message data to the message
		 * @param data message payload
		 * @return this builder
		 */
		public Builder withMessageData ( JSONObject data )
		{
			fPayload = JsonUtil.clone ( data );
			return this;
		}

		/**
		 * Add message data to the message
		 * @param data message payload
		 * @return this builder
		 */
		public Builder withMessageData ( String data )
		{
			fPayload = new JSONObject ().put ( "data", data );
			return this;
		}

		/**
		 * Start a metadata section
		 * @param sectionName a section of metadata
		 * @return a metadata section that must be closed
		 */
		public MetadataSectionBuilder withMetaDataSection ( String sectionName )
		{
			JSONObject data = fMetadata.optJSONObject ( sectionName );
			if ( data == null )
			{
				data = new JSONObject ();
				fMetadata.put ( sectionName, data );
			}
			return new MetadataSectionBuilder ( this, data );
		}

		/**
		 * Build the message
		 * @return a message
		 */
		public ContinualMessage build ()
		{
			return new ContinualMessage ( fPayload, fMetadata );
		}

		private JSONObject fPayload = new JSONObject ();
		private JSONObject fMetadata = new JSONObject ();
	}

	/**
	 * Construct a builder for a message 
	 * @return a message builder
	 */
	public static Builder builder () { return new Builder(); }

	/**
	 * Instantiate a message from a serialized string (normally from toString())
	 * @param jsonString
	 * @return a new message
	 */
	public static ContinualMessage fromSerializedString ( String jsonString )
	{
		final JSONObject top = new JSONObject ( new CommentedJsonTokener ( jsonString ) );
		final JSONObject meta = top.optJSONObject ( kMetadataKey );
		top.remove ( kMetadataKey );
		return new ContinualMessage ( top, meta );
	}

	/**
	 * Construct a message from JSON data
	 * @param data if null, an empty JSON document is used. If this contains our standard metadata tag, that part is overwritten
	 */
	public ContinualMessage ( JSONObject data )
	{
		this ( data, null );
	}

	/**
	 * Construct a message from JSON data
	 * @param data if null, an empty JSON document is used. If this contains our standard metadata tag, that part is overwritten
	 * @param meta a metadata structure. If null, an empty JSON document is used.
	 */
	public ContinualMessage ( JSONObject data, JSONObject meta )
	{
		fMessageData = data == null ? new JSONObject () : JsonUtil.clone ( data );
		fMessageData.put ( kMetadataKey, meta == null ? new JSONObject () : JsonUtil.clone ( meta ) );
	}

	@Override
	public String toString ()
	{
		return fMessageData.toString ();
	}
	
	/**
	 * @return a copy of this message's data
	 */
	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fMessageData );
	}

	/**
	 * return a copy of this message's data without the metadata structure
	 * @return a copy of the message data
	 */
	public JSONObject getMessagePayload ()
	{
		final JSONObject result = toJson ();
		result.remove ( kMetadataKey );
		return result;
	}

	@Override
	public int hashCode ()
	{
		return JsonUtil.hash ( fMessageData );
	}

	@Override
	public boolean equals ( Object that )
	{
		if ( this == that ) return true;
		if ( that == null ) return false;
		if ( getClass () != that.getClass () ) return false;
		
		final String thisStr = JsonUtil.writeConsistently ( fMessageData );
		final String thatStr = JsonUtil.writeConsistently ( ((ContinualMessage)that).fMessageData );
		return thisStr.equals ( thatStr );
	}

	private final JSONObject fMessageData;

	private static final String kMetadataKey = "∞ⓜⓔⓣⓐ∞";
	private static final String kStdMetadata = "standard";
	private static final String kStdMetata_CreateTime = "createTimeMs";
	private static final String kStdMetata_CreatedBy = "createdBy";
}
