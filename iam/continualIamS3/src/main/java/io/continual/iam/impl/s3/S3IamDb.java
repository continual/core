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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.impl.common.CommonJsonApiKey;
import io.continual.iam.impl.common.CommonJsonDb;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.util.collections.LruCache;
import io.continual.util.data.StreamTools;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.time.Clock;

public class S3IamDb extends CommonJsonDb<CommonJsonIdentity,CommonJsonGroup>
{
	public static S3IamDb fromJson ( JSONObject config ) throws IamSvcException, BuildFailure
	{
		final String sysAdminGroup = config.optString ( "sysAdminGroup", "sysadmin" );

		Builder b = new Builder ()
			.withAccessKey ( config.getString ( "accessKey" ) )
			.withSecretKey ( config.getString ( "secretKey" ) )
			.withBucket ( config.getString ( "bucketId" ) )
			.withPathPrefix ( config.optString ( "pathPrefix", "" ) )
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
		final JSONObject jwt = config.optJSONObject ( "jwt" );
		if ( jwt != null )
		{
			final String jwtIssuer = jwt.optString ( "issuer", null );
			final String jwtSecret = jwt.optString ( "sha256Key", null );
			if ( jwtIssuer != null && jwtSecret != null )
			{
				b = b.withJwtProducer ( new JwtProducer.Builder ()
					.withIssuerName ( jwtIssuer )
					.usingSigningKey ( jwtSecret )
					.build () );
			}
		}
		return b.build ();
	}
	
	public static class Builder
	{
		public Builder withAccessKey ( String key )
		{
			apiKey = key;
			return this;
		}

		public Builder withSecretKey ( String key )
		{
			privateKey = key;
			return this;
		}

		public Builder withBucket ( String bucket )
		{
			this.bucket = bucket;
			return this;
		}

		public Builder withPathPrefix ( String pathPrefix )
		{
			this.prefix = pathPrefix;
			return this;
		}

		public Builder createBucketIfReqd ()
		{
			this.create = true;
			return this;
		}

		public Builder usingAclFactory ( AclFactory af )
		{
			this.aclFactory = af;
			return this;
		}

		public Builder withJwtProducer ( JwtProducer p )
		{
			this.jwtProducer = p;
			return this;
		}

		public S3IamDb build () throws IamSvcException, BuildFailure
		{
			final S3IamDb db = new S3IamDb ( apiKey, privateKey, bucket, prefix, aclFactory, jwtProducer );
			if ( create )
			{
				db.findOrCreateBucket ();
			}
			return db;
		}

		private String apiKey;
		private String privateKey;
		private String bucket;
		private String prefix;
		private AclFactory aclFactory;
		private boolean create = false;
		private JwtProducer jwtProducer = null;
	}

	@SuppressWarnings("deprecation")
	protected S3IamDb ( String s3ApiKey, String s3PrivateKey, String bucket, String prefix, AclFactory aclFactory, JwtProducer jwtIssuer ) throws BuildFailure
	{
		super ( aclFactory, jwtIssuer );

		if ( bucket == null || bucket.length () == 0 ) throw new BuildFailure ( "A bucket ID is required." );
		if ( prefix == null ) prefix = "";

		fDb = new AmazonS3Client ( new S3Creds ( s3ApiKey, s3PrivateKey ) );
		fBucketId = bucket;
		fPrefix = prefix;
		fCache = new LruCache<String,JSONObject>( 1024 );	// FIXME: clean caching
	}

	protected void findOrCreateBucket () throws IamSvcException
	{
		try
		{
			if ( !fDb.doesBucketExist ( fBucketId ) )
			{
				fDb.createBucket ( fBucketId );
			}
		}
		catch ( AmazonClientException x )
		{
			throw new IamSvcException ( x );
		}
	}

	public void close ()
	{
		fDb.shutdown ();
	}

	@Override
	public Map<String, CommonJsonIdentity> loadAllUsers () throws IamSvcException
	{
		final HashMap<String,CommonJsonIdentity> result = new HashMap<String,CommonJsonIdentity> ();
		for ( String userId : getAllUsers () )
		{
			result.put ( userId, loadUser ( userId ) );
		}
		return result;
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		final String prefix = concatPathParts ( getPrefix (), "users/" );
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
	public Collection<String> getAllGroups () throws IamSvcException
	{
		final String prefix = concatPathParts ( getPrefix (), "groups/" );
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
		final String sysPrefix = concatPathParts ( getPrefix (), "users" );
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
		final TreeSet<String> keys = new TreeSet<String> ();
		try
		{
			final String prefix = makeByTagId ( "" );
			final ListObjectsRequest listObjectRequest = new ListObjectsRequest()
				.withBucketName ( fBucketId )
				.withPrefix ( prefix )
			;
			ObjectListing objects = fDb.listObjects ( listObjectRequest );
			do
			{
				for ( S3ObjectSummary objectSummary : objects.getObjectSummaries () )
				{
					final String tagId = objectSummary.getKey ().substring ( prefix.length () );
					keys.add ( tagId );
				}
				objects = fDb.listNextBatchOfObjects ( objects );
			}
			while ( objects.isTruncated () );		
		}
		catch ( AmazonS3Exception x )
		{
			throw new IamSvcException ( x );
		}

		for ( String key : keys )
		{
			loadTagObject ( key, false );
		}
	}

	private final AmazonS3Client fDb;
	private final String fBucketId;
	private final String fPrefix;
	private final LruCache<String,JSONObject> fCache;

	private static final long kMaxCacheAgeMs = 1000 * 60;
	
	String getPrefix ()
	{
		return fPrefix + "/";
	}

	String concatPathParts ( String... parts )
	{
		final StringBuilder sb = new StringBuilder ();
		String last = null;
		for ( String part : parts )
		{
			if ( last != null && !last.endsWith ( "/" ) )
			{
				sb.append ( "/" );
			}
			if ( part.startsWith ( "/" ) )
			{
				part = part.substring ( 1 );
			}
			sb.append ( part );

			last = part;
		}
		return sb.toString ();
	}

	String makeUserId ( String userId )
	{
		return concatPathParts ( getPrefix (), "users/", userId );
	}

	String makeGroupId ( String groupId )
	{
		return concatPathParts ( getPrefix (), "groups/", groupId );
	}

	String makeByApiKeyId ( String apiKeyId )
	{
		return concatPathParts ( getPrefix (), "apikeys/byKey/", apiKeyId );
	}

	String makeByUserApiKeyId ( String userId, String apiKeyId )
	{
		return concatPathParts ( getPrefix (), "apikeys/byUser/", userId, apiKeyId );
	}

	String makeUserApiKeyFolderId ( String userId )
	{
		return concatPathParts ( getPrefix (), "apikeys/byUser/", userId );
	}

	String makeAclId ( String aclId )
	{
		return concatPathParts ( getPrefix (), "acls/", aclId );
	}

	String makeByTagId ( String tagId )
	{
		return concatPathParts ( getPrefix (), "tags/byTag/", tagId );
	}

	String makeByUserTagId ( String userId, String type )
	{
		return concatPathParts ( getPrefix (), "tags/byUser/", userId, type );
	}

	String makeByAliasId ( String alias )
	{
		return concatPathParts ( getPrefix (), "aliases/byKey/", alias );
	}

	String makeByUserAliasId ( String userId )
	{
		return concatPathParts ( getPrefix (), "aliases/byUser/", userId );
	}

	String makeJwtTokenId ( String token )
	{
		return concatPathParts ( getPrefix (), "invalidJwts/", token );
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
			final JSONObject cached = fCache.get ( key, kMaxCacheAgeMs );
			if ( cached != null )
			{
				final long durMs = Clock.now () - startMs; 
				if ( log.isDebugEnabled () )
				{
					log.debug ( "S3IamDb.loadObject ( " + key + " ): from cache, " + durMs + " ms" );
				}
				return cached;
			}

			final InputStream is = load ( key );
			if ( is == null ) return null;

			final JSONObject result = new JSONObject ( new CommentedJsonTokener ( is ) );
			fCache.put ( key, JsonUtil.clone ( result ) );

			final long durMs = Clock.now () - startMs; 
			if ( log.isDebugEnabled () )
			{
				log.debug ( "S3IamDb.loadObject ( " + key + " ): from S3, " + durMs + " ms" );
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
		S3Object object = null;
		try
		{
			object = fDb.getObject ( new GetObjectRequest ( fBucketId, key ) );
			final InputStream is = object.getObjectContent ();

			// s3 objects must be closed or will leak an HTTP connection
			StreamTools.copyStream ( is, os );

			return true;
		}
		catch ( AmazonServiceException x )
		{
			if ( 404 == x.getStatusCode () ) return false;
			throw new IamSvcException ( x ); 
		}
		catch ( AmazonClientException x )
		{
			throw new IamSvcException ( x ); 
		}
		catch ( IOException x )
		{
			throw new IamSvcException ( x ); 
		}
		finally
		{
			if ( object != null )
			{
				try
				{
					object.close ();
				}
				catch ( IOException e )
				{
					throw new IamSvcException ( e ); 
				}
			}
		}
	}

	List<String> loadKeysBelow ( String key ) throws IamSvcException
	{
		final LinkedList<String> result = new LinkedList<String> ();
		try
		{
			final ListObjectsRequest listObjectsRequest = new ListObjectsRequest ()
				.withBucketName ( fBucketId )
				.withPrefix ( key )
			;
			ObjectListing objectListing;
			do
			{
				objectListing = fDb.listObjects ( listObjectsRequest );
				for ( S3ObjectSummary objectSummary : objectListing.getObjectSummaries () )
				{
					result.add ( objectSummary.getKey () );
				}
				listObjectsRequest.setMarker ( objectListing.getNextMarker () );
			}
			while ( objectListing.isTruncated () );
		}
		catch ( AmazonClientException e )
		{
			throw new IamSvcException ( e );
		}	
		return result;
	}
	
	void storeObject ( String key, JSONObject o ) throws IamSvcException
	{
		try
		{
			fCache.put ( key, JsonUtil.clone ( o ) );

			final String data = o.toString ();
			final InputStream is = new ByteArrayInputStream ( data.getBytes ( "UTF-8" ) );
			final long length = data.length ();

			final ObjectMetadata om = new ObjectMetadata ();
			om.setContentLength ( length );
			om.setContentType ( "application/json" );
			fDb.putObject ( new PutObjectRequest ( fBucketId, key, is, om ) );
		}
		catch ( AmazonS3Exception x )
		{
			throw new IamSvcException ( x ); 
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new IamSvcException ( e );
		}
	}

	private void deleteObject ( String key ) throws IamSvcException
	{
		try
		{
			fCache.drop ( key );
			fDb.deleteObject ( fBucketId, key );
		}
		catch ( AmazonS3Exception x )
		{
			throw new IamSvcException ( x );
		}
	}

	private static class S3Creds implements AWSCredentials
	{
		public S3Creds ( String key, String secret )
		{
			fAccessKey = key;
			fPrivateKey = secret;
		}

		@Override
		public String getAWSAccessKeyId ()
		{
			return fAccessKey;
		}

		@Override
		public String getAWSSecretKey ()
		{
			return fPrivateKey;
		}

		private final String fAccessKey;
		private final String fPrivateKey;
	}

//////////////////////////

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
	protected CommonJsonIdentity instantiateIdentity ( String id, JSONObject data )
	{
		return new CommonJsonIdentity ( id, data, this );
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

	@Override
	protected CommonJsonGroup instantiateGroup ( String id, JSONObject data )
	{
		return new CommonJsonGroup ( id, data, this );
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

	private static final Logger log = LoggerFactory.getLogger ( S3IamDb.class );
}
