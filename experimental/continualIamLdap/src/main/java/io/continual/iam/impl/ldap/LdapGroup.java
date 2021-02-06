package io.continual.iam.impl.ldap;

import java.util.Map;
import java.util.Set;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;

class LdapGroup implements Group
{
	@Override
	public void reload ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getUserData ( String key )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putUserData ( String key, String val )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUserData ( String key )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, String> getAllUserData ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMember ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<String> getMembers ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
