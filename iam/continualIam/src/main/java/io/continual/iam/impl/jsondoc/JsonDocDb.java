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

import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.impl.common.CommonJsonApiKey;
import io.continual.iam.impl.common.CommonJsonDb;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.time.Clock;

/**
 * This "database" is a single JSON document.
 */
public class JsonDocDb extends CommonJsonDb<CommonJsonIdentity,CommonJsonGroup>
{
	public JsonDocDb ()
	{
		this ( new JSONObject () );
	}

	public JsonDocDb ( JSONObject doc )
	{
		fTop = doc;
	}

	@Override
	public void close ()
	{
	}

	public String serialize ()
	{
		return fTop.toString ();
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		final LinkedList<String> result = new LinkedList<String> ();
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
	public Collection<String> getAllUsers () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<String> ();
		JsonVisitor.forEachElement ( fTop.optJSONObject ( "users" ), new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String userId, JSONObject user ) throws JSONException
			{
				result.add ( userId );
				return true;
			}
		} );
		return result;
	}

	@Override
	public Collection<String> getAllGroups () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<String> ();
		JsonVisitor.forEachElement ( fTop.optJSONObject ( "groups" ), new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String userId, JSONObject user ) throws JSONException
			{
				result.add ( userId );
				return true;
			}
		} );
		return result;
	}

	@Override
	public Map<String, CommonJsonIdentity> loadAllUsers () throws IamSvcException
	{
		final HashMap<String,CommonJsonIdentity> result = new HashMap<String,CommonJsonIdentity> ();
		JsonVisitor.forEachElement ( fTop.optJSONObject ( "users" ), new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String userId, JSONObject user ) throws JSONException
			{
				result.put ( userId, instantiateIdentity ( userId, user ) );
				return true;
			}
		} );
		return result;
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		final JSONObject tags = fTop.optJSONObject ( "tags" );
		if ( tags == null ) return;

		final JSONObject byTag = tags.optJSONObject ( "byTag" );
		if ( byTag == null ) return;

		final LinkedList<JSONObject> removals = new LinkedList<> ();
		JsonVisitor.forEachElement ( byTag, new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String key, JSONObject t ) throws JSONException
			{
				if ( isExpired ( t ) )
				{
					removals.add ( t );
				}
				return true;
			}
		} );

		for ( JSONObject tag : removals )
		{
			deleteTag ( tag );
		}
	}

	private final JSONObject fTop;

	@Override
	protected JSONObject createNewUser ( String id )
	{
		return CommonJsonIdentity.initializeIdentity ();
	}

	@Override
	protected JSONObject loadUserObject ( String id ) throws IamSvcException
	{
		final JSONObject users = fTop.optJSONObject ( "users" );
		if ( users == null ) return null;
		return users.optJSONObject ( id );
	}

	@Override
	protected void storeUserObject ( String id, JSONObject data ) throws IamSvcException
	{
		JSONObject users = fTop.optJSONObject ( "users" );
		if ( users == null )
		{
			users = new JSONObject ();
			fTop.put ( "users", users );
		}
		users.put ( id, data );
	}

	@Override
	protected void deleteUserObject ( String id ) throws IamSvcException
	{
		final JSONObject users = fTop.optJSONObject ( "users" );
		if (null != users)
		{
			users.remove ( id );
		}
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
		final JSONObject groups = fTop.optJSONObject ( "groups" );
		if ( groups == null ) return null;
		return groups.optJSONObject(id);
	}

	@Override
	protected void storeGroupObject ( String id, JSONObject data ) throws IamSvcException
	{
		JSONObject groups = fTop.optJSONObject ( "groups" );
		if ( groups == null )
		{
			groups = new JSONObject ();
			fTop.put ( "groups", groups );
		}
		groups.put ( id, data );
	}

	@Override
	protected void deleteGroupObject ( String id ) throws IamSvcException
	{
		final JSONObject groups = fTop.optJSONObject ( "groups" );
		if ( groups != null )
		{
			groups.remove ( id );
		}
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
		final JSONObject apiKeys = fTop.optJSONObject("apikeys");
		if ( apiKeys == null ) return null;
		return apiKeys.optJSONObject(id);
	}

	@Override
	protected void storeApiKeyObject ( String apiKeyId, JSONObject data ) throws IamSvcException, IamBadRequestException
	{
		final String userId = data.optString ( kUserId, null );
		if ( userId == null ) throw new IamBadRequestException ( "no user specified for api key" );

		// make sure the user exists
		final JSONObject user = loadUserObject ( data.getString ( kUserId ) );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		// store in apikeys section
		JSONObject apiKeys = fTop.optJSONObject ( "apikeys" );
		if ( apiKeys == null )
		{
			apiKeys = new JSONObject ();
			fTop.put ( "apikeys", apiKeys );
		}
		apiKeys.put ( apiKeyId, data );
		
		// store with user
		JSONArray userApiKeys = user.optJSONArray ( "apiKeys" );
		if ( userApiKeys == null )
		{
			userApiKeys = new JSONArray ();
			user.put ( "apiKeys", apiKeys );
		}
		final Set<String> existing = new TreeSet<String> ( JsonVisitor.arrayToList ( userApiKeys ) );
		if ( !existing.contains ( apiKeyId ) )
		{
			userApiKeys.put ( apiKeyId );
		}
	}

	@Override
	protected void deleteApiKeyObject ( String id ) throws IamSvcException
	{
		final JSONObject apiKeys = fTop.optJSONObject ( "apikeys" );
		if ( apiKeys == null ) return;

		final JSONObject apiKey = apiKeys.getJSONObject(id);
		if ( apiKey != null )
		{
			final String userId = apiKey.getString ( kUserId );
			final JSONObject user = loadUserObject ( userId );
			JSONArray userApiKeys = user.optJSONArray ( "apiKeys" );
			if ( userApiKeys != null )
			{
				JsonUtil.removeStringFromArray ( userApiKeys, id );
			}
		}
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
		final JSONObject user = loadUserObject(userId);
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );
		
		// read from user
		JSONArray userApiKeys = user.optJSONArray("apiKeys");
		if ( userApiKeys != null )
		{
			return JsonVisitor.arrayToList ( userApiKeys );
		}

		return new LinkedList<String> ();
	}

	@Override
	protected JSONObject loadAclObject ( String id ) throws IamSvcException
	{
		final JSONObject acls = fTop.optJSONObject ( "acls" );
		if ( acls == null ) return null;
		return acls.optJSONObject ( id );
	}

	@Override
	protected void storeAclObject ( String id, JSONObject data ) throws IamSvcException
	{
		JSONObject acls = fTop.optJSONObject ( "acls" );
		if ( acls == null )
		{
			acls = new JSONObject ();
			fTop.put ( "acls", acls );
		}
		acls.put ( id, data );
	}

	@Override
	protected void deleteAclObject ( String id ) throws IamSvcException
	{
		final JSONObject acls = fTop.optJSONObject ( "acls" );
		if ( acls != null )
		{
			acls.remove ( id );
		}
	}

	private static boolean isExpired ( JSONObject entry )
	{
		final long expireEpoch = entry.getLong ( kExpireEpoch );
		return expireEpoch < ( Clock.now () / 1000L );
	}
	
	@Override
	protected JSONObject loadTagObject ( String id, boolean expiredOk ) throws IamSvcException
	{
		final JSONObject tags = fTop.optJSONObject ( "tags" );
		if ( tags == null ) return null;
	
		final JSONObject byTag = tags.optJSONObject ( "byTag" );
		if ( byTag == null ) return null;
	
		final JSONObject entry = byTag.optJSONObject(id);
		if ( entry == null ) return null;

		if ( isExpired ( entry ) && !expiredOk )
		{
			removeMatchingTag ( entry.getString ( kUserId ), entry.getString ( kTagType ) );
			return null;
		}
	
		return entry;
	}

	@Override
	protected JSONObject loadTagObject ( String userId, String appTagType, boolean expiredOk ) throws IamSvcException
	{
		// lookup required structures
		final JSONObject tags = fTop.optJSONObject ( "tags" );
		if ( tags == null ) return null;

		final JSONObject byUser = tags.optJSONObject ( "byUser" );
		if ( byUser == null ) return null;

		final JSONObject thisUser = byUser.optJSONObject ( userId );
		if ( thisUser == null ) return null;

		final JSONObject entry = thisUser.optJSONObject ( appTagType );
		if ( entry == null ) return null;

		if ( isExpired ( entry ) && !expiredOk )
		{
			removeMatchingTag ( userId, appTagType );
			return null;
		}
	
		return entry;
	}

	@Override
	protected void storeTagObject ( String id, String userId, String appTagType, JSONObject data ) throws IamSvcException
	{
		// lookup / create any missing structures
		JSONObject tags = fTop.optJSONObject ( "tags" );
		if ( tags == null )
		{
			tags = new JSONObject ();
			fTop.put ( "tags", tags );
		}
		JSONObject byTag = tags.optJSONObject ( "byTag" );
		if ( byTag == null )
		{
			byTag = new JSONObject ();
			tags.put ( "byTag", byTag );
		}
		JSONObject byUser = tags.optJSONObject ( "byUser" );
		if ( byUser == null )
		{
			byUser = new JSONObject ();
			tags.put ( "byUser", byUser );
		}
		JSONObject thisUser = byUser.optJSONObject ( userId );
		if ( thisUser == null )
		{
			thisUser = new JSONObject ();
			byUser.put ( userId, thisUser );
		}

		byTag.put ( id, data );
		thisUser.put ( appTagType, data );
	}

	private void deleteTag ( JSONObject entry ) throws JSONException, IamSvcException
	{
		deleteTagObject (
			entry.getString ( kTagId ),
			entry.getString ( kUserId ),
			entry.getString ( kTagType )
		);
	}

	@Override
	protected void deleteTagObject ( String id, String userId, String appTagType ) throws IamSvcException
	{
		final JSONObject tags = fTop.optJSONObject ( "tags" );
		if ( tags == null ) return;

		final JSONObject byTag = tags.optJSONObject ( "byTag" );
		if ( byTag != null )
		{
			byTag.remove ( id );
		}

		final JSONObject byUser = tags.optJSONObject("byUser");
		if ( byUser != null )
		{
			final JSONObject thisUser = byUser.optJSONObject ( userId );
			if ( thisUser != null )
			{
				thisUser.remove ( appTagType );
			}
		}
	}

	@Override
	protected JSONObject loadAliasObject ( String id ) throws IamSvcException
	{
		final JSONObject aliases = fTop.optJSONObject ( "aliases" );
		if ( aliases == null ) return null;
		return aliases.optJSONObject ( id );
	}

	@Override
	protected void storeAliasObject ( String apiKeyId, JSONObject data ) throws IamBadRequestException, IamSvcException
	{
		final String userId = data.optString ( kUserId, null );
		if ( userId == null ) throw new IamBadRequestException ( "no user specified on alias record" );

		// make sure the user exists
		final JSONObject user = loadUserObject ( data.getString ( kUserId ) );
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );

		// store in alias section
		JSONObject aliasKeys = fTop.optJSONObject ( "aliases" );
		if ( aliasKeys == null )
		{
			aliasKeys = new JSONObject ();
			fTop.put ( "aliases", aliasKeys );
		}
		aliasKeys.put ( apiKeyId, data );
		
		// store with user
		JSONArray userAliases = user.optJSONArray ( "aliases" );
		if ( userAliases == null )
		{
			userAliases = new JSONArray ();
			user.put ( "aliases", aliasKeys );
		}
		final Set<String> existing = new TreeSet<String> ( JsonVisitor.arrayToList ( userAliases ) );
		if ( !existing.contains ( apiKeyId ) )
		{
			userAliases.put ( apiKeyId );
		}
	}

	@Override
	protected void deleteAliasObject ( String id ) throws IamSvcException
	{
		final JSONObject aliases = fTop.optJSONObject ( "aliases" );
		if ( aliases == null ) return;

		final JSONObject alias = aliases.getJSONObject ( id );
		if ( alias != null )
		{
			final String userId = alias.getString ( kUserId );
			final JSONObject user = loadUserObject ( userId );
			JSONArray userAliases = user.optJSONArray ( "aliases" );
			if ( userAliases != null )
			{
				JsonUtil.removeStringFromArray ( userAliases, id );
			}
		}
	}

	@Override
	protected Collection<String> loadAliasesForUser ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		// make sure the user exists
		final JSONObject user = loadUserObject(userId);
		if ( user == null ) throw new IamIdentityDoesNotExist ( userId );
		
		// read from user
		JSONArray userAliases = user.optJSONArray("aliases");
		if ( userAliases != null )
		{
			return JsonVisitor.arrayToList ( userAliases );
		}

		return new LinkedList<String> ();
	}

	@Override
	protected void storeInvalidJwtToken ( String token ) throws IamSvcException
	{
		// lookup / create any missing structures
		JSONObject tokens = fTop.optJSONObject ( "tokens" );
		if ( tokens == null )
		{
			tokens = new JSONObject ();
			fTop.put ( "tokens", tokens );
		}
		tokens.put ( token, Clock.now () );
	}

	@Override
	protected boolean isInvalidJwtToken ( String token ) throws IamSvcException
	{
		JSONObject tokens = fTop.optJSONObject ( "tokens" );
		return ( tokens != null && tokens.has ( token ) );
	}

	@Override
	protected boolean checkPassword ( UsernamePasswordCredential upc, CommonJsonIdentity user )
	{
		final String salt = user.getPasswordSalt ();
		if ( kNoEncrypt.equals ( salt ) )
		{
			final String hash = user.getPasswordHash ();
			if ( hash != null && hash.equals ( upc.getPassword () ) )
			{
				return true;
			}
		}

		return super.checkPassword ( upc, user );
	}

	private static final String kNoEncrypt = "pepper";
}
