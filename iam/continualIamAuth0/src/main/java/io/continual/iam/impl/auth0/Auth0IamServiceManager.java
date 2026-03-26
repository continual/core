package io.continual.iam.impl.auth0;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class Auth0IamServiceManager extends SimpleService implements IamServiceManager<Auth0Identity,Auth0Group>
{
	public Auth0IamServiceManager ( ServiceContainer sc, JSONObject config ) throws IamSvcException, BuildFailure
	{
		fDb = Auth0IamDb.fromJson ( sc.getExprEval ().evaluateJsonObject ( config ) );
	}

	@Override
	public Auth0IamDb getIdentityDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public Auth0IamDb getAccessDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public Auth0IamDb getIdentityManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public Auth0IamDb getAccessManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public Auth0IamDb getTagManager () throws IamSvcException
	{
		return fDb;
	}

	private final Auth0IamDb fDb;
}
