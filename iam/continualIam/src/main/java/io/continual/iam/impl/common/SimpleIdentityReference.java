package io.continual.iam.impl.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;

public class SimpleIdentityReference implements Identity
{
	public SimpleIdentityReference ( String id )
	{
		fId = id;
	}

	@Override
	public void reload () {}

	@Override
	public String getUserData ( String key ) { return null; }

	@Override
	public void putUserData ( String key, String val ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public void removeUserData ( String key ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public Map<String, String> getAllUserData () { return new HashMap<>(); }

	@Override
	public String getId () { return fId; }

	@Override
	public boolean isEnabled () { return true; }

	@Override
	public void enable ( boolean enable ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public void setPassword ( String password ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public ApiKey createApiKey () throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public Collection<String> loadApiKeysForUser () throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public void deleteApiKey ( ApiKey key ) throws IamSvcException { throw new IamSvcException ( "not implemented" ); }

	@Override
	public Set<String> getGroupIds () throws IamSvcException { return new TreeSet<> (); }

	@Override
	public Collection<Group> getGroups () throws IamSvcException { return new TreeSet<> (); }

	@Override
	public Group getGroup ( String groupId ) throws IamSvcException { return null; }

	private final String fId;
}
