package io.continual.iam.impl.auth0;
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


import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.json.mgmt.Role;
import com.auth0.json.mgmt.RolesPage;
import com.auth0.json.mgmt.users.User;
import com.auth0.json.mgmt.users.UsersPage;
import com.auth0.net.AuthRequest;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.ProtectedResource;
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
import io.continual.iam.identity.JwtValidator;
import io.continual.metrics.MetricsCatalog;
import io.continual.util.collections.ShardedExpiringCache;
import io.continual.util.collections.ShardedExpiringCache.Fetcher;
import io.continual.util.collections.ShardedExpiringCache.Fetcher.FetchException;

public class Auth0IamDb implements IamDb<Auth0Identity,Auth0Group>
{
	public static Auth0IamDb fromJson ( JSONObject config ) throws IamSvcException, BuildFailure
	{
		return new Auth0IamDb ( config );
	}

	private Auth0IamDb ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fDomain = config.getString ( "domain" );
			fClientId = config.getString ( "clientId" );

			fAuthApi = new AuthAPI (
				fDomain,
				fClientId,
				config.getString ( "clientSecret" )
			);
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}

		fMgmntApi = null;
		fMgmtApiToken = null;

		fUserCache = new ShardedExpiringCache.Builder<String,Auth0Identity> ()
			.named ( "group cache" )
			.cachingFor ( 5, TimeUnit.MINUTES )
			.withShardCount ( 32 )
			.build ()
		;

		fGroupCache = new ShardedExpiringCache.Builder<String,Auth0Group> ()
			.named ( "group cache" )
			.cachingFor ( 5, TimeUnit.MINUTES )
			.withShardCount ( 32 )
			.build ()
		;
	}
	
	@Override
	public boolean userExists ( String userId )throws IamSvcException
	{
		return loadUser ( userId ) != null;
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		return userExists ( userIdOrAlias );
	}

	@Override
	public Auth0Identity loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		return loadUser ( userIdOrAlias );
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		try
		{
			final TreeSet<String> result = new TreeSet<String> ();

			final UsersPage up = getMgmntApi ().users ().list ( null ).execute ();
			for ( User u : up.getItems () )
			{
				result.add ( u.getEmail () );
			}

			return result;
		}
		catch ( Auth0Exception e )
		{
			throw new IamSvcException ( e );
		}
	}

	@Override
	public Map<String, Auth0Identity> loadAllUsers () throws IamSvcException
	{
		final HashMap<String,Auth0Identity> result = new HashMap<> ();
		for ( String email : getAllUsers () )
		{
			result.put ( email, loadUser ( email ) );
		}
		return result;
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		final LinkedList<String> result = new LinkedList<> ();
		for ( String userId : getAllUsers () )
		{
			if ( userId.startsWith ( startingWith ) )
			{
				result.add ( userId );
			}
		}
		return result;
	}

	@Override
	public Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		final Auth0Identity user = loadUser ( userId );
		if ( user != null )
		{
			return user.getGroupIds ();
		}
		throw new IamIdentityDoesNotExist ( userId );
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist
	{
		final TreeSet<String> result = new TreeSet<> ();
		try
		{
			final UsersPage up = getMgmntApi().roles ().listUsers ( groupId, null ).execute ();
			if ( up == null ) throw new IamGroupDoesNotExist ( groupId );	// not sure this happens

			for ( User u : up.getItems () )
			{
				result.add ( u.getEmail () );
			}
		}
		catch ( APIException e )
		{
			throw new IamGroupDoesNotExist ( groupId );
		}
		catch ( Auth0Exception e )
		{
			throw new IamSvcException ( e );
		}
		return result;
	}

	@Override
	public Collection<String> getAllGroups () throws IamSvcException
	{
		try
		{
			final TreeSet<String> result = new TreeSet<String> ();

			final RolesPage up = getMgmntApi ().roles ().list ( null ).execute ();
			for ( Auth0Group g : groupsFromRoles ( up ) )
			{
				result.add ( g.getId () );
			}

			return result;
		}
		catch ( Auth0Exception e )
		{
			throw new IamSvcException ( e );
		}
	}

	@Override
	public Auth0Identity loadUser ( String userId ) throws IamSvcException
	{
		try
		{
			return fUserCache.read ( userId, null, new Fetcher<String,Auth0Identity> ()
			{
				@Override
				public Auth0Identity fetch ( String key ) throws FetchException
				{
					try
					{
						final List<User> users = getMgmntApi ().users ().listByEmail ( userId, null ).execute ();
						if ( users.size () > 0 )
						{
							final User first = users.get ( 0 );
							if ( users.size () > 1 )
							{
								log.warn ( "Ignoring additional records for {}", first.getEmail () );
							}
			
							final RolesPage roles = getMgmntApi().users ().listRoles ( first.getId (), null ).execute ();
							final Set<Auth0Group> groups = groupsFromRoles ( roles );
			
							return new Auth0Identity ( first, groups );
						}
						return null;
					}
					catch ( Auth0Exception | IamSvcException e )
					{
						throw new FetchException ( e );
					}
				}
			} );
		}
		catch ( FetchException x )
		{
			throw new IamSvcException ( x );
		}
	}

	@Override
	public Auth0Group loadGroup ( String id ) throws IamSvcException
	{
		try
		{
			return fGroupCache.read ( id, null, new Fetcher<String,Auth0Group> ()
			{
				@Override
				public Auth0Group fetch ( String key ) throws FetchException
				{
					try
					{
						final Role role = getMgmntApi().roles ().get ( key ).execute ();
						return new Auth0Group ( Auth0IamDb.this, role );
					}
					catch ( Auth0Exception | IamSvcException e )
					{
						throw new FetchException ( e );
					}
				}
			} );
		}
		catch ( FetchException e )
		{
			throw new IamSvcException ( e );
		}
	}

	@Override
	public AccessControlList getAclFor ( Resource resource )
	{
		if ( resource instanceof ProtectedResource )
		{
			return ((ProtectedResource) resource ).getAccessControlList ();
		}
		return null;
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		final AccessControlList acl = getAclFor ( resource );
		return acl == null ? false : acl.canUser ( loadUser ( id ), operation );
	}

	@Override
	public Auth0Identity createUser ( String userId ) throws IamIdentityExists, IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public Auth0Identity createAnonymousUser () throws IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public void deleteUser ( String userId ) throws IamSvcException
	{
		readOnlyDbException ();
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException
	{
		readOnlyDbException ();
	}

	@Override
	public void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException
	{
		readOnlyDbException ();
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		// no aliases here
		return new LinkedList<> ();
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword ) throws IamSvcException
	{
		return false;
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException
	{
		return null;
	}

	@Override
	public void restoreApiKey ( ApiKey key ) throws IamIdentityDoesNotExist, IamBadRequestException, IamSvcException
	{
		readOnlyDbException ();
	}

	@Override
	public void addJwtValidator ( JwtValidator v )
	{
		log.warn ( "Ignoring added JWT validator in Auth0Db" );
	}

	@Override
	public Auth0Identity authenticate ( UsernamePasswordCredential upc )
	{
		return null;
	}

	@Override
	public Auth0Identity authenticate ( ApiKeyCredential akc )
	{
		return null;
	}

	@Override
	public Auth0Identity authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		return null;
	}

	@Override
	public String createJwtToken ( Identity ii, long duration, TimeUnit tu ) throws IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public void invalidateJwtToken ( String jwtToken )
	{
	}

	@Override
	public Auth0Group createGroup ( String groupDesc ) throws IamGroupExists, IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public Auth0Group createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public void addUserToGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		readOnlyDbException ();
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		readOnlyDbException ();
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		readOnlyDbException ();
		return null;
	}

	@Override
	public String getUserIdForTag ( String tag )
	{
		return null;
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType )
	{
	}

	@Override
	public void sweepExpiredTags ()
	{
	}

	@Override
	public void onAclUpdate ( AccessControlList accessControlList )
	{
		// ignore
	}

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
	}

	private final String fDomain;
	private final String fClientId;
	private final AuthAPI fAuthApi;
	private ManagementAPI fMgmntApi;
	private JwtCredential fMgmtApiToken;

	private final ShardedExpiringCache<String, Auth0Identity> fUserCache;
	private final ShardedExpiringCache<String, Auth0Group> fGroupCache;

	private static final Logger log = LoggerFactory.getLogger ( Auth0IamDb.class );

	private JSONObject readOnlyDbException () throws IamSvcException
	{
		throw new IamSvcException ( "Auth0 db is read-only" );
	}

	private ManagementAPI getMgmntApi () throws IamSvcException
	{
		if ( fMgmntApi != null && !fMgmtApiToken.isExpired () )
		{
			return fMgmntApi;
		}

		try
		{
			final AuthRequest authRequest = fAuthApi.requestToken ( "https://" + fDomain + "/api/v2/" );
			final TokenHolder holder = authRequest.execute ();
			final String accessToken = holder.getAccessToken ();

			fMgmtApiToken = new JwtCredential ( accessToken );
			fMgmntApi = new ManagementAPI ( fDomain, accessToken );

			return fMgmntApi;
		}
		catch ( Auth0Exception | JwtCredential.InvalidJwtToken x )
		{
			throw new IamSvcException ( x );
		}
	}

	private Set<Auth0Group> groupsFromRoles ( RolesPage roles )
	{
		final TreeSet<Auth0Group> result = new TreeSet<> ();

		for ( Role r : roles.getItems () )
		{
			Auth0Group group = fGroupCache.read ( r.getId () );
			if ( group == null )
			{
				group = new Auth0Group ( this, r );
				fGroupCache.write ( r.getId (), group );
			}
			result.add ( group );
		}

		return result;
	}
}
