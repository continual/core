package io.continual.iam.impl.ldap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;

class LdapIdentity implements Identity
{
	public LdapIdentity ( String username )
	{
		
	}

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
	public boolean isEnabled ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void enable ( boolean enable )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPassword ( String password )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String requestPasswordReset ( long secondsUntilExpire, String nonce )
		throws IamSvcException,
			IamBadRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ApiKey createApiKey ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> loadApiKeysForUser ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getGroupIds ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Group> getGroups ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Group getGroup ( String groupId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
