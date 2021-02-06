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
package io.continual.iam.app.iam;

import org.junit.Test;

import io.continual.iam.IamServiceManager;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;

import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class IamTest extends TestCase
{
	@Test
	public void testSignonWithPasswordOk () throws IamSvcException, IamIdentityDoesNotExist, IamIdentityExists, IamGroupExists
	{
		final IamServiceManager<?,?> am = BasicTest.makeSimpleDb ();

		final Group group = am.getAccessManager().createGroup ( "a1" );
		assertEquals ( "a1", group.getName () );

		final IdentityManager<?> im = am.getIdentityManager ();
		final Identity newUser = im.createUser ( "u1@example.com" );

		newUser.setPassword ( "pwd" );
		newUser.enable ( true );

		assertNotNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "pwd" ) ) );
		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "wrongpwd" ) ) );

		newUser.enable ( false );
		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "pwd" ) ) );
	}

	@Test
	public void testSignonWithBadPassword () throws IamSvcException, IamIdentityDoesNotExist, IamIdentityExists, IamGroupExists
	{
		final IamServiceManager<?,?> am = BasicTest.makeSimpleDb ();

		final Group group = am.getAccessManager().createGroup ( "a1" );
		assertEquals ( "a1", group.getName () );

		final IdentityManager<?> im = am.getIdentityManager ();
		final Identity newUser = im.createUser ( "u1@example.com" );

		newUser.setPassword ( "pwd" );
		newUser.enable ( true );

		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "wrongpwd" ) ) );
	}

	@Test
	public void testSignonWithNoPasswordSet () throws IamIdentityDoesNotExist, IamSvcException, IamIdentityExists, IamGroupExists 
	{
		final IamServiceManager<?,?> am = BasicTest.makeSimpleDb ();

		final Group group = am.getAccessManager().createGroup ( "a1" );
		assertEquals ( "a1", group.getName () );

		final IdentityManager<?> im = am.getIdentityManager ();
		final Identity newUser = im.createUser ( "u1@example.com" );
		newUser.enable ( true );
		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "wrongpwd" ) ) );
	}

	@Test
	public void testSignonWithUserDisabled () throws IamSvcException, IamIdentityDoesNotExist, IamIdentityExists, IamGroupExists 
	{
		final IamServiceManager<?,?> am = BasicTest.makeSimpleDb ();

		final Group group = am.getAccessManager().createGroup ( "a1" );
		assertEquals ( "a1", group.getName () );

		final IdentityManager<?> im = am.getIdentityManager ();
		final Identity newUser = im.createUser ( "u1@example.com" );

		newUser.setPassword ( "pwd" );
		newUser.enable ( false );

		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "pwd" ) ) );
	}

	@Test
	public void testPasswordReset () throws IamSvcException, IamBadRequestException
	{
		final IamServiceManager<?,?> am = BasicTest.makeSimpleDb ();

		final Group group = am.getAccessManager().createGroup ( "a1" );
		assertEquals ( "a1", group.getName () );

		final IdentityManager<?> im = am.getIdentityManager ();
		final Identity newUser = im.createUser ( "u1@example.com" );
		newUser.setPassword ( "pwd" );
		newUser.enable ( true );

		final String token = newUser.requestPasswordReset ( 60 * 60 * 24, "" + Clock.now () );

		// password should not change yet
		assertNotNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "pwd" ) ) );

		im.completePasswordReset ( token, "new" );

		// old password fails
		assertNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "pwd" ) ) );

		// new password works
		assertNotNull ( im.authenticate ( new UsernamePasswordCredential ( "u1@example.com", "new" ) ) );
	}
}
