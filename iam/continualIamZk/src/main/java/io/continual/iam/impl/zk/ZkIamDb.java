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
package io.continual.iam.impl.zk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.JwtValidator;
import io.continual.iam.impl.common.CommonJsonApiKey;
import io.continual.iam.impl.common.CommonJsonDb;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.iam.impl.common.jwt.SimpleJwtValidator;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.time.Clock;

public abstract class ZkIamDb<I extends CommonJsonIdentity, G extends CommonJsonGroup> extends CommonJsonDb<I,G>
{
	public static abstract class Builder<I extends CommonJsonIdentity, G extends CommonJsonGroup>
	{
		public Builder<I,G> connectingTo ( String key )
		{
			fZkConnectionString = key;
			return this;
		}

		public Builder<I,G> withPathPrefix ( String pathPrefix )
		{
			this.prefix = pathPrefix;
			return this;
		}

		public Builder<I,G> usingAclFactory ( AclFactory af )
		{
			this.fAclFactory = af;
			return this;
		}

		public Builder<I,G> withJwtProducer ( JwtProducer p )
		{
			this.fJwtProducer = p;
			return this;
		}

		public Builder<I,G> addJwtValidator ( JwtValidator v )
		{
			fJwtValidators.add ( v );
			return this;
		}

		public abstract ZkIamDb<I,G> build () throws IamSvcException;

		private String fZkConnectionString;
		private String prefix;
		private AclFactory fAclFactory;
		private JwtProducer fJwtProducer = null;
		private LinkedList<JwtValidator> fJwtValidators = new LinkedList<> ();
	}

	public static <I extends CommonJsonIdentity, G extends CommonJsonGroup> void populateBuilderFrom ( Builder<I,G> b, ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final ExpressionEvaluator evaluator = sc.getExprEval ( config );
		final String sysAdminGroup = evaluator.evaluateText ( config.optString ( "sysAdminGroup", "sysadmin" ) );

		final JSONObject jwt = config.optJSONObject ( "jwt" );
		if ( jwt != null )
		{
			final String jwtIssuer = jwt.optString ( "issuer", null );
			final String jwtSecret = jwt.optString ( "sha256Key", null );
			if ( jwtIssuer != null && jwtSecret != null )
			{
				b.withJwtProducer ( new JwtProducer.Builder ()
					.withIssuerName ( jwtIssuer )
					.usingSigningKey ( jwtSecret )
					.build ()
				);
			}
		}

		// get the ZK settings
		final JSONObject zkConfig = config.getJSONObject ( "zk" );

		// build our ZK db
		b
			.connectingTo ( evaluator.evaluateText ( zkConfig.getString ( "connectionString" ) ) )
			.withPathPrefix ( evaluator.evaluateText ( zkConfig.optString ( "pathPrefix", "" ) ) )
			.usingAclFactory ( new AclFactory ()
			{
				@Override
				public AccessControlList createDefaultAcl ( AclUpdateListener acll )
				{
					final AccessControlList acl = new AccessControlList ( acll );
					acl
						.permit ( sysAdminGroup, AccessControlList.READ )
						.permit ( sysAdminGroup, AccessControlList.UPDATE )
						.permit ( sysAdminGroup, AccessControlList.CREATE )
						.permit ( sysAdminGroup, AccessControlList.DELETE )
					;
					return acl;
				}
			} )
		;

		// optionally add 3rd party JWT validators to the db
		if ( jwt != null )
		{
			final JSONArray auths = jwt.optJSONArray ( "thirdPartyAuth" );
			JsonVisitor.forEachElement ( auths, new ArrayVisitor<JSONObject,BuildFailure> ()
			{
				@Override
				public boolean visit ( JSONObject authEntry ) throws JSONException,BuildFailure
				{
					final String keys = authEntry.optString ( "keys" );
					
					final SimpleJwtValidator v = new SimpleJwtValidator.Builder ()
						.named ( authEntry.optString ( "name", "(anonymous)" ) )
						.forIssuer ( authEntry.getString ( "issuer" ) )
						.forAudience ( authEntry.getString ( "audience" ) )
						.getPublicKeysFrom ( keys )
						.build ()
					;
					b.addJwtValidator ( v );
					return true;
				}
			} );
		}
	}
	
	protected ZkIamDb ( Builder<I,G> b ) throws IamSvcException
	{
		super ( b.fAclFactory, b.fJwtProducer );

		fZk = CuratorFrameworkFactory
			.builder ()
			.namespace ( b.prefix )
			.connectString ( b.fZkConnectionString )
			.retryPolicy ( new ExponentialBackoffRetry ( 1000, 3 ) )
			.build ()
		;
	}

	@Override
	public void start () throws IamSvcException
	{
		super.start ();
		
		fZk.start ();

		// make sure this ZK db has the correct parent paths
		ensurePathExists ( "users" );
		ensurePathExists ( "groups" );
		ensurePathExists ( "apikeys/byKey" );
		ensurePathExists ( "acls" );
		ensurePathExists ( "tags/byTag" );
		ensurePathExists ( "tags/byUser" );
		ensurePathExists ( "aliases/byKey" );
		ensurePathExists ( "aliases/byUser" );
		ensurePathExists ( "invalidJwts" );
	}

	@Override
	public void close ()
	{
		fZk.close ();
	}

	@Override
	public Map<String, I> loadAllUsers () throws IamSvcException
	{
		final HashMap<String,I> result = new HashMap<> ();
		for ( String userId : getAllUsers () )
		{
			result.put ( userId, loadUser ( userId ) );
		}
		return result;
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		final String key = concatPathParts ( "users" );
		final LinkedList<String> result = new LinkedList<String> ();
		result.addAll ( loadKeysBelow ( key ) );
		return result;
	}

	@Override
	public Collection<String> getAllGroups () throws IamSvcException
	{
		final String prefix = concatPathParts ( "groups" );
		final LinkedList<String> result = new LinkedList<String> ();
		for ( String key : loadKeysBelow ( prefix ) )
		{
			final String localPart = key.substring ( prefix.length () );
			if ( localPart.length () > 0 )
			{
				result.add ( localPart );
			}
		}
		return result;
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		final String sysPrefix = concatPathParts ( "users" );
		final String prefix = concatPathParts ( sysPrefix, startingWith );
		final List<String> matches = loadKeysBelow ( prefix );
		final LinkedList<String> result = new LinkedList<String> ();
		for ( String match : matches )
		{
			result.add ( match.substring ( sysPrefix.length () + 1 ) );
		}
		return result;
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		for ( String key : loadKeysBelow ( "/tags/byTag" )  )
		{
			loadTagObject ( key, false );
		}
	}

	private final CuratorFramework fZk;

	String concatPathParts ( String... parts )
	{
		final StringBuilder sb = new StringBuilder ();
		for ( String part : parts )
		{
			if ( part.startsWith ( "/" ) ) part = part.substring ( 1 );
			if ( part.endsWith ( "/" ) ) part = part.substring ( 0, part.length () - 1 );
			
			sb
				.append ( "/" )
				.append ( part )
			;
		}
		return sb.toString ();
	}

	String makeUserId ( String userId )
	{
		return concatPathParts ( "users/", userId );
	}

	String makeGroupId ( String groupId )
	{
		return concatPathParts ( "groups/", groupId );
	}

	String makeByApiKeyId ( String apiKeyId )
	{
		return concatPathParts ( "apikeys/byKey/", apiKeyId );
	}

	String makeAclId ( String aclId )
	{
		return concatPathParts ( "acls/", aclId );
	}

	String makeByTagId ( String tagId )
	{
		return concatPathParts ( "tags/byTag/", tagId );
	}

	String makeByUserTagId ( String userId, String type )
	{
		return concatPathParts ( "tags/byUser/", userId, type );
	}

	String makeByAliasId ( String alias )
	{
		return concatPathParts ( "aliases/byKey/", alias );
	}

	String makeByUserAliasId ( String userId )
	{
		return concatPathParts ( "aliases/byUser/", userId );
	}

	String makeJwtTokenId ( String token )
	{
		return concatPathParts ( "invalidJwts/", token );
	}

	/**
	 * Load an object by key, returning the stream or null
	 * @param key
	 * @return a stream or null if not found
	 * @throws IamSvcException
	 */
	private InputStream load ( String key ) throws IamSvcException
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		if ( loadTo ( key, baos ) )
		{
			return new ByteArrayInputStream ( baos.toByteArray () );
		}
		return null;
	}

	/**
	 * Load an object by key, returning the object or null if not found
	 * @param key
	 * @return the object or null if not found
	 * @throws IamSvcException
	 */
	private JSONObject loadObject ( String key ) throws IamSvcException
	{
		final long startMs = Clock.now ();
		try
		{
			final InputStream is = load ( key );
			if ( is == null ) return null;

			final JSONObject result = new JSONObject ( new CommentedJsonTokener ( is ) );

			final long durMs = Clock.now () - startMs; 
			if ( log.isDebugEnabled () )
			{
				log.debug ( "ZkIamDb.loadObject ( " + key + " ): from ZK, " + durMs + " ms" );
			}

			return result;
		}
		catch ( JSONException e )
		{
			throw new IamSvcException ( e );
		}
	}

	/**
	 * Load an object to a stream.
	 * @param key
	 * @param os
	 * @returns true if found, false if not found
	 * @throws IamSvcException
	 * @throws IamBadRequestException 
	 */
	private boolean loadTo ( String key, OutputStream os ) throws IamSvcException
	{
		try
		{
			final byte[] data = fZk
				.getData ()
				.forPath ( key )
			;

			StreamTools.copyStream ( new ByteArrayInputStream ( data ), os );

			return true;
		}
		catch ( KeeperException x )
		{
			switch ( x.code () )
			{
				case NONODE:
				{
					log.info ( "No node {}", key );
					return false;
				}

				default:
					throw new IamSvcException ( x ); 
			}
		}
		catch ( Exception x )
		{
			throw new IamSvcException ( x ); 
		}
	}

	List<String> loadKeysBelow ( String key ) throws IamSvcException
	{
		final LinkedList<String> result = new LinkedList<String> ();
		try
		{
			final List<String> children = fZk
				.getChildren ()
				.forPath ( key )
			;
			result.addAll ( children );
		}
		catch ( KeeperException x )
		{
			switch ( x.code () )
			{
				case NONODE:
				{
					log.info ( "No node {}", key );
					// ignore
				}
				break;

				default:
					throw new IamSvcException ( x ); 
			}
		}
		catch ( Exception x )
		{
			throw new IamSvcException ( x ); 
		}
		return result;
	}
	
	void storeObject ( String key, JSONObject o ) throws IamSvcException
	{
		try
		{
			final String data = o.toString ();
			final byte[] bytes = data.getBytes ( "UTF-8" );

			fZk
				.create ()
				.orSetData ()
				.creatingParentsIfNeeded ()
				.forPath ( key, bytes )
			;
		}
		catch ( Exception x )
		{
			throw new IamSvcException ( x ); 
		}
	}

	private void deleteObject ( String key ) throws IamSvcException
	{
		try
		{
			fZk
				.delete ()
				.forPath ( key )
			;
		}
		catch ( Exception x )
		{
			throw new IamSvcException ( x ); 
		}
	}

	@Override
	protected JSONObject createNewUser ( String id )
	{
		return CommonJsonIdentity.initializeIdentity ();
	}

	@Override
	protected JSONObject loadUserObject ( String id ) throws IamSvcException
	{
		return loadObject ( makeUserId ( id ) );
	}

	@Override
	protected void storeUserObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( makeUserId ( id ), data );
	}

	@Override
	protected void deleteUserObject ( String id ) throws IamSvcException
	{
		deleteObject ( makeUserId ( id ) );
	}

	@Override
	protected JSONObject createNewGroup ( String id, String groupDesc )
	{
		return CommonJsonGroup.initializeGroup ( groupDesc );
	}

	@Override
	protected JSONObject loadGroupObject ( String id ) throws IamSvcException
	{
		return loadObject ( makeGroupId ( id ) );
	}

	@Override
	protected void storeGroupObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( makeGroupId ( id ), data );
	}

	@Override
	protected void deleteGroupObject ( String id ) throws IamSvcException
	{
		deleteObject ( makeGroupId ( id ) );
	}

	protected JSONObject createApiKeyObject ( String userId, String apiKey, String apiSecret )
	{
		return CommonJsonApiKey.initialize ( apiSecret, userId );
	}

	@Override
	protected JSONObject loadApiKeyObject ( String id ) throws IamSvcException
	{
		return loadObject ( makeByApiKeyId ( id ) );
	}

	@Override
	protected void storeApiKeyObject ( String id, JSONObject apiKeyObject ) throws IamSvcException, IamBadRequestException
	{
		final String userId = apiKeyObject.optString ( kUserId, null );
		if ( userId == null ) throw new IamBadRequestException ( "no user specified for api key" );

		// make sure the user exists
		final JSONObject user = loadUserObject ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		// store in apikeys section
		storeObject ( makeByApiKeyId ( id ), apiKeyObject );
		
		// store with user
		JSONArray userApiKeys = user.optJSONArray ( "apiKeys" );
		if ( userApiKeys == null )
		{
			userApiKeys = new JSONArray ();
			user.put ( "apiKeys", userApiKeys );
		}
		final Set<String> existing = new TreeSet<String> ( JsonVisitor.arrayToList ( userApiKeys ) );
		if ( !existing.contains ( id ) )
		{
			userApiKeys.put ( id );
			storeUserObject ( userId, user );
		}
	}

	@Override
	protected void deleteApiKeyObject ( String id ) throws IamSvcException
	{
		final JSONObject apiKey = loadApiKeyObject ( id );
		if ( apiKey != null )
		{
			final String userId = apiKey.getString ( kUserId );
			final JSONObject user = loadUserObject ( userId );
			final JSONArray userApiKeys = user.optJSONArray ( "apiKeys" );
			if ( userApiKeys != null )
			{
				if ( JsonUtil.removeStringFromArray ( userApiKeys, id ) )
				{
					storeUserObject ( userId, user );
				}
			}
		}

		deleteObject ( makeByApiKeyId ( id ) );
	}

	@Override
	protected ApiKey instantiateApiKey ( String id, JSONObject data )
	{
		return new CommonJsonApiKey ( id, data );
	}

	@Override
	protected Collection<String> loadApiKeysForUser ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		// make sure the user exists
		final JSONObject user = loadUserObject ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );
		
		// read from user
		JSONArray userApiKeys = user.optJSONArray ( "apiKeys" );
		if ( userApiKeys != null )
		{
			return JsonVisitor.arrayToList ( userApiKeys );
		}

		return new LinkedList<String> ();
	}

	@Override
	protected JSONObject loadAclObject ( String id ) throws IamSvcException
	{
		return loadObject ( makeAclId ( id ) );
	}

	@Override
	protected void storeAclObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( makeAclId ( id ), data );
	}

	@Override
	protected void deleteAclObject ( String id ) throws IamSvcException
	{
		deleteObject ( makeAclId ( id ) );
	}

	@Override
	protected JSONObject loadTagObject ( String id, boolean expiredOk ) throws IamSvcException
	{
		final JSONObject entry = loadObject ( makeByTagId ( id ) );
		if ( entry == null ) return null;

		final long expireEpoch = entry.getLong ( kExpireEpoch );
		if ( expireEpoch < ( Clock.now () / 1000L ) && !expiredOk )
		{
			final String userId = entry.optString ( kUserId, null );
			final String tagType = entry.optString ( kTagType, entry.optString ( "type", null ) );
			if ( userId == null || tagType == null )
			{
				log.warn ( "Tag " + id + " is damaged." );
			}
			else
			{
				deleteTagObject ( id, userId, tagType );
				log.info ( "Tag " + id + " (" + userId + "/" + tagType + ") deleted." );
			}
			return null;
		}
		return entry;
	}

	@Override
	protected JSONObject loadTagObject ( String userId, String appTagType, boolean expiredOk ) throws IamSvcException
	{
		final JSONObject entry = loadObject ( makeByUserTagId ( userId, appTagType ) );
		if ( entry == null ) return null;

		final long expireEpoch = entry.getLong ( kExpireEpoch );
		if ( expireEpoch < ( Clock.now () / 1000L ) && !expiredOk )
		{
			removeMatchingTag ( userId, appTagType );
			return null;
		}

		return entry;
	}

	@Override
	protected void storeTagObject ( String id, String userId, String appTagType, JSONObject data ) throws IamSvcException
	{
		storeObject ( makeByTagId ( id ), data );
		storeObject ( makeByUserTagId ( userId, appTagType ), data );
	}

	@Override
	protected void deleteTagObject ( String id, String userId, String appTagType ) throws IamSvcException
	{
		deleteObject ( makeByTagId ( id ) );
		deleteObject ( makeByUserTagId ( userId, appTagType ) );
	}

	@Override
	protected JSONObject loadAliasObject ( String id ) throws IamSvcException
	{
		return loadObject ( makeByAliasId ( id ) );
	}

	@Override
	protected void storeAliasObject ( String id, JSONObject aliasObject ) throws IamSvcException, IamBadRequestException
	{
		final String userId = aliasObject.optString ( kUserId, null );
		if ( userId == null ) throw new IamBadRequestException ( "no user specified for alias" );

		// make sure the user exists
		final JSONObject user = loadUserObject ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		// store in apikeys section
		storeObject ( makeByAliasId ( id ), aliasObject );

		// store with user
		JSONArray userAliases = user.optJSONArray ( "aliases" );
		if ( userAliases == null )
		{
			userAliases = new JSONArray ();
			user.put ( "aliases", userAliases );
		}
		final Set<String> existing = new TreeSet<String> ( JsonVisitor.arrayToList ( userAliases ) );
		if ( !existing.contains ( id ) )
		{
			userAliases.put ( id );
			storeUserObject ( userId, user );
		}
	}

	@Override
	protected void deleteAliasObject ( String id ) throws IamSvcException
	{
		final JSONObject alias = loadAliasObject ( id );
		if ( alias != null )
		{
			final String userId = alias.getString ( kUserId );
			final JSONObject user = loadUserObject ( userId );
			final JSONArray userAliases = user.optJSONArray ( "aliases" );
			if ( userAliases != null )
			{
				if ( JsonUtil.removeStringFromArray ( userAliases, id ) )
				{
					storeUserObject ( userId, user );
				}
			}
		}

		deleteObject ( makeByAliasId ( id ) );
	}

	@Override
	protected Collection<String> loadAliasesForUser ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		// make sure the user exists
		final JSONObject user = loadUserObject ( userId );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );
		
		// read from user
		JSONArray userAliases = user.optJSONArray ( "aliases" );
		if ( userAliases != null )
		{
			return JsonVisitor.arrayToList ( userAliases );
		}

		return new LinkedList<String> ();
	}

	@Override
	protected void storeInvalidJwtToken ( String token ) throws IamSvcException
	{
		storeObject ( makeJwtTokenId ( token ), new JSONObject () );
	}

	@Override
	protected boolean isInvalidJwtToken ( String token ) throws IamSvcException
	{
		return null != loadObject ( makeJwtTokenId ( token ) );
	}

	private void ensurePathExists ( String... pathParts ) throws IamSvcException
	{
		final String path = concatPathParts ( pathParts );

		boolean exists = false;
		try
		{
			exists = null != fZk
				.checkExists ()
				.forPath ( path )
			;
		}
		catch ( Exception x )
		{
			throw new IamSvcException ( x ); 
		}

		if ( !exists )
		{
			storeObject ( path, new JSONObject () );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( ZkIamDb.class );
}
