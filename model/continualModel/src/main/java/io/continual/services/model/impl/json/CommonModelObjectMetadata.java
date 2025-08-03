package io.continual.services.model.impl.json;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.time.Clock;

public class CommonModelObjectMetadata implements ModelObjectMetadata
{
	public static final String kMeta_AclTag = "acl";
	public static final String kMeta_CreateTs = "createMs";
	public static final String kMeta_UpdateTs = "updateMs";
	public static final String kMeta_VersionStamp = "versionStamp";
	public static final String kMeta_LockedTypes = "types";

	public CommonModelObjectMetadata ()
	{
		final long updateTime = Clock.now ();

		fMeta = new JSONObject ()
			.put ( kMeta_CreateTs, updateTime )
			.put ( kMeta_UpdateTs, updateTime )
			.put ( kMeta_VersionStamp, 0L )
		;
		fAcl = AccessControlList.createOpenAcl ();
	}

	public static CommonModelObjectMetadata asCloneOfData ( JSONObject meta )
	{
		return new CommonModelObjectMetadata ( JsonUtil.clone ( meta ) );
	}

	public static CommonModelObjectMetadata adoptingData ( JSONObject meta )
	{
		return new CommonModelObjectMetadata ( meta );
	}

	@Override
	public Set<String> getLockedTypes ()
	{
		return new TreeSet<String>( JsonVisitor.arrayToList ( fMeta.optJSONArray ( kMeta_LockedTypes ) ) );
	}

	@Override
	public long getCreateTimeMs ()
	{
		return fMeta.optLong ( kMeta_CreateTs, -1L );
	}

	@Override
	public long getLastUpdateTimeMs () { return fMeta.optLong ( kMeta_UpdateTs, -1L ); }

	@Override
	public long getVersionStamp () { return fMeta.optLong ( kMeta_VersionStamp, 0L ); }

	@Override
	public long bumpVersionStamp ()
	{
		final long newVersion = 1 + fMeta.optLong ( kMeta_VersionStamp, 0L );
		fMeta.put ( kMeta_VersionStamp, newVersion );
		return newVersion;
	}

	@Override
	public JSONObject toJson ()
	{
		packAcl ();
		return JsonUtil.clone ( fMeta );
	}
	
	@Override
	public AccessControlList getAccessControlList ()
	{
		return fAcl;
	}

	private CommonModelObjectMetadata ( JSONObject meta )
	{
		fMeta = meta;
		fAcl = AccessControlList.deserialize ( meta.optJSONObject ( kMeta_AclTag ), null );
	}

	private final JSONObject fMeta;
	private final AccessControlList fAcl;

	private void packAcl ()
	{
		fMeta.put ( kMeta_AclTag, fAcl.asJson () );
	}
}
