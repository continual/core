package io.continual.iam.impl.jsondoc;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.tags.TagManager;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * This service is provided for test. It loads "db" from the configuration as a simple, read-only
 * IAM database instance.
 */
public class SimpleDocDbSvc extends SimpleService implements IamServiceManager<CommonJsonIdentity,CommonJsonGroup>
{
	public SimpleDocDbSvc ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fDbJson = config.optJSONObject ( "db" );
		if ( fDbJson == null ) throw new BuildFailure ( "No IAM db provided." );

		fDb = new JsonDocDb ( fDbJson );
	}

	@Override
	public IdentityDb<CommonJsonIdentity> getIdentityDb () { return fDb; }

	@Override
	public AccessDb<CommonJsonGroup> getAccessDb () { return fDb; }

	@Override
	public IdentityManager<CommonJsonIdentity> getIdentityManager () { return fDb; }

	@Override
	public AccessManager<CommonJsonGroup> getAccessManager () { return fDb; }

	@Override
	public TagManager getTagManager () { return fDb; }

	private final JSONObject fDbJson;
	private final JsonDocDb fDb;
}
