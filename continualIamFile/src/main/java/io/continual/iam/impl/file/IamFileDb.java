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
package io.continual.iam.impl.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.impl.common.CommonJsonApiKey;
import io.continual.iam.impl.common.CommonJsonDb;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.db.file.JsonObjectFile;
import io.continual.util.time.Clock;

public class IamFileDb extends CommonJsonDb<CommonJsonIdentity,CommonJsonGroup>
{
	public static class Builder
	{
		public Builder usingFile ( File f )
		{
			this.file = f;
			return this;
		}

		public Builder withPassword ( String password )
		{
			this.password = password != null && password.length () > 0 ? password : null;
			return this;
		}

		public Builder readonly ( boolean ro )
		{
			this.readonly = ro;
			return this;
		}

		public Builder readonly ()
		{
			return readonly ( true );
		}

		public Builder forWrites ()
		{
			return readonly ( false );
		}

		public Builder forceInit ()
		{
			this.forceInit = true;
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

		public IamFileDb build () throws IamSvcException
		{
			return new IamFileDb ( this );
		}

		private File file = null;
		private boolean forceInit = false;
		private boolean readonly = true;
		private String password = null;
		private AclFactory aclFactory;
		private JwtProducer jwtProducer = null;
	}

	protected IamFileDb ( Builder b ) throws IamSvcException
	{
		super ( b.aclFactory, b.jwtProducer );

		try
		{
			boolean init = ( !b.file.exists () && b.file.getParentFile ().isDirectory () ) || b.forceInit;
			if ( init )
			{
				JsonObjectFile.initialize ( b.file, 1024 );
			}

			fDb = new JsonObjectFile ( b.file, !b.readonly, b.password );
			if ( init )
			{
				final JSONObject index = new JSONObject ()
					.put ( "users", new JSONObject () )
					.put ( "groups", new JSONObject () )
					// etc.
				;
				final long mainIndex = fDb.write ( index );
				if ( mainIndex != fDb.indexToAddress ( 0 ) )
				{
					throw new IamSvcException ( "Couldn't initialize JSON DB file propertly." );
				}
			}
			fMainIndex = fDb.read ( fDb.indexToAddress ( 0 ) );
		}
		catch ( IOException x )
		{
			throw new IamSvcException ( x );
		}
	}

	public void close () throws IOException
	{
		fDb.close ();
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
		final TreeSet<String> result = new TreeSet<> ();
		final JSONObject userIndex = fMainIndex.optJSONObject ( "users" );
		if ( userIndex != null )
		{
			result.addAll ( userIndex.keySet () );
		}
		return result;
	}

	@Override
	public Collection<String> getAllGroups() throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<> ();
		final JSONObject groupIndex = fMainIndex.optJSONObject ( "groups" );
		if ( groupIndex != null )
		{
			result.addAll ( groupIndex.keySet () );
		}
		return result;
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		final ArrayList<String> result = new ArrayList<> ();
		final JSONObject userIndex = fMainIndex.optJSONObject ( "users" );
		if ( userIndex != null )
		{
			for ( String userId : userIndex.keySet () )
			{
				if ( userId.startsWith ( startingWith ) )
				{
					result.add ( userId );
				}
			}
		}
		return result;
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<> ();
		final JSONObject tagIndex = fMainIndex.optJSONObject ( "tags" );
		if ( tagIndex != null )
		{
			result.addAll ( tagIndex.keySet () );
		}

		for ( String key : result )
		{
			loadTagObject ( key, false );	// which will drop expired tags
		}
	}

	private final JSONObject fMainIndex;
	private final JsonObjectFile fDb;

	/**
	 * Load an object by address, returning the object or null if not found
	 * @param addr
	 * @return the object or null if not found
	 * @throws IamSvcException
	 */
	private JSONObject loadObject ( long addr ) throws IamSvcException
	{
		try
		{
			return fDb.read ( addr );
		}
		catch ( JSONException | IOException e )
		{
			throw new IamSvcException ( e );
		}
	}

	private JSONObject loadObject ( String... ids ) throws IamSvcException
	{
		final long addr = readIndex ( ids );
		if ( addr < 0 ) return null;
		return loadObject ( addr );
	}

	private void storeObject ( long addr, JSONObject o ) throws IamSvcException
	{
		try
		{
			fDb.overwrite ( addr, o );
		}
		catch ( IOException x )
		{
			throw new IamSvcException ( x );
		}
	}

	private long storeNewObject ( JSONObject o ) throws IamSvcException
	{
		try
		{
			return fDb.write ( o );
		}
		catch ( IOException x )
		{
			throw new IamSvcException ( x );
		}
	}

	private void storeObject ( JSONObject data, String... ids ) throws IamSvcException
	{
		long addr = readIndex ( ids );
		if ( addr < 0 )
		{
			addr = storeNewObject ( data );
			updateIndex ( addr, ids );
		}
		else
		{
			storeObject ( addr, data );
		}
	}

	private void deleteObject ( String... ids ) throws IamSvcException
	{
		long addr = readIndex ( ids );
		if ( addr < 0 ) return;

		try
		{
			// delete the index entry
			deleteFromIndex ( ids );

			// delete the record
			fDb.delete ( addr );
		}
		catch ( IOException e )
		{
			throw new IamSvcException ( e );
		}
	}

	private static final long kEntryNotFound = -1L;

	private long readIndex ( String... ids )
	{
		JSONObject current = fMainIndex;
		for ( int i=0; i<ids.length-1; i++ )
		{
			current = current.optJSONObject ( ids[i] );
			if ( current == null ) return kEntryNotFound;
		}
		return current.optLong ( ids[ids.length-1], kEntryNotFound );
	}

	private void writeIndex () throws IamSvcException
	{
		try
		{
			fDb.overwrite ( fDb.indexToAddress ( 0 ), fMainIndex );
		}
		catch ( IOException e )
		{
			throw new IamSvcException ( e );
		}
	}

	private void updateIndex ( long addr, String... ids ) throws IamSvcException
	{
		JSONObject parent = null;
		JSONObject current = fMainIndex;
		for ( int i=0; i<ids.length-1; i++ )
		{
			parent = current;
			current = current.optJSONObject ( ids[i] );
			if ( current == null )
			{
				current = new JSONObject (); 
				parent.put ( ids[i], current );
			}
		}
		current.put( ids[ids.length-1], addr );
		writeIndex ();
	}

	private void deleteFromIndex ( String... ids ) throws IamSvcException
	{
		JSONObject current = fMainIndex;
		for ( int i=0; i<ids.length-1; i++ )
		{
			current = current.optJSONObject ( ids[i] );
			if ( current == null ) return;
		}
		current.remove ( ids[ids.length-1] );
		writeIndex ();
	}

	@Override
	protected JSONObject createNewUser ( String id )
	{
		return CommonJsonIdentity.initializeIdentity ();
	}

	@Override
	protected JSONObject loadUserObject ( String id ) throws IamSvcException
	{
		return loadObject ( "users", id );
	}

	@Override
	protected void storeUserObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( data, "users", id );
	}

	@Override
	protected void deleteUserObject ( String id ) throws IamSvcException
	{
		deleteObject ( "users", id );
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
		return loadObject ( "groups", id );
	}

	@Override
	protected void storeGroupObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( data, "groups", id );
	}

	@Override
	protected void deleteGroupObject ( String id ) throws IamSvcException
	{
		deleteObject ( "groups", id );
	}

	@Override
	protected CommonJsonGroup instantiateGroup ( String id, JSONObject data )
	{
		return new CommonJsonGroup ( this, id, data );
	}

	protected JSONObject createApiKeyObject ( String userId, String apiKey, String apiSecret )
	{
		return CommonJsonApiKey.initialize ( apiSecret, userId );
	}

	@Override
	protected JSONObject loadApiKeyObject ( String id ) throws IamSvcException
	{
		return loadObject ( "apiKeys", id );
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
		storeObject ( apiKeyObject, "apiKeys", id );

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

		deleteObject ( "apiKeys", id );
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
		return loadObject ( "acls", id );
	}

	@Override
	protected void storeAclObject ( String id, JSONObject data ) throws IamSvcException
	{
		storeObject ( data, "acls", id );
	}

	@Override
	protected void deleteAclObject ( String id ) throws IamSvcException
	{
		deleteObject ( "acls",id );
	}

	@Override
	protected JSONObject loadTagObject ( String id, boolean expiredOk ) throws IamSvcException
	{
		final JSONObject entry = loadObject ( "tags", id );
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
		final JSONObject entry = loadObject ( "tagsByUser", userId, appTagType );
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
		storeObject ( data, "tags", id );
		storeObject ( data, "tagsByUser", userId, appTagType );
	}

	@Override
	protected void deleteTagObject ( String id, String userId, String appTagType ) throws IamSvcException
	{
		deleteObject ( "tags", id );
		deleteObject ( "tagsByUser", userId, appTagType );
	}

	@Override
	protected JSONObject loadAliasObject ( String id ) throws IamSvcException
	{
		return loadObject ( "aliases", id );
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
		storeObject ( aliasObject, "aliases", id );

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

		deleteObject ( "aliases", id );
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
		storeObject ( new JSONObject(), "invalidJwts", token );
	}

	@Override
	protected boolean isInvalidJwtToken ( String token ) throws IamSvcException
	{
		return null != loadObject ( "invalidJwts", token );
	}

	private static final Logger log = LoggerFactory.getLogger ( IamFileDb.class );
}
