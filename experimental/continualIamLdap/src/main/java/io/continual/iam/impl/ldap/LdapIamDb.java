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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessManager;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;

public class LdapIamDb implements IdentityManager<LdapIdentity>, AccessManager<LdapGroup>, TagManager, AclUpdateListener, IamDb<LdapIdentity,LdapGroup>
{
	public static class Builder 
	{
		public LdapIamDb build ()
		{
			return new LdapIamDb ( this );
		}

		private String fHostname = "localhost";
		private int fPort = 389;
		private String fAdminUsername = null;
		private String fAdminPassword = null;
		public long fConnectionTimeoutMs = 30000L;
	}
	
	private LdapIamDb ( Builder b )
	{
		final LdapConnectionConfig config = new LdapConnectionConfig ();
		config.setLdapHost ( b.fHostname );
		config.setLdapPort ( b.fPort );
		if ( b.fAdminUsername != null )
		{
			config.setName ( b.fAdminUsername );
			config.setCredentials ( b.fAdminPassword );
		}

		final DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory ( config );
		factory.setTimeOut ( b.fConnectionTimeoutMs );

		// FIXME: deliver builder settings thru here
		final GenericObjectPool.Config poolConfig = new GenericObjectPool.Config ();
		poolConfig.lifo = true;
		poolConfig.maxActive = 8;
		poolConfig.maxIdle = 8;
		poolConfig.maxWait = -1L;
		poolConfig.minEvictableIdleTimeMillis = 1000L * 60L * 30L;
		poolConfig.minIdle = 0;
		poolConfig.numTestsPerEvictionRun = 3;
		poolConfig.softMinEvictableIdleTimeMillis = -1L;
		poolConfig.testOnBorrow = false;
		poolConfig.testOnReturn = false;
		poolConfig.testWhileIdle = false;
		poolConfig.timeBetweenEvictionRunsMillis = -1L;
		poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

		fLdapDb = new LdapConnectionPool( new DefaultPoolableLdapConnectionFactory( factory ), poolConfig );
		fOuPart = ",ou=system";
	}

	@Override
	public void close () throws IOException
	{
		try
		{
			fLdapDb.close ();
		}
		catch ( Exception e )
		{
			throw new IOException ( e );
		}
	}

	@Override
	public LdapIdentity authenticate ( UsernamePasswordCredential upc ) throws IamSvcException
	{
		try
		{
			final LdapConnection conn = fLdapDb.borrowObject ();
			try
			{
				conn.bind ( makeDn ( upc.getUsername () ), upc.getPassword () );
				if ( conn.isAuthenticated () )
				{
					return new LdapIdentity ( upc.getUsername () );
				}
			}
			finally
			{
				fLdapDb.releaseConnection ( conn );
			}
			return null;
		}
		catch ( LdapException e )
		{
			throw new IamSvcException ( e );
		}
		catch ( Exception e )	// borrowObject throws plain Exception
		{
			throw new IamSvcException ( e );
		}
	}

	@Override
	public LdapIdentity authenticate ( ApiKeyCredential akc ) throws IamSvcException
	{
		throw new IamSvcException ( "LDAP identity management doesn't support API keys at this time." );
	}

	@Override
	public LdapIdentity authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		throw new IamSvcException ( "LDAP identity management doesn't support JWTs at this time." );
	}

	@Override
	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		throw new IamSvcException ( "LDAP identity management doesn't support JWTs at this time." );
	}

	@Override
	public void invalidateJwtToken ( String jwtToken ) throws IamSvcException
	{
	}

	@Override
	public LdapGroup loadGroup ( String id ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccessControlList getAclFor ( Resource resource ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onAclUpdate ( AccessControlList accessControlList )
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserIdForTag ( String tag )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sweepExpiredTags ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public LdapGroup createGroup ( String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LdapGroup createGroup ( String groupId, String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addUserToGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getUsersGroups ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId )
		throws IamSvcException,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean userExists ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public LdapIdentity loadUser ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LdapIdentity loadUserOrAlias ( String userIdOrAlias )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findUsers ( String startingWith )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LdapIdentity createUser ( String userId )
		throws IamIdentityExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LdapIdentity createAnonymousUser ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUser ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAlias ( String userId, String alias )
		throws IamSvcException,
			IamBadRequestException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAlias ( String alias )
		throws IamBadRequestException,
			IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<String> getAliasesFor ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getAllUsers ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, LdapIdentity> loadAllUsers ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final LdapConnectionPool fLdapDb;
	private final String fOuPart;
	
	private static final Logger log = LoggerFactory.getLogger ( LdapIamDb.class );

	private String makeDn ( String username )
	{
		// FIXME: escaping etc. See https://docs.microsoft.com/en-us/previous-versions/windows/desktop/ldap/distinguished-names
		return "uid=" + username + fOuPart;
	}
}
