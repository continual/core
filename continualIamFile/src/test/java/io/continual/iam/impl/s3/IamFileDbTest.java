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
package io.continual.iam.impl.s3;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.file.IamFileDb;
import junit.framework.TestCase;

public class IamFileDbTest extends TestCase
{
	@Test
	public void testS3Setup () throws IamSvcException, IamIdentityExists, IamIdentityDoesNotExist, IamGroupDoesNotExist, IOException
	{
		final IamFileDb db = new IamFileDb.Builder ()
			.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
			.forWrites ()
			.forceInit ()
			.build ()
		;

		final String userId = "johndoe@example.com";

		CommonJsonIdentity i = null;
		if ( !db.userExists ( userId ) )
		{
			i = db.createUser ( userId );
			assertNotNull ( i );
			assertTrue ( db.userExists ( userId ) );
		}
		else
		{
			i = db.loadUser ( userId );
		}

		i.setPassword ( "foobar" );
		i.enable ( true );

		final CommonJsonIdentity j = db.authenticate ( new UsernamePasswordCredential ( userId, "foobar" ) );
		assertNotNull ( j );

		final CommonJsonGroup group = db.createGroup ( "Very Nice Group" );
		db.addUserToGroup ( group.getId(), i.getId() );

		{
			final CommonJsonIdentity k = db.loadUser ( userId );
			final Set<String> groups = k.getGroupIds ();
			assertTrue ( groups.contains ( group.getId () ));
		}
	}

	@Test
	public void testAcl () throws IamSvcException, IamIdentityExists, IamIdentityDoesNotExist, IamGroupDoesNotExist, IOException
	{
		final IamFileDb db = new IamFileDb.Builder ()
			.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
			.forWrites ()
			.forceInit ()
			.build ()
		;

		final Resource res = new Resource ()
		{
			@Override
			public String getId () { return "resId"; }
		};

		final String userId = "johndoe@example.com";
		if ( !db.userExists ( userId ))
		{
			final Identity i = db.createUser ( userId );
			i.setPassword ( "foobar" );
			i.enable ( true );
		}

		final AccessControlList acl = db.getAclFor ( res );
		assertNotNull ( acl );

		acl.clear ();
		acl.permit ( userId, AccessDb.kReadOperation );

		final CommonJsonIdentity j = db.authenticate ( new UsernamePasswordCredential ( userId, "foobar" ) );
		assertNotNull ( j );

		final CommonJsonGroup group = db.createGroup ( "Very Nice Group" );
		db.addUserToGroup ( group.getId(), j.getId() );
		acl.permit ( group.getId (), AccessDb.kWriteOperation );

		assertTrue ( db.canUser ( userId, res, AccessDb.kReadOperation ) );
		assertTrue ( db.canUser ( userId, res, AccessDb.kWriteOperation ) );
		assertFalse ( db.canUser ( userId, res, AccessDb.kCreateOperation ) );
	}
}
