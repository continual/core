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
package io.continual.iam.impl.common;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
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
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.metrics.MetricsCatalog;
import io.continual.util.data.OneWayHasher;
import io.continual.util.data.Sha1HmacSigner;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.time.Clock;

/**
 *	CommonJsonDb manages identity related objects that are serialized in JSON 
 * 
 * @param <I> an identity class
 * @param <G> a group class
 */
public abstract class CommonJsonDb<I extends CommonJsonIdentity,G extends CommonJsonGroup> implements IamDb<I,G>
{
	public static final String kTagId = "tagId";
	public static final String kUserId = "userId";
	public static final String kTagType = "tagType";
	public static final String kExpireEpoch = "expireEpoch";
	public static final String kSecret = "secret";
	public static final String kAlias = "alias";
	public static final String kCreateTsMs = "createMs";

	public static final String kEnabled = "enabled";

	public static final String kPasswordBlock = "password";
	public static final String kPasswordSalt = "salt";
	public static final String kPasswordHash = "hash";

	public static final String kTagType_PasswordReset = "passwordReset";

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
	}

	@Override
	public boolean userExists ( String userId ) throws IamSvcException
	{
		return null != loadUser ( userId );
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		return
			userExists ( userIdOrAlias ) ||
			aliasExists ( userIdOrAlias )
		;
	}

	protected boolean aliasExists ( String userId ) throws IamSvcException
	{
		return null != loadAliasObject ( userId );
	}

	@Override
	public I loadUser ( String userId ) throws IamSvcException
	{
		final JSONObject user = loadUserObject ( userId );
		if ( user != null )
		{
			return instantiateIdentity ( userId, user );
		}
		return null;
	}

	@Override
	public I loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		I user = loadUser ( userIdOrAlias );
		if ( user != null ) return user;

		final JSONObject alias = loadAliasObject ( userIdOrAlias );
		if ( alias != null )
		{
			final String userId = alias.getString ( kUserId );
			if ( userId != null )
			{
				return loadUser ( userId );
			}
			else
			{
				log.warn ( "Alias [" + userIdOrAlias + "] record exists but doesn't contain " + kUserId + "." );
			}
		}

		return null;
	}

	@Override
	public I createUser ( String userId ) throws IamSvcException, IamIdentityExists
	{
		if ( userExists ( userId ) )
		{
			throw new IamIdentityExists ( userId );
		}

		storeUserObject (
			userId,
			createNewUser ( userId )
		);

		return loadUser ( userId );
	}

	@Override
	public I createAnonymousUser () throws IamSvcException
	{
		try
		{
			final String anonId = UniqueStringGenerator.create ( "continual iam json db" );
			return createUser ( anonId );
		}
		catch ( IamIdentityExists e )
		{
			throw new IamSvcException ( "anonymous user exists... " + e.getMessage () );
		}
	}

	@Override
	public void deleteUser ( String userId ) throws IamSvcException
	{
		deleteUserObject ( userId );
	}

	@Override
	public boolean completePasswordReset ( String tagId, String newPassword ) throws IamSvcException
	{
		final String userId = getUserIdForTag ( tagId );
		if ( userId != null )
		{
			final I i = loadUser ( userId );
			if ( i != null )
			{
				i.setPassword ( newPassword );
				deleteTagObject ( tagId, userId, kTagType_PasswordReset );
				return true;
			}
			else
			{
				authLog ( "Ignoring password reset completion on tag " + tagId + " for user [" + userId + "], who does not exist." );
			}
		}
		return false;
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException
	{
		final JSONObject record = loadApiKeyObject ( apiKey );
		if ( record != null )
		{
			return instantiateApiKey ( apiKey, record );
		}
		return null;
	}

	/**
	 * Restore an API key into the API key store
	 * @param key
	 * @throws IamSvcException
	 * @throws IamBadRequestException 
	 * @throws IamIdentityDoesNotExist 
	 */
	@Override
	public void restoreApiKey ( ApiKey key ) throws IamIdentityDoesNotExist, IamBadRequestException, IamSvcException
	{
		final JSONObject record = loadApiKeyObject ( key.getKey () );
		if ( record != null )
		{
			throw new IamBadRequestException ( "The API key already exists." );
		}

		final JSONObject o = createApiKeyObject ( key.getUserId (), key.getKey (), key.getSecret () );
		storeApiKeyObject ( key.getKey (), o );
	}

	@Override
	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		if ( fJwtTokenFactory == null )
		{
			throw new IamSvcException ( "This identity manager does not have a JWT token factory." );
		}
		return fJwtTokenFactory.createJwtToken ( ii );
	}

	@Override
	public I authenticate ( ApiKeyCredential akc ) throws IamSvcException
	{
		final String apiKey = akc.getApiKey ();
		
		final ApiKey key = loadApiKeyRecord ( apiKey );
		if ( key != null )
		{
			// use private key to sign content
			final String expectedSignature = Sha1HmacSigner.sign ( akc.getContent (), key.getSecret () );
			authLog ( "expecting [" + expectedSignature + "]; received [" + akc.getSignature () + "]. signed content [" + akc.getContent () + "]." );

			// compare
			if ( expectedSignature.equals ( akc.getSignature () ) )
			{
				authLog ( key.getUserId () + " authenticated via API key " + apiKey );
				final I result = loadUser ( key.getUserId () );
				result.setApiKeyUsedForAuth ( apiKey );
				return result;
			}
		}

		authLog ( akc.getApiKey () + " authentication failed" );
		return null;
	}
	
	@Override
	public I authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		// check if this token has been marked invalid (e.g. user explicitly logged out)
//		if ( isInvalidJwtToken ( jwt.toBearerString () ) ) return null;
// FIXME: we need to hash the string or something -- it's too long for AWS as an s3 key
		
		for ( JwtValidator v : fJwtValidators )
		{
			if ( v.validate ( jwt ) )
			{
				return loadUser ( jwt.getSubject () );
			}
		}

		return null;
	}

	public void invalidateJwtToken ( String token ) throws IamSvcException
	{
		storeInvalidJwtToken ( token );
	}

	@Override
	public I authenticate ( UsernamePasswordCredential upc ) throws IamSvcException
	{
		final I user = loadUserOrAlias ( upc.getUsername () );
		if ( user == null )
		{
			authLog ( "No such user " + upc.getUsername () );
			return null;
		}

		if ( !user.isEnabled () )
		{
			authLog ( "User " + upc.getUsername () + " is disabled." );
			return null;
		}

		final String attemptedPassword = upc.getPassword ();
		if ( attemptedPassword == null )
		{
			authLog ( "User " + upc.getUsername () + " auth attempt without password." );
			return null;
		}

		final String salt = user.getPasswordSalt ();
		if ( salt == null || salt.length () == 0 )
		{
			authLog ( "User " + upc.getUsername () + " does not have a password." );
			return null;
		}

		final String hashedPassword = OneWayHasher.pbkdf2HashToString ( attemptedPassword, salt );

		final String hash = user.getPasswordHash ();
		if ( hash == null || !hash.equals ( hashedPassword ) )
		{
			authLog ( "Password for " + upc.getUsername () + " doesn't match." );
			return null;
		}

		return user;
	}

	@Override
	public G createGroup ( String groupDesc ) throws IamSvcException
	{
		final String groupId = UUID.randomUUID ().toString ();
		try
		{
			return createGroup ( groupId, groupDesc );
		}
		catch ( IamGroupExists e )
		{
			log.warn ( "UUID created randomly conflicted with an exist group name." );
			return loadGroup ( groupId );
		}
	}

	@Override
	public G createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException
	{
		final G group = loadGroup ( groupId );
		if ( group != null ) throw new IamGroupExists ( groupId );

		final JSONObject o = createNewGroup ( groupId, groupDesc );
		storeGroupObject ( groupId, o );

		return loadGroup ( groupId );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId ) throws IamIdentityDoesNotExist, IamSvcException, IamGroupDoesNotExist
	{
		final I user = loadUserOrAlias ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		final G group = loadGroup ( groupId );
		if ( group == null ) throw new IamGroupDoesNotExist ( groupId );

		group.addUser ( userId ); 	// this stores if there's a change

		user.addGroup ( groupId );
		storeUserObject ( userId, user.asJson () );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		final I user = loadUserOrAlias ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( "User does not exist: " + userId );

		final G group = loadGroup ( groupId );
		if ( group == null ) throw new IamGroupDoesNotExist ( "Group does not exist: " + groupId );

		group.removeUser ( userId );	// this stores if there's a change

		user.removeGroup ( groupId );
		storeUserObject ( userId, user.asJson () );
	}

	@Override
	public Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		final I user = loadUserOrAlias ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		return user.getGroupIds ();
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamGroupDoesNotExist, IamSvcException
	{
		final G g = loadGroup ( groupId );
		if ( g == null ) throw new IamGroupDoesNotExist ( groupId + " does not exist" );
		return g.getMembers ();
	}

	@Override
	public G loadGroup ( String groupId ) throws IamSvcException
	{
		final JSONObject group = loadGroupObject ( groupId );
		if ( group != null )
		{
			return instantiateGroup ( groupId, group );
		}
		return null;
	}

	@Override
	public AccessControlList getAclFor ( Resource resource ) throws IamSvcException
	{
		if ( resource instanceof ProtectedResource )
		{
			return ((ProtectedResource)resource).getAccessControlList ();
		}
		else
		{
			final String resId = resource.getId ();
			final AclUpdateListener acll = 
				new AclUpdateListener ()
				{
					@Override
					public void onAclUpdate ( AccessControlList acl )
					{
						try
						{
							storeAclObject ( resId, acl.asJson () );
						}
						catch ( IamSvcException e )
						{
							// FIXME: this exception should propagate upward
							log.warn ( "Couldn't store ACL: " + e.getMessage () );
						}
					}
				};

			final JSONObject o = loadAclObject ( resource.getId () );
			if ( o == null )
			{
				if ( fAclFactory == null )
				{
					log.warn ( "No ACL factory established; returning null from getAclFor ( Resource res )" );
					return null;
				}
				else
				{
					return fAclFactory.createDefaultAcl ( acll );
				}
			}
			else
			{
				return AccessControlList.deserialize ( o.toString (), acll );
			}
		}
	}

	public void onAclUpdate ( AccessControlList acl )
	{
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		final AccessControlList acl = getAclFor ( resource );
		if ( acl == null ) return true;

		final Identity user = loadUserOrAlias ( id );
		return acl.canUser ( id, user.getGroupIds (), operation );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		// remove any existing tag of the same type
		removeMatchingTag ( userId, appTagType );

		// build the tag entry
		final String tagId = UniqueStringGenerator.createUrlKey ( nonce );
		final long expiration = ( Clock.now () + TimeUnit.MILLISECONDS.convert ( duration, durationTimeUnit ) ) / 1000;
		final JSONObject entry =
			new JSONObject ()
				.put ( kTagId, tagId )
				.put ( kUserId, userId )
				.put ( kTagType, appTagType )
				.put ( kExpireEpoch, expiration )
		;

		storeTagObject ( tagId, userId, appTagType, entry );

		return tagId;
	}

	@Override
	public String getUserIdForTag ( String tagId ) throws IamSvcException
	{
		final JSONObject tag = loadTagObject ( tagId, false );
		return ( tag == null ) ? null : tag.getString ( kUserId );
	}


	@Override
	public void removeMatchingTag ( String userId, String appTagType ) throws IamSvcException
	{
		// find the tag by user...
		String tagId = null;
		final JSONObject existing = loadTagObject ( userId, appTagType, true );
		if ( existing != null )
		{
			tagId = existing.getString ( kTagId );
		}

		if ( tagId != null )
		{
			deleteTagObject ( tagId, userId, appTagType );
		}
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException
	{
		// build the alias entry
		final JSONObject entry =
			new JSONObject ()
				.put ( kAlias, alias )
				.put ( kUserId, userId )
		;

		storeAliasObject ( alias, entry );
	}

	@Override
	public void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException
	{
		deleteAliasObject ( alias );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		return new TreeSet<String> ( loadAliasesForUser ( userId ) );
	}

	public void addJwtValidator ( JwtValidator v )
	{
		fJwtValidators.add ( v );
	}
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	protected CommonJsonDb ()
	{
		this ( null, null );
	}

	public interface AclFactory
	{
		AccessControlList createDefaultAcl ( AclUpdateListener acll );
	}

	private static class DefaultAclFactory implements AclFactory
	{
		@Override
		public AccessControlList createDefaultAcl ( AclUpdateListener acll )
		{
			return AccessControlList.initialize ( acll );
		}
	}

	protected CommonJsonDb ( AclFactory aclMaker, JwtProducer jwtProd )
	{
		fAclFactory =
			aclMaker == null ?
				new DefaultAclFactory () :
				aclMaker
		;

		fJwtTokenFactory = jwtProd;

		fJwtValidators = new LinkedList<> ();
		if ( fJwtTokenFactory != null )
		{
			fJwtValidators.add ( fJwtTokenFactory );	// our factory is a validator for its own tokens
		}
	}

	/**
	 * return a nonce value for used in seeding things like password salts
	 * @return a string
	 */
	public String getAppNonce ()
	{
		return "my app didn't register a nonce";
	}

	public ApiKey createApiKey ( String userId ) throws IamIdentityDoesNotExist, IamSvcException, IamBadRequestException
	{
		if ( userId == null ) throw new IamBadRequestException ( "A valid user ID is required to create an API key." );

		final String appSig = getAppNonce ();
		final String newApiKey = generateKey ( 16, appSig );
		final String newApiSecret = generateKey ( 24, appSig );

		final JSONObject o = createApiKeyObject ( userId, newApiKey, newApiSecret );
		storeApiKeyObject ( newApiKey, o );

		return instantiateApiKey ( newApiKey, loadApiKeyObject ( newApiKey ) );
	}

	protected abstract JSONObject createNewUser ( String id );
	protected abstract JSONObject loadUserObject ( String id ) throws IamSvcException;
	protected abstract void storeUserObject ( String id, JSONObject data ) throws IamSvcException;
	protected abstract void deleteUserObject ( String id ) throws IamSvcException;
	protected abstract I instantiateIdentity ( String id, JSONObject data );
	
	protected abstract JSONObject createNewGroup ( String id, String groupDesc );
	protected abstract JSONObject loadGroupObject ( String id ) throws IamSvcException;
	protected abstract void storeGroupObject ( String id, JSONObject data ) throws IamSvcException;
	protected abstract void deleteGroupObject ( String id ) throws IamSvcException;
	protected abstract G instantiateGroup ( String id, JSONObject data );

	protected abstract JSONObject createApiKeyObject ( String userId, String apiKey, String apiSecret );
	protected abstract JSONObject loadApiKeyObject ( String id ) throws IamSvcException;
	protected abstract void storeApiKeyObject ( String id, JSONObject data ) throws IamSvcException, IamIdentityDoesNotExist, IamBadRequestException;
	protected abstract void deleteApiKeyObject ( String id ) throws IamSvcException;
	protected abstract ApiKey instantiateApiKey ( String id, JSONObject data );
	protected abstract Collection<String> loadApiKeysForUser ( String userId ) throws IamSvcException, IamIdentityDoesNotExist;

	protected abstract JSONObject loadAclObject ( String id ) throws IamSvcException;
	protected abstract void storeAclObject ( String id, JSONObject data ) throws IamSvcException;
	protected abstract void deleteAclObject ( String id ) throws IamSvcException;

	protected abstract JSONObject loadTagObject ( String id, boolean expiredOk ) throws IamSvcException;
	protected abstract JSONObject loadTagObject ( String userId, String appTagType, boolean expiredOk ) throws IamSvcException;
	protected abstract void storeTagObject ( String id, String userId, String appTagType, JSONObject data ) throws IamSvcException;
	protected abstract void deleteTagObject ( String id, String userId, String appTagType ) throws IamSvcException;

	protected abstract JSONObject loadAliasObject ( String id ) throws IamSvcException;
	protected abstract void storeAliasObject ( String id, JSONObject data ) throws IamSvcException, IamBadRequestException;
	protected abstract void deleteAliasObject ( String id ) throws IamSvcException;
	protected abstract Collection<String> loadAliasesForUser ( String userId ) throws IamSvcException, IamIdentityDoesNotExist;

	protected abstract void storeInvalidJwtToken ( String token ) throws IamSvcException;
	protected abstract boolean isInvalidJwtToken ( String token ) throws IamSvcException;

	private static final String kKeyChars = "ABCDEFGHJIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	public static String generateKey ( int length, String nonce  )
	{
		return UniqueStringGenerator.createKeyUsingAlphabet ( nonce, kKeyChars, length );
	}

	private final AclFactory fAclFactory;
	private final JwtProducer fJwtTokenFactory;
	private final LinkedList<JwtValidator> fJwtValidators;
	
	static final int kSaltChars = 64;

	private static final Logger log = LoggerFactory.getLogger ( CommonJsonDb.class );
	private static final boolean skAuthLogging = true;
	private static void authLog ( String msg )
	{
		if ( skAuthLogging )
		{
			log.info ( msg );
		}
		else
		{
			log.debug ( msg );
		}
	}
}
