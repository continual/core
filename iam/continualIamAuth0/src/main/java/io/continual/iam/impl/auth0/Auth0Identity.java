package io.continual.iam.impl.auth0;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.auth0.json.mgmt.users.User;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;

class Auth0Identity implements Identity
{
	public Auth0Identity ( User u, Set<Auth0Group> groups )
	{
		fUser = u;
		fRoles = new TreeSet<> ();
		fRoles.addAll ( groups );
	}

	@Override
	public void reload ()
	{
		// ignore
	}

	@Override
	public String getUserData ( String key )
	{
		Object val = getAppMetadata ().get ( key );
		if ( val != null ) return val.toString ();

		val = getUserMetadata ().get ( key );
		if ( val != null ) return val.toString ();

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
		final HashMap<String,String> result = new HashMap<> ();

		for ( Entry<String, Object> e : getUserMetadata ().entrySet () )
		{
			result.put ( e.getKey (), e.getValue ().toString () );
		}
		for ( Entry<String, Object> e : getAppMetadata ().entrySet () )
		{
			result.put ( e.getKey (), e.getValue ().toString () );
		}

		return result;
	}

	@Override
	public Set<String> getGroupIds ()
	{
		final TreeSet<String> result = new TreeSet<> ();
		for ( Auth0Group r : fRoles )
		{
			result.add ( r.getName () );
		}
		return result;
	}

	@Override
	public Collection<Group> getGroups ()
	{
		final LinkedList<Group> result = new LinkedList<> ();
		for ( Auth0Group r : fRoles )
		{
			result.add ( r );
		}
		return result;
	}

	@Override
	public Group getGroup ( String groupId )
	{
		for ( Auth0Group r : fRoles )
		{
			if ( r.getName ().equals ( groupId ) )
			{
				return r;
			}
		}
		return null;
	}

	@Override
	public String getId ()
	{
		return fUser.getEmail ();
	}

	@Override
	public boolean isEnabled () throws IamSvcException
	{
		return !fUser.isBlocked ();
	}

	@Override
	public void enable ( boolean enable ) throws IamSvcException
	{
		throw new IamSvcException ( "Can't enable or disable here." );
	}

	@Override
	public void setPassword ( String password ) throws IamSvcException
	{
		throw new IamSvcException ( "Can't set password here." );
	}

	@Override
	public String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException, IamBadRequestException
	{
		throw new IamSvcException ( "Can't reset password here." );
	}

	@Override
	public ApiKey createApiKey () throws IamSvcException
	{
		return null;
	}

	@Override
	public Collection<String> loadApiKeysForUser ()
	{
		return new TreeSet<String> ();
	}

	@Override
	public void deleteApiKey ( ApiKey key )
	{
		// ignore
	}

	private final User fUser;
	private final TreeSet<Auth0Group> fRoles;

	private Map<String,Object> getAppMetadata ()
	{
		// auth0 lib returns null rather than empty map...
		final Map<String,Object> map = fUser.getAppMetadata ();
		return ( map == null ) ? new HashMap<> () : map;
	}
	
	private Map<String,Object> getUserMetadata ()
	{
		// auth0 lib returns null rather than empty map...
		final Map<String,Object> map = fUser.getUserMetadata ();
		return ( map == null ) ? new HashMap<> () : map;
	}
}
