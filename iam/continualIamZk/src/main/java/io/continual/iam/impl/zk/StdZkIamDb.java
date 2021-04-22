package io.continual.iam.impl.zk;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.ServiceContainer;

public class StdZkIamDb extends ZkIamDb<CommonJsonIdentity,CommonJsonGroup>
{
	public static class Builder extends ZkIamDb.Builder<CommonJsonIdentity, CommonJsonGroup>
	{
		@Override
		public StdZkIamDb build () throws IamSvcException
		{
			return new StdZkIamDb ( this );
		}
	}

	private StdZkIamDb ( Builder builder ) throws IamSvcException
	{
		super ( builder );
	}

	public static StdZkIamDb fromJson ( ServiceContainer sc, JSONObject config ) throws BuildFailure, IamSvcException
	{
		final StdZkIamDb.Builder b = new StdZkIamDb.Builder ();
		ZkIamDb.populateBuilderFrom ( b, sc, config );
		return b.build ();
	}

	@Override
	protected CommonJsonIdentity instantiateIdentity ( String id, JSONObject data )
	{
		return new CommonJsonIdentity ( id, data, this );
	}

	@Override
	protected CommonJsonGroup instantiateGroup ( String id, JSONObject data )
	{
		return new CommonJsonGroup ( id, data, this );
	}
}
