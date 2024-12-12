package io.continual.iam.impl.remote;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * This service wraps a connection to a remote IAM database.
 */
public class RemoteDbSvc extends SimpleService implements IamServiceManager<Identity,Group>
{
	public RemoteDbSvc ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fDb = new RemoteAuthDb ( sc.getExprEval ().evaluateJsonObject ( config ) );
	}

	@Override
	public IdentityDb<Identity> getIdentityDb () { return fDb; }

	@Override
	public AccessDb<Group> getAccessDb () { return fDb; }

	@Override
	public IdentityManager<Identity> getIdentityManager () { return fDb; }

	@Override
	public AccessManager<Group> getAccessManager () { return fDb; }

	@Override
	public TagManager getTagManager () { return fDb; }

	private final RemoteAuthDb fDb;
}
