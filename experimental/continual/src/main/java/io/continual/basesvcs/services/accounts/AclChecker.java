package io.continual.basesvcs.services.accounts;

import io.continual.iam.access.AccessControlList;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;

public class AclChecker
{
	public AclChecker ()
	{
		fUser = null;
		fOp = null;
		fAcl = null;
		fResource = "";
	}

	public AclChecker forUser ( Identity user )
	{
		fUser = user;
		return this;
	}

	public AclChecker reading ()
	{
		return performing ( Operation.READ );
	}

	public AclChecker updating ()
	{
		return performing ( Operation.UPDATE );
	}

	public AclChecker creating ()
	{
		return performing ( Operation.CREATE );
	}

	public AclChecker deleting ()
	{
		return performing ( Operation.DELETE );
	}

	public AclChecker performing ( Operation op )
	{
		fOp = op;
		return this;
	}

	public AclChecker onResource ( String resource )
	{
		fResource = resource;
		return this;
	}

	public AclChecker controlledByAcl ( AccessControlList acl )
	{
		fAcl = acl;
		return this;
	}

	public void check ()  throws AccessException, IamSvcException
	{
		if ( fUser == null ) throw new AccessException ( "No user provided." );
		if ( fAcl == null ) throw new AccessException ( "No ACL provided." );
		if ( fOp == null ) throw new AccessException ( "No operation provided." );

		if ( !fAcl.canUser ( fUser, fOp.toString () ) )
		{
			throw new AccessException ( fUser.getId () + " may not " + fOp.toString ().toLowerCase () + " " + fResource );
		}
	}

	private AccessControlList fAcl;
	private Identity fUser;
	private Operation fOp;
	private String fResource;
}
