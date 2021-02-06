package io.continual.basesvcs.services.accounts.impl.remote;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.continual.basesvcs.services.accounts.AccountItemDoesNotExistException;
import io.continual.basesvcs.services.accounts.impl.BaseAcctSvc;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.util.naming.Path;

public class RemoteAccountService extends BaseAcctSvc<CommonJsonIdentity,CommonJsonGroup>
{
	@Override
	public Path getAccountBasePath ( Identity user )
		throws IamSvcException,
			AccountItemDoesNotExistException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path setStandardAccountBasePath ( Identity user )
		throws IamSvcException,
			AccountItemDoesNotExistException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean userExists ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CommonJsonIdentity loadUser ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonIdentity loadUserOrAlias ( String userIdOrAlias )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findUsers ( String startingWith )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonIdentity createUser ( String userId )
		throws IamIdentityExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonIdentity createAnonymousUser ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteUser ( String userId )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAlias ( String userId, String alias )
		throws IamSvcException,
			IamBadRequestException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAlias ( String alias )
		throws IamBadRequestException,
			IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<String> getAliasesFor ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getAllUsers ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, CommonJsonIdentity> loadAllUsers ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonIdentity authenticate ( UsernamePasswordCredential upc )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void invalidateJwtToken ( String token ) throws IamSvcException
	{
	}

	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		throw new IamSvcException ( "not implemented" );
	}

	@Override
	public JwtCredential parseJwtToken ( String token ) throws InvalidJwtToken, IamSvcException
	{
		throw new InvalidJwtToken ();
	}

	@Override
	public CommonJsonIdentity authenticate ( JwtCredential jwt )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonIdentity authenticate ( ApiKeyCredential akc )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonGroup createGroup ( String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonGroup createGroup ( String groupId, String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addUserToGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getUsersGroups ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId )
		throws IamSvcException,
			IamGroupDoesNotExist
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CommonJsonGroup loadGroup ( String id )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccessControlList getAclFor ( Resource resource )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration,
		TimeUnit durationTimeUnit, String nonce )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserIdForTag ( String tag )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType )
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sweepExpiredTags ()
		throws IamSvcException
	{
		// TODO Auto-generated method stub
		
	}

}
