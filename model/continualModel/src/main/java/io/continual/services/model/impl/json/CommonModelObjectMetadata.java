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
	public static final String kMeta_LockedTypes = "types";

	public CommonModelObjectMetadata ()
	{
		fMeta = new JSONObject ()
			.put ( kMeta_CreateTs, Clock.now () )
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
	public long getLastUpdateTimeMs ()
	{
		return fMeta.optLong ( kMeta_UpdateTs, -1L );
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

	private JSONObject fMeta;
	private AccessControlList fAcl;

	private void packAcl ()
	{
		fMeta.put ( kMeta_AclTag, fAcl.asJson () );
	}
}
