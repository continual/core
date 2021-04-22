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
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.util.data.OneWayHasher;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class CommonJsonIdentity extends CommonJsonObject implements Identity
{
	public static JSONObject initializeIdentity ()
	{
		return new JSONObject ()
			.put ( CommonJsonDb.kEnabled, true )
		;
	}

	public CommonJsonIdentity ( String userId, JSONObject userRecord, CommonJsonDb<? extends CommonJsonIdentity,?> db )
	{
		fDb = db;
		fUserId = userId;
		fUserRecord = userRecord;
	}

	@Override
	public String getId () { return fUserId; }

	@Override
	public String toString ()
	{
		return fApiKey == null ? 
			getId() :
			getId() + " (" + fApiKey + ")"
		;
	}

	public void setApiKeyUsedForAuth ( String apiKey )
	{
		fApiKey = apiKey;
	}

	/**
	 * Get the API key supplied to the constructor, if any.
	 * @return an API key or null
	 */
	public String getApiKey ()
	{
		return fApiKey;
	}

	public JSONObject asJson ()
	{
		return fUserRecord;
	}
	
	@Override
	public void setPassword ( String password ) throws IamSvcException
	{
		final String salt = generateSalt ( fDb.getAppNonce () );
		final String hash = generateHash ( password, salt );
		setPasswordSaltAndHash ( salt, hash );
	}

	public void setPasswordSaltAndHash ( String salt, String hash ) throws IamSvcException
	{
		final JSONObject o = new JSONObject ()
			.put ( CommonJsonDb.kPasswordSalt, salt )
			.put ( CommonJsonDb.kPasswordHash, hash )
		;
		fUserRecord.put ( CommonJsonDb.kPasswordBlock, o );
	
		fDb.storeUserObject ( getId(), asJson() );
	}

	@Override
	public String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException, IamBadRequestException
	{
		if ( !isEnabled() ) throw new IamBadRequestException ( getId() + " is disabled." );
		return fDb.createTag ( getId(), CommonJsonDb.kTagType_PasswordReset,
			secondsUntilExpire, TimeUnit.SECONDS, nonce );
	}

	@Override
	public ApiKey createApiKey () throws IamSvcException
	{
		try
		{
			return fDb.createApiKey ( getId () );
		}
		catch ( IamBadRequestException e )
		{
			// this shouldn't happen since we're inside the user record. on the other hand,
			// someone could delete the record after this object was created, so ignore
			log.warn ( e.getMessage (), e );
			throw new IamSvcException ( e );
		}
	}

	@Override
	public Collection<String> loadApiKeysForUser () throws IamSvcException
	{
		try
		{
			return fDb.loadApiKeysForUser ( getId () );
		}
		catch ( IamBadRequestException e )
		{
			// this shouldn't happen since we're inside the user record. on the other hand,
			// someone could delete the record after this object was created, so ignore
			log.warn ( e.getMessage (), e );
			throw new IamSvcException ( e );
		}
	}

	@Override
	public void deleteApiKey ( ApiKey key ) throws IamSvcException
	{
		fDb.deleteApiKeyObject ( key.getKey () );
	}

	@Override
	public void enable ( boolean enable ) throws IamSvcException
	{
		fUserRecord.put ( CommonJsonDb.kEnabled, enable );
		fDb.storeUserObject ( getId(), asJson() );
	}

	public boolean isEnabled ()
	{
		return fUserRecord.optBoolean ( CommonJsonDb.kEnabled, false );
	}

	public String getPasswordSalt ()
	{
		final JSONObject pwd = fUserRecord.optJSONObject ( CommonJsonDb.kPasswordBlock );
		if ( pwd != null )
		{
			return pwd.optString ( CommonJsonDb.kPasswordSalt, null );
		}
		return null;
	}

	public String getPasswordHash ()
	{
		final JSONObject pwd = fUserRecord.optJSONObject ( CommonJsonDb.kPasswordBlock );
		if ( pwd != null )
		{
			return pwd.optString ( CommonJsonDb.kPasswordHash, null );
		}
		return null;
	}

	@Override
	public Set<String> getGroupIds () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<String> ();
		JsonVisitor.forEachElement ( fUserRecord.optJSONArray ( "groups" ),
			new ArrayVisitor<String, IamSvcException> ()
			{
				@Override
				public boolean visit ( String groupId ) throws IamSvcException
				{
					result.add ( groupId );
					return true;
				}
			}
		);
		return result;
	}

	@Override
	public Collection<Group> getGroups () throws IamSvcException
	{
		final LinkedList<Group> result = new LinkedList<Group> ();
		for ( String groupId : getGroupIds () )
		{
			try
			{
				final Group g = fDb.loadGroup ( groupId );
				if ( g != null )
				{
					result.add ( g );
				}
			}
			catch ( JSONException e )
			{
				log.warn ( "Error loading group [" + groupId + "]", e );
			}
		}
		return result;
	}

	@Override
	public Group getGroup ( String groupId ) throws IamSvcException
	{
		if ( getGroupIds().contains ( groupId ) ) 
		{
			return fDb.loadGroup ( groupId );
		}
		return null;
	}

	public void addApiKey ( String newApiKey )
	{
		JSONArray a = fUserRecord.optJSONArray ( "apiKeys" );
		if ( a == null )
		{
			a = new JSONArray ();
			fUserRecord.put ( "apiKeys", a );
		}

		a.put ( newApiKey );
	}

	private final String fUserId;
	private final CommonJsonDb<? extends CommonJsonIdentity,?> fDb;
	private JSONObject fUserRecord;
	private String fApiKey = null;
	
	private static final Logger log = LoggerFactory.getLogger ( CommonJsonIdentity.class );

	protected boolean addGroup ( String groupId )
	{
		JSONArray groups = fUserRecord.optJSONArray ( "groups" );
		if ( groups == null )
		{
			groups = new JSONArray ();
			fUserRecord.put ( "groups", groups );
		}
		return JsonUtil.ensureStringInArray ( groupId, groups );
	}

	protected boolean removeGroup ( String groupId )
	{
		return JsonUtil.removeStringFromArray ( fUserRecord.optJSONArray ( "groups" ), groupId );
	}

	@Override
	protected JSONObject getDataRecord ()
	{
		return fUserRecord;
	}

	@Override
	public void reload () throws IamSvcException
	{
		fUserRecord = fDb.loadUserObject ( getId() );
	}

	@Override
	protected void store () throws IamSvcException
	{
		fDb.storeUserObject ( getId(), getDataRecord() );
	}

	private static String generateSalt ( String appNonce )
	{
		return CommonJsonDb.generateKey ( CommonJsonDb.kSaltChars, appNonce );
	}

	private static String generateHash ( String pwd, String salt )
	{
		return OneWayHasher.pbkdf2HashToString ( pwd, salt );
	}
	
	public static void main ( String[] args )
	{
		if ( args.length != 1 )
		{
			System.err.println ( "usage: CommonJsonIdentity <password>" );
			return;
		}

		final String salt = generateSalt ( null );
		final String hash = generateHash ( args[0], salt );
		System.out.println (
			new JSONObject ()
				.put ( "salt", salt )
				.put ( "hash", hash )
				.toString ()
		);
	}
}
