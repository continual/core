package io.continual.services.model.impl.json;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.time.Clock;

public class CommonJsonDbObject implements ModelObject, AclUpdateListener
{
	public static final String kStdType_Alias = "Alias";
	public static final String kStdType_ObjectContainer = "ObjectContainer";
	
	public CommonJsonDbObject ( )
	{
		this ( null, new JSONObject () );
	}

	public CommonJsonDbObject ( String id, JSONObject rawData )
	{
		fRaw = rawData;

		// read or create a metadata object, setting create timestamp as needed 
		final JSONObject meta = getMeta ();
		fAcl = AccessControlList.deserialize ( meta.optJSONObject ( kMeta_AclTag ), this );
	}

	public static class Builder<T extends CommonJsonDbObject>
	{
		public interface Constructor<T extends CommonJsonDbObject>
		{
			T construct ( String id, JSONObject rawData );
		};

		public Builder<T> withId ( String id )
		{
			fId = id;
			return this;
		}

		public Builder<T> withData ( JSONObject newData, boolean merge )
		{
			JSONObject data = fRawData.optJSONObject ( kDataTag );
			if ( merge )
			{
				if ( data == null )
				{
					data = new JSONObject ();
					fRawData.put ( kDataTag, data );
				}
				JsonUtil.overlay ( data, newData );
			}
			else
			{
				fRawData.put ( kDataTag, JsonUtil.clone ( newData ) );
			}
			return this;
		}

		public Builder<T> withAcl ( AccessControlList acl )
		{
			getMetaBlock().put ( kMeta_AclTag, acl.asJson () );
			return this;
		}

		public Builder<T> withType ( String type )
		{
			JSONObject data = getMetaBlock ();
			JSONArray types = data.optJSONArray ( kMeta_LockedTypes );
			if ( types == null )
			{
				types = new JSONArray ();
				data.put ( kMeta_LockedTypes, types );
			}
			types.put ( type );

			return this;
		}

		public Builder<T> withCreateTimestampMs ( long createTimeMs )
		{
			JSONObject data = getMetaBlock ();
			data.put ( kMeta_CreateTs, createTimeMs );
			return this;
		}

		public Builder<T> withUpdateTimestampMs ( long updateTimeMs )
		{
			JSONObject data = getMetaBlock ();
			data.put ( kMeta_UpdateTs, updateTimeMs );
			return this;
		}

		public Builder<T> constructUsing ( Constructor<T> cc )
		{
			fConstructor = cc;
			return this;
		}
		
		public T build ()
		{
			if ( fId == null )
			{
				fId = UUID.randomUUID ().toString ();
			}
			return fConstructor.construct ( fId, fRawData );
		}

		private JSONObject getMetaBlock ()
		{
			JSONObject metadata = fRawData.optJSONObject ( kMetaTag );
			if ( metadata == null )
			{
				metadata = new JSONObject ();
				fRawData.put ( kMetaTag, metadata );
			}
			return metadata;
		}

		private String fId = null;
		private JSONObject fRawData = new JSONObject ();
		private Constructor<T> fConstructor = new Constructor<T> ()
		{
			@SuppressWarnings("unchecked")
			@Override
			public T construct ( String id, JSONObject rawData )
			{
				return (T) new CommonJsonDbObject ( id, rawData );
			}
		};
	}

	@Override
	public AccessControlList getAccessControlList ()
	{
		return fAcl;
	}

	@Override
	public void onAclUpdate ( AccessControlList acl )
	{
		// sanity check the argument
		if ( acl != fAcl ) throw new IllegalArgumentException ( "ACL update notification from wrong ACL." );

		// the ACL was updated; pull it back into our raw data
		getMeta ().put ( kMeta_AclTag, fAcl.asJson () );
	}

	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fRaw );
	}

	@Override
	public ModelObjectMetadata getMetadata ()
	{
		return new ModelObjectMetadata ()
		{
			@Override
			public Set<String> getLockedTypes ()
			{
				final TreeSet<String> result = new TreeSet<String>();

				final JSONObject meta = fRaw.optJSONObject ( kMetaTag );
				if ( meta != null )
				{
					final JSONArray types = meta.optJSONArray ( kMeta_LockedTypes );
					if ( types != null )
					{
						final List<String> typeList = JsonVisitor.arrayToList ( types );
						result.addAll ( typeList );
					}
				}

				return result;
			}

			@Override
			public long getCreateTimeMs ()
			{
				final JSONObject meta = fRaw.optJSONObject ( kMetaTag );
				return ( meta == null ? -1L : meta.optLong ( kMeta_CreateTs, -1L ) );
			}

			@Override
			public long getLastUpdateTimeMs ()
			{
				final JSONObject meta = fRaw.optJSONObject ( kMetaTag );
				return ( meta == null ? -1L : meta.optLong ( kMeta_UpdateTs, -1L ) );
			}

			@Override
			public JSONObject toJson ()
			{
				return JsonUtil.clone ( getMeta () );
			}
			
			@Override
			public AccessControlList getAccessControlList ()
			{
				return fAcl;
			}
		};
	}

	@Override
	public JSONObject getData ()
	{
		final JSONObject data = fRaw.optJSONObject ( kDataTag );
		return ( data == null ? new JSONObject () : JsonUtil.clone ( data ) );
	}

	@Override
	public void putData ( JSONObject data )
	{
		fRaw.put ( kDataTag, JsonUtil.clone ( data ) );
		getMeta().put ( kMeta_UpdateTs, Clock.now () );
	}

	@Override
	public void patchData ( JSONObject data )
	{
		final JSONObject existing = getData ();
		JsonUtil.copyInto ( data, existing );
		putData ( existing );
	}

	private final AccessControlList fAcl;	// this is awkwardly placed -- it's stored in the metadata object but deser'd once on the object (FIXME)
	private final JSONObject fRaw;

	public static final String kDataTag = "data";
	public static final String kMetaTag = "meta";

	public static final String kMeta_AclTag = "acl";
	public static final String kMeta_CreateTs = "createMs";
	public static final String kMeta_UpdateTs = "updateMs";
	public static final String kMeta_LockedTypes = "types";

	private JSONObject getMeta ()
	{
		JSONObject meta = fRaw.optJSONObject ( kMetaTag );
		if ( meta == null )
		{
			meta = new JSONObject ()
				.put ( kMeta_CreateTs, Clock.now () )
			;
			fRaw.put ( kMetaTag, meta );
		}
		return meta;
	}
}
