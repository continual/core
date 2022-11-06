package io.continual.iam.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
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
import io.continual.metrics.MetricsCatalog;
import io.continual.services.ServiceContainer;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class MultiSourceDb<I extends Identity,G extends Group> implements IamDb<I,G>
{
	public MultiSourceDb ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		final ExpressionEvaluator evaluator = sc.getExprEval (  );
		final JSONObject config = evaluator.evaluateJsonObject ( rawConfig );

		fDbs = new ArrayList<> ();
		
		try
		{
			final JSONArray dbStack = config.getJSONArray ( "dbs" );
			JsonVisitor.forEachElement ( dbStack, new ArrayVisitor<JSONObject,BuildFailure> ()
			{
				@SuppressWarnings("unchecked")
				@Override
				public boolean visit ( JSONObject dbConfig ) throws JSONException, BuildFailure
				{
					final IamDb<?,?> db = Builder.fromJson ( IamDb.class, dbConfig, sc );
					fDbs.add ( (IamDb<I,G>) db );

					return true;
				}
			} );
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public boolean userExists ( String userId ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			if ( db.userExists ( userId ) ) return true;
		}
		return false;
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			if ( db.userOrAliasExists ( userIdOrAlias ) ) return true;
		}
		return false;
	}

	@Override
	public I loadUser ( String userId ) throws IamSvcException
	{
		final IamDb<I,G> db = getDbFor ( userId );
		if ( db != null )
		{
			return db.loadUser ( userId );
		}
		return null;
	}

	@Override
	public I loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		final IamDb<I,G> db = getDbFor ( userIdOrAlias );
		if ( db != null )
		{
			return db.loadUserOrAlias ( userIdOrAlias );
		}
		return null;
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		final LinkedList<String> result = new LinkedList<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.findUsers ( startingWith ) );
		}
		return result;
	}

	@Override
	public I createUser ( String userId ) throws IamIdentityExists, IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public I createAnonymousUser () throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void deleteUser ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		final LinkedList<String> result = new LinkedList<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.getAliasesFor ( userId ) );
		}
		return result;
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final ApiKey key = db.loadApiKeyRecord ( apiKey );
			if ( key != null ) return key;
		}
		return null;
	}

	@Override
	public void restoreApiKey ( ApiKey key ) throws IamIdentityDoesNotExist, IamBadRequestException, IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void addJwtValidator ( JwtValidator v )
	{
		log.warn ( "addJwtValidator ignored in multisource db" );
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.getAllUsers () );
		}
		return result;
	}

	@Override
	public Map<String, I> loadAllUsers () throws IamSvcException
	{
		final HashMap<String,I> result = new HashMap<> ();
		final Collection<String> users = getAllUsers ();
		for ( String user : users )
		{
			result.put ( user, loadUser ( user ) );
		}
		return result;
	}

	@Override
	public I authenticate ( UsernamePasswordCredential upc ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final I i = db.authenticate ( upc );
			if ( i != null ) return i;
		}
		return null;
	}

	@Override
	public I authenticate ( ApiKeyCredential akc ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final I i = db.authenticate ( akc );
			if ( i != null ) return i;
		}
		return null;
	}

	@Override
	public I authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final I i = db.authenticate ( jwt );
			if ( i != null ) return i;
		}
		return null;
	}

	@Override
	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void invalidateJwtToken ( String jwtToken ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			db.invalidateJwtToken ( jwtToken );
		}
	}

	@Override
	public G createGroup ( String groupDesc ) throws IamGroupExists, IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public G createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		final TreeSet<String> result = new TreeSet<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.getUsersGroups ( userId ) );
		}
		return result;
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist
	{
		final TreeSet<String> result = new TreeSet<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.getUsersInGroup ( groupId ) );
		}
		return result;
	}

	@Override
	public Collection<String> getAllGroups () throws IamSvcException
	{
		final TreeSet<String> result = new TreeSet<> ();
		for ( IamDb<I,G> db : fDbs )
		{
			result.addAll ( db.getAllGroups () );
		}
		return result;
	}

	@Override
	public G loadGroup ( String id ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			G group = db.loadGroup ( id );
			if ( group != null ) return group;
		}
		return null;
	}

	@Override
	public AccessControlList getAclFor ( Resource resource ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final AccessControlList acl = db.getAclFor ( resource );
			if ( acl != null ) return acl;
		}
		return null;
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented in multisource db" );
	}

	@Override
	public String getUserIdForTag ( String tag ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			final String result = db.getUserIdForTag ( tag );
			if ( result != null ) return result;
		}
		return null;
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			db.removeMatchingTag ( userId, appTagType );
		}
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			db.sweepExpiredTags ();
		}
	}

	@Override
	public void onAclUpdate ( AccessControlList accessControlList )
	{
		log.warn ( "onAclUpdate ignored in multisource db" );
	}

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
		int index = 1;
		for ( IamDb<I,G> db : fDbs )
		{
			db.populateMetrics ( metrics.getSubCatalog ( "" + index ) );
			index++;
		}
	}

	private final ArrayList<IamDb<I,G>> fDbs;
	private static final Logger log = LoggerFactory.getLogger ( MultiSourceDb.class );

	private IamDb<I,G> getDbFor ( String userId ) throws IamSvcException
	{
		for ( IamDb<I,G> db : fDbs )
		{
			if ( db.userExists ( userId ) ) return db;
		}
		return null;
	}
}
