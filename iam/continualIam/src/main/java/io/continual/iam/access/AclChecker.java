/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam.access;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;

/**
 * An ACL checker for convenience
 */
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
		return performing ( AccessControlList.READ );
	}

	public AclChecker updating ()
	{
		return performing ( AccessControlList.UPDATE );
	}

	public AclChecker creating ()
	{
		return performing ( AccessControlList.CREATE );
	}

	public AclChecker deleting ()
	{
		return performing ( AccessControlList.DELETE );
	}

	public AclChecker performing ( String op )
	{
		fOp = op;
		return this;
	}

	/**
	 * Label the resource for use in an access exception message.
	 * @param resource a resource label
	 * @return this checker
	 */
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

	/**
	 * Check the user's access. If not allowed, AccessException is thrown. 
	 * @throws AccessException thrown if user is not permitted
	 * @throws IamSvcException thrown on a processing error in the IAM subsystem
	 */
	public void check () throws AccessException, IamSvcException
	{
		final String userId = fUser == null ? "anonymous" : fUser.getId ();

		if ( fAcl == null ) throw new AccessException ( "No ACL provided." );
		if ( fOp == null ) throw new AccessException ( "No operation provided." );

		if ( !fAcl.canUser ( fUser, fOp ) )
		{
			throw new AccessException ( userId + " may not " + fOp.toLowerCase () + " " + fResource );
		}
	}

	private AccessControlList fAcl;
	private Identity fUser;
	private String fOp;
	private String fResource;
}
