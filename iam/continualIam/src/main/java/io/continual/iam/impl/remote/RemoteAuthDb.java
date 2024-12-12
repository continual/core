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
package io.continual.iam.impl.remote;

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

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
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
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.JwtValidator;
import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.jsonHttpClient.JsonOverHttpClientBuilder;
import io.continual.metrics.MetricsCatalog;
import io.continual.util.collections.ShardedExpiringCache;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;

/**
 * This is a minimal implementation of a remote IAM database.
 * 
 * Be sure to explicitly include the continualJsonHttpClient project in your build.
 */
public class RemoteAuthDb implements IamDb<Identity,Group>
{
	public RemoteAuthDb ( JSONObject config ) throws BuildFailure
	{
		fIamClient = new JsonOverHttpClientBuilder()
			.readJsonConfig ( config )
			.build ()
		;
		String baseUrl = config.optString ( "baseUrl", "https://auth.continual.io/" );
		if ( baseUrl.endsWith ( "/" ) )
		{
			baseUrl = baseUrl.substring ( 0, baseUrl.length () - 1 );
		}
		fBaseUrl = baseUrl;

		fAuthUser = config.optString ( "authUser", null );
		fAuthPassword = config.optString ( "authPassword", null );
		if ( fAuthUser == null || fAuthPassword == null )
		{
			throw new BuildFailure ( "Missing authUser or authPassword" );
		}

		fKnownAuths = new ShardedExpiringCache.Builder<String,CacheEntry> ()
			.named ( "RemoteAuths" )
			.cachingFor ( config.optInt ( "cacheTimeSeconds", 15*60 ), TimeUnit.SECONDS )
			.build ()
		;
	}

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
	}

	@Override
	public void close ()
	{
		fIamClient.close ();
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity createUser ( String userId ) throws IamIdentityExists, IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity createAnonymousUser () throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void deleteUser ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void restoreApiKey ( ApiKey key ) throws IamIdentityDoesNotExist, IamBadRequestException, IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void addJwtValidator ( JwtValidator v )
	{
		// ignored
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Map<String, Identity> loadAllUsers () throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public boolean userExists ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity loadUser ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public String createJwtToken ( Identity ii, long duration, TimeUnit durationUnits ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void invalidateJwtToken ( String jwtToken ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity authenticate ( UsernamePasswordCredential upc ) throws IamSvcException
	{
		final String username = upc.getUsername ();
		final String password = upc.getPassword ();

		// do we have a recent auth with the same password?
		final CacheEntry ce = fKnownAuths.read ( username, entry -> entry.usedPassword ( password ) );
		if ( ce != null )
		{
			return ce.getIdentity ();
		}

		// no, ask the IAM service
		try ( final HttpResponse resp = fIamClient.newRequest ()
				.onPath ( makePath ( "/auth/login" ) )
				.post ( new JSONObject ()
					.put ( "username", username )
					.put ( "password", password )
				)
		)
		{
			final int code = resp.getCode ();
			switch ( code )
			{
				case HttpStatusCodes.k200_ok:
				{
					final RemoteIdentity id = new RemoteIdentity ( username );
					final CacheEntry newCe = new CacheEntry ( password, id );
					fKnownAuths.write ( username, newCe );
					return newCe.getIdentity ();
				}

				case HttpStatusCodes.k401_unauthorized:
				{
					final CacheEntry newCe = new CacheEntry ( password, null );
					fKnownAuths.write ( username, newCe );
					return null;
				}

				default:
					throw new IamSvcException ( "Unexpected response from Auth service" );
			}
		}
		catch ( JSONException | HttpServiceException e )
		{
			throw new IamSvcException ( e );
		}
	}

	@Override
	public Identity authenticate ( ApiKeyCredential akc ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Identity authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Group createGroup ( String groupDesc ) throws IamGroupExists, IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Group createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Collection<String> getAllGroups () throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public Group loadGroup ( String id ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public AccessControlList getAclFor ( Resource resource ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public String getUserIdForTag ( String tag ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType ) throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		throw new IamSvcException ( "Not implemented" );
	}

	@Override
	public void onAclUpdate ( AccessControlList accessControlList )
	{
	}

	private final JsonOverHttpClient fIamClient;
	private final String fBaseUrl;

	private final String fAuthUser;
	private final String fAuthPassword;

	private final ShardedExpiringCache<String,CacheEntry> fKnownAuths;

	private String makePath ( String... parts )
	{
		final StringBuilder sb = new StringBuilder ( fBaseUrl );
		for ( String part : parts )
		{
			if ( !part.startsWith ( "/" ) )
			{
				sb.append ( "/" );
			}
			if ( part.endsWith ( "/" ) )
			{
				part = part.substring ( 0, part.length () - 1 );
			}
			sb.append ( part );
		}
		return sb.toString ();
	}

	private class RemoteIdentity implements Identity
	{
		public RemoteIdentity ( String username )
		{
			fUsername = username;
			fBackingData = null;
		}
		
		@Override
		public String getId () { return fUsername; }

		@Override
		public void reload () throws IamSvcException
		{
			try ( final HttpResponse resp = fIamClient.newRequest ()
				.onPath ( makePath ( "users", fUsername ) )
				.asUser ( new HttpUsernamePasswordCredentials ( fAuthUser, fAuthPassword ) )
				.get ()
			)
			{
				if ( HttpStatusCodes.isSuccess ( resp.getCode () ) )
				{
					fBackingData = resp.getBody ();
				}
				else
				{
					// anything else is a problem
					throw new IamSvcException ( "Unexpected response from Auth service while loading user record." );
				}
			}
			catch ( HttpServiceException | BodyFormatException e )
			{
				throw new IamSvcException ( e );
			}
		}

		@Override
		public String getUserData ( String key ) throws IamSvcException
		{
			final JSONObject backingData = getBackingData ();
			if ( backingData != null )
			{
				final JSONObject dataBlock = backingData.optJSONObject ( "data" );
				if ( dataBlock != null )
				{
					return dataBlock.optString ( key );
				}
			}
			return null;
		}

		@Override
		public void putUserData ( String key, String val ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public void removeUserData ( String key ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public Map<String, String> getAllUserData () throws IamSvcException
		{
			final JSONObject backingData = getBackingData ();
			if ( backingData != null )
			{
				final JSONObject dataBlock = backingData.optJSONObject ( "data" );
				if ( dataBlock != null )
				{
					return JsonVisitor.objectToMap ( dataBlock );
				}
			}
			return new HashMap<> ();
		}


		@Override
		public boolean isEnabled () throws IamSvcException
		{
			final JSONObject backingData = getBackingData ();
			if ( backingData != null )
			{
				return backingData.optBoolean ( "enabled", true );
			}
			return true;
		}

		@Override
		public void enable ( boolean enable ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public void setPassword ( String password ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException, IamBadRequestException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public ApiKey createApiKey () throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public Collection<String> loadApiKeysForUser () throws IamSvcException
		{
			final JSONObject backingData = getBackingData ();
			if ( backingData != null )
			{
				return JsonVisitor.arrayToList ( backingData.optJSONArray ( "apiKeys" ) );
			}
			return new LinkedList<> ();
		}

		@Override
		public void deleteApiKey ( ApiKey key ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public Set<String> getGroupIds () throws IamSvcException
		{
			final JSONObject backingData = getBackingData ();
			if ( backingData != null )
			{
				return new TreeSet<> ( JsonVisitor.arrayToList ( backingData.optJSONArray ( "groups" ) ) );
			}
			return new TreeSet<> ();
		}

		@Override
		public Collection<Group> getGroups () throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		@Override
		public Group getGroup ( String groupId ) throws IamSvcException
		{
			throw new IamSvcException ( "Not implemented" );
		}

		private final String fUsername;
		private JSONObject fBackingData = null;

		private JSONObject getBackingData () throws IamSvcException
		{
			if ( fBackingData == null )
			{
				reload ();
			}
			return fBackingData;
		}
	}

	private class CacheEntry
	{
		public CacheEntry ( String password, RemoteIdentity id )
		{
			fPassword = hash ( password );
			fIdentity = id;
		}

		/**
		 * Get the stored identity record, which may be null
		 * @return an identity or null
		 */
		public RemoteIdentity getIdentity () { return fIdentity; }

		/**
		 * Return true if the given password was used
		 * @param password
		 * @return true if the given password was used
		 */
		public boolean usedPassword ( String password )
		{
			return ( fPassword == null && password == null ) || fPassword.equals ( hash ( password ) );
		}

		private final Integer fPassword;
		private final RemoteIdentity fIdentity;

		private final Integer hash ( String val )
		{
			return val == null ? null : val.hashCode ();
		}
	}
}
