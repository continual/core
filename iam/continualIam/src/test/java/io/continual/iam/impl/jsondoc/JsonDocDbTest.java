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
package io.continual.iam.impl.jsondoc;

import java.io.IOException;

import org.junit.Test;

import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class JsonDocDbTest extends TestCase
{
	@Test
	public void testApiKeyCreateOnNullUser () throws IamIdentityDoesNotExist, IamSvcException, IOException
	{
		try ( final JsonDocDb db = new JsonDocDb () )
		{
			
			db.createApiKey ( null );
			fail ( "can't create api key on null user" );
		}
		catch ( IamBadRequestException x )
		{
			// good
		}
	}

	@Test
	public void testApiKeyCreate () throws IamSvcException, IamBadRequestException, IOException
	{
		try ( final JsonDocDb db = new JsonDocDb () )
		{
			final CommonJsonIdentity i = db.createUser ( "test" );
			assertNotNull ( i );
	
			final ApiKey key = db.createApiKey ( i.getId () );
	
			assertNotNull ( key );
			assertNotNull ( key.getKey () );
			assertNotNull ( key.getSecret () );
		}
	}

	@Test
	public void testPasswordReset () throws IamSvcException, IamBadRequestException, IOException
	{
		final Clock.TestClock tc = Clock.useNewTestClock ();

		try ( final JsonDocDb db = new JsonDocDb () )
		{
			final CommonJsonIdentity i = db.createUser ( "test" );
			assertNotNull ( i );
	
			tc.add ( 100 );
	
			String resetTag = i.requestPasswordReset ( 10, "nonce" );
			assertTrue ( db.completePasswordReset ( resetTag, "foobar" ) );
	
			assertNotNull ( db.authenticate ( new UsernamePasswordCredential ( "test", "foobar" ) ) );
	
			resetTag = i.requestPasswordReset ( 10, "nonce" );
			tc.add ( 50000 );
			assertFalse ( db.completePasswordReset ( resetTag, "foobar" ) );
		}
	}

	@Test
	public void testSerialize ()
	{
		try (JsonDocDb jdd = new JsonDocDb ()) {
			assertNotNull ( jdd.serialize() );
		}
	}
}
