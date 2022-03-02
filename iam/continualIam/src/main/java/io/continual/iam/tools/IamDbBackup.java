package io.continual.iam.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.IamDb;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.time.Clock;

class IamDbBackup<I extends Identity, G extends Group>
{
	public IamDbBackup ( IamDb<I,G> db )
	{
		fDb = db;
	}

	public void backupTo ( OutputStream os ) throws IamSvcException, IOException
	{
		final JSONObject doc = new JSONObject ();

		// FIXME: consider rebuilding this to stream segments of the file instead of storing the entire db
		// in memory
		
		// users
		{
			final JSONObject users = new JSONObject ();
			for ( Map.Entry<String,I> user : fDb.loadAllUsers ().entrySet () )
			{
				final I id = user.getValue ();
				final JSONObject userRec;
				if ( id instanceof CommonJsonIdentity )
				{
					userRec = ((CommonJsonIdentity)id).asJson ();
				}
				else
				{
					userRec = new JSONObject ();
				}
				userRec
					.put ( "enabled", id.isEnabled () )
					.put ( "groups", JsonVisitor.collectionToArray ( id.getGroupIds () ) )
					.put ( "data", JsonVisitor.mapOfStringsToObject ( id.getAllUserData () ) )
				;

				final JSONObject apiKeys = new JSONObject ();
				for ( String apiKey : id.loadApiKeysForUser () )
				{
					final ApiKey rec = fDb.loadApiKeyRecord ( apiKey );
					apiKeys.put ( apiKey, new JSONObject ()
						.put ( "secret", rec.getSecret () )
						.put ( "createdAtMs", rec.getCreationTimestamp () )
					);
				}
				userRec.put ( "apiKeys", apiKeys );

				
				users.put ( user.getKey (), userRec );
			}
			doc.put ( "users", users );
		}

		// groups
		{
			final JSONObject groups = new JSONObject ();
			for ( String groupId : fDb.getAllGroups () )
			{
				final G group = fDb.loadGroup ( groupId );

				final JSONObject groupRec = new JSONObject ()
					.put ( "name", group.getName () )
					.put ( "members", JsonVisitor.collectionToArray ( group.getMembers () ) )
					.put ( "data", JsonVisitor.mapOfStringsToObject ( group.getAllUserData () ) )
				;
				groups.put ( groupId, groupRec );
			}
			doc.put ( "groups", groups );
		}

		// output
		try ( PrintWriter fw = new PrintWriter ( os ) )
		{
			fw.println ( doc.toString ( 4 ) );
		}
	}

	public void restoreFrom ( InputStream is ) throws JSONException, FileNotFoundException, IOException, IamSvcException
	{
		final JSONObject db = new JSONObject ( new CommentedJsonTokener ( is ) );

		// make sure each group is present and updated
		JsonVisitor.forEachElement ( db.optJSONObject ( "groups" ), new ObjectVisitor<JSONObject,IamSvcException> ()
		{
			@Override
			public boolean visit ( String groupId, JSONObject groupJson ) throws JSONException, IamSvcException
			{
				G group = fDb.loadGroup ( groupId );
				if ( group == null )
				{
					try
					{
						group = fDb.createGroup ( groupId, groupJson.optString ( "name", groupId ) );
					}
					catch ( IamGroupExists e )
					{
						throw new IamSvcException ( "Conflict in DB response: group didn't exist, but IamGroupExists thrown" );
					}
				}

				final Group finalGroup = group;
				JsonVisitor.forEachElement ( groupJson.optJSONObject ( "data" ), new ObjectVisitor<String,IamSvcException> ()
				{
					@Override
					public boolean visit ( String dataKey, String dataVal ) throws IamSvcException
					{
						finalGroup.putUserData ( dataKey, dataVal );
						return true;
					}
				} );
				
				return true;
			}
		} );

		// make sure each user is present and updated
		JsonVisitor.forEachElement ( db.optJSONObject ( "users" ), new ObjectVisitor<JSONObject,IamSvcException> ()
		{
			@Override
			public boolean visit ( String userId, JSONObject user ) throws JSONException, IamSvcException
			{
				if ( !fDb.userExists ( userId ) )
				{
					try
					{
						fDb.createUser ( userId );
					}
					catch ( IamIdentityExists e )
					{
						throw new IamSvcException ( "Conflict in DB response: user didn't exist, but IamIdentityExists thrown" );
					}
				}

				final I userRec = fDb.loadUser ( userId );
				userRec.enable ( user.optBoolean ( "enabled", true ) );

				// credentials
				final JSONObject pwdBlock = user.optJSONObject ( "password" );
				if ( pwdBlock != null && userRec instanceof CommonJsonIdentity )
				{
					final String salt = pwdBlock.getString ( "salt" );
					final String hash = pwdBlock.getString ( "hash" );
					((CommonJsonIdentity)userRec).setPasswordSaltAndHash ( salt, hash );
				}
				
				// data settings
				JsonVisitor.forEachElement ( user.optJSONObject ( "data" ), new ObjectVisitor<String,IamSvcException> ()
				{
					@Override
					public boolean visit ( String dataKey, String dataVal ) throws IamSvcException
					{
						userRec.putUserData ( dataKey, dataVal );
						return true;
					}
				} );

				// group memberships
				JsonVisitor.forEachElement ( user.optJSONArray ( "groups" ), new ArrayVisitor<String,IamSvcException> ()
				{
					@Override
					public boolean visit ( String groupId ) throws IamSvcException
					{
						try
						{
							fDb.addUserToGroup ( groupId, userId );
						}
						catch ( IamIdentityDoesNotExist | IamGroupDoesNotExist e )
						{
							throw new IamSvcException ( e );
						}
						return true;
					}
				} );

				// API keys
				JsonVisitor.forEachElement ( user.optJSONObject ( "apiKeys" ), new ObjectVisitor<JSONObject,IamSvcException> ()
				{
					@Override
					public boolean visit ( String apiKey, JSONObject apiKeyRecord ) throws IamSvcException
					{
						final String secret = apiKeyRecord.getString ( "secret" );

						final ApiKey existingKey = fDb.loadApiKeyRecord ( apiKey );
						if ( existingKey != null )
						{
							// is the key for this user?
							if ( !existingKey.getUserId ().equals ( userId ) )
							{
								log.warn ( "API key " + apiKey + " exists for user " + existingKey.getUserId () + " but is being restored for " + userId + ". Deleting the key entirely..." );
								fDb.loadUser ( existingKey.getUserId () ).deleteApiKey ( existingKey );
								return true;
							}
							
							if ( !existingKey.getSecret ().equals ( secret ) )
							{
								userRec.deleteApiKey ( existingKey );
							}
							else
							{
								return true;
							}
						}
						
						try
						{
							fDb.restoreApiKey ( new ApiKey ()
							{
								@Override
								public String getUserId () { return userId; }

								@Override
								public String getKey () { return apiKey; }

								@Override
								public String getSecret () { return secret; }

								@Override
								public long getCreationTimestamp () { return apiKeyRecord.optLong ( "createdAtMs", Clock.now () ); }
							} );
						}
						catch ( IamBadRequestException e )
						{
							throw new IamSvcException ( e );
						}
						return true;
					}
				} );

				return true;
			}
		} );
	}

	private final IamDb<I,G> fDb;
	private static final Logger log = LoggerFactory.getLogger ( IamDbBackup.class );
}
