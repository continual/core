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

import java.util.Set;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import io.continual.iam.tags.TagManager;

public class BasicTest extends TestCase
{
	@Test
	public void testSimpleStructure () throws IamSvcException
	{
		final IamServiceManager<?,?> db = makeSimpleDb ();
		assertNull ( db.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( "nuthin", "nobody" ) ) );

		final Identity user = db.getIdentityManager ().loadUser ( "johndoe" );
		assertNotNull ( user );
		
		final Set<String> groups = user.getGroupIds ();
		assertTrue ( groups.contains ( "myGroup" ));

		// no acl = default to no access
		assertFalse ( db.getAccessDb ().canUser ( "johndoe", new Resource(){
			@Override
			public String getId ()
			{
				return "123";
			}}, AccessDb.kReadOperation ) );

		// non-owner acl with granted access
		assertTrue ( db.getAccessDb ().canUser ( "johndoe", new Resource(){
			@Override
			public String getId ()
			{
				return "someObjectId";
			}}, AccessDb.kReadOperation ) );
	}

	@SuppressWarnings("rawtypes")
	public static IamServiceManager<?,?> makeSimpleDb ()
	{
		final JsonDocDb db = new JsonDocDb (
			new JSONObject ()
				.put ( "users",
					new JSONObject ()
						.put ( "johndoe",
							new JSONObject ()
								.put ( "enabled", true )
								.put ( "groups",
									new JSONArray ()
										.put ( "myGroup" )
								)
						)
					)
				.put ( "groups",
					new JSONObject ()
						.put ( "myGroup",
							new JSONObject ()
								.put ( "name", "some fine group" )
						)
				)
				.put ( "acls",
					new JSONObject ()
						.put ( "someObjectId",
							new JSONObject ()
								.put ( "owner", "jones" )
								.put ( "entries",
									new JSONArray ()
										.put (
											new JSONObject ()
												.put ( "who", AccessControlEntry.kAnyUser )
												.put ( "access", AccessControlEntry.Access.PERMIT )
												.put ( "operations", new JSONArray (). put ( AccessDb.kReadOperation ) )
										)
							)
						)
				)
			);

		return new IamServiceManager ()
		{
			@Override
			public JsonDocDb getIdentityDb () { return db; }

			@Override
			public JsonDocDb getAccessDb () { return db; }

			@Override
			public IdentityManager getIdentityManager () { return db; }

			@Override
			public AccessManager getAccessManager () { return db; }

			@Override
			public TagManager getTagManager () throws IamSvcException { return db; }
		};
	}
}
