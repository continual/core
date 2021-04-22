package io.continual.iam.impl;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsSupplier;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * Creates a service object to wrap an IAM manager implementation
 *
 * @param <I>
 * @param <G>
 */
public class BasicIamServiceWrapper<I extends Identity, G extends Group> extends SimpleService implements IamServiceManager<I,G>, Service, MetricsSupplier
{
	@SuppressWarnings("unchecked")
	public BasicIamServiceWrapper ( ServiceContainer sc, JSONObject config ) throws IamSvcException, BuildFailure
	{
		// build the IAM DB we're hosting
		fDb = Builder.withBaseClass ( IamDb.class )
			.withClassNameInData ()
			.providingContext ( sc )
			.usingData ( config.getJSONObject ( "db" ) )
			.build ()
		;
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		try
		{
			fDb.start ();
		}
		catch ( IamSvcException e )
		{
			throw new FailedToStart ( e );
		}
	}
	
	@Override
	protected void onStopRequested ()
	{
		fDb.close ();
	}

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
		fDb.populateMetrics ( metrics );
	}

	@Override
	public IdentityDb<I> getIdentityDb () throws IamSvcException { return fDb; }

	@Override
	public AccessDb<G> getAccessDb () throws IamSvcException { return fDb; }

	@Override
	public IdentityManager<I> getIdentityManager () throws IamSvcException { return fDb; }

	@Override
	public AccessManager<G> getAccessManager () throws IamSvcException { return fDb; }

	@Override
	public TagManager getTagManager () throws IamSvcException { return fDb; }

	private final IamDb<I,G> fDb;
}
