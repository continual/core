package io.continual.iam.impl.auth0;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.auth0.json.mgmt.Role;

import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;

/**
 * An Auth0 group.
 * 
 * We use Auth0's roles to represent groups. The role objects have an opaque ID as well as a name. Our group structure
 * does as well. However... convention prior to integrating Auth0 has been to create groups with a specified ID that
 * matched the group name. (We can't specify IDs to Auth0 of course.) For systems that are adopting Auth0 as supplemental
 * auth and user management on existing data, our group IDs (e.g. "acme") don't line up with the new role IDs ("role-1234")
 * so we'll use Auth0's name field for both our name and the ID.
 * 
 */
class Auth0Group implements Group, Comparable<Auth0Group>
{
	public Auth0Group ( Auth0IamDb db, Role r )
	{
		fDb = db;
		fRole = r;
	}

	@Override
	public int hashCode ()
	{
		return fRole.getId ().hashCode ();
	}

	@Override
	public boolean equals ( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass () != obj.getClass () )
			return false;
		Auth0Group other = (Auth0Group) obj;
		return fRole.getId ().equals ( other.fRole.getId () );
	}

	@Override
	public int compareTo ( Auth0Group that )
	{
		return fRole.getId ().compareTo ( that.fRole.getId () );
	}

	@Override
	public void reload ()
	{
		// ignored
	}

	@Override
	public String getUserData ( String key )
	{
		return null;
	}

	@Override
	public void putUserData ( String key, String val ) throws IamSvcException
	{
		throw new IamSvcException ( "Can't update user data here." );
	}

	@Override
	public void removeUserData ( String key ) throws IamSvcException
	{
		throw new IamSvcException ( "Can't update user data here." );
	}

	@Override
	public Map<String, String> getAllUserData ()
	{
		// auth0 doesn't store data in roles
		return new HashMap<> ();
	}

	@Override
	public String getId ()
	{
		//return fRole.getId ();
		return getName ();
	}

	@Override
	public String getName ()
	{
		return fRole.getName ();
	}

	@Override
	public boolean isMember ( String userId ) throws IamSvcException
	{
		return getMembers().contains ( userId );
	}

	@Override
	public Set<String> getMembers () throws IamSvcException
	{
		try
		{
			return fDb.getUsersInGroup ( getId() );
		}
		catch ( IamGroupDoesNotExist e )
		{
			throw new IamSvcException ( e );
		}
	}

	private final Auth0IamDb fDb;
	private final Role fRole;
}
