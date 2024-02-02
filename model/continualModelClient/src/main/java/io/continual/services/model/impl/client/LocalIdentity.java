package io.continual.services.model.impl.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;

class LocalIdentity implements Identity
{
	public LocalIdentity ( String id )
	{
		fId = id;
	}

	@Override
	public void reload () {}

	@Override
	public String getUserData ( String key ) { return null; }

	@Override
	public void putUserData ( String key, String val ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public void removeUserData ( String key ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public Map<String, String> getAllUserData () { return new HashMap<>(); }

	@Override
	public String getId () { return fId; }

	@Override
	public boolean isEnabled () { return true; }

	@Override
	public void enable ( boolean enable ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public void setPassword ( String password ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public ApiKey createApiKey () throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public Collection<String> loadApiKeysForUser () throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public void deleteApiKey ( ApiKey key ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public Set<String> getGroupIds () throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public Collection<Group> getGroups () throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	@Override
	public Group getGroup ( String groupId ) throws IamSvcException { throw new IamSvcException ( "not implemented on client" ); }

	private final String fId;
}
