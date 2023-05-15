package io.continual.iam.impl.auth0;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class Auth0IamServiceManager extends SimpleService implements IamServiceManager<Auth0Identity,Auth0Group>
{
	public Auth0IamServiceManager ( ServiceContainer sc, JSONObject config ) throws IamSvcException, BuildFailure
	{
		fDb = Auth0IamDb.fromJson ( sc.getExprEval ().evaluateJsonObject ( config ) );
	}

	@Override
	public IdentityDb<Auth0Identity> getIdentityDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessDb<Auth0Group> getAccessDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public IdentityManager<Auth0Identity> getIdentityManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessManager<Auth0Group> getAccessManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public TagManager getTagManager () throws IamSvcException
	{
		return fDb;
	}

	private final Auth0IamDb fDb;
}
