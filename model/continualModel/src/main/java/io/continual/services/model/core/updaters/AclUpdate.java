package io.continual.services.model.core.updaters;

import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelUpdater;

public class AclUpdate implements ModelUpdater
{
	public AclUpdate ( AccessControlList acl )
	{
		this ( acl, false );
	}

	public AclUpdate ( AccessControlList acl, boolean force )
	{
		fAcl = acl;
		fForce = force;
	}

	@Override
	public ModelOperation[] getAccessRequired ()
	{
		return fForce ? new ModelOperation[] {} : new ModelOperation[] { ModelOperation.ACL_UPDATE };
	}

	@Override
	public void update ( ModelRequestContext context, ModelObject o )
	{
		if ( fAcl != null )
		{
			final AccessControlList targetAcl = o.getAccessControlList ();
	
			targetAcl.clear ();
			targetAcl.setOwner ( fAcl.getOwner () );
			for ( AccessControlEntry e : fAcl.getEntries () )
			{
				targetAcl.addAclEntry ( e );
			}
		}
	}

	private final AccessControlList fAcl;
	private final boolean fForce;
}
