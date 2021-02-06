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
package io.continual.iam.impl.ldap;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class LdapIamServiceManager extends SimpleService implements IamServiceManager<LdapIdentity,LdapGroup>, Service
{
	public LdapIamServiceManager ( ServiceContainer sc, JSONObject settings ) throws IamSvcException, BuildFailure
	{
		fDb = new LdapIamDb.Builder ()
			.build ()
		;
	}

	@Override
	public IdentityDb<LdapIdentity> getIdentityDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessDb<LdapGroup> getAccessDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public IdentityManager<LdapIdentity> getIdentityManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessManager<LdapGroup> getAccessManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public TagManager getTagManager () throws IamSvcException
	{
		return fDb;
	}

	private final LdapIamDb fDb;
}
