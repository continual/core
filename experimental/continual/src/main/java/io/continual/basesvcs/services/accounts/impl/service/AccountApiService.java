package io.continual.basesvcs.services.accounts.impl.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.continual.basesvcs.services.accounts.AccountItemDoesNotExistException;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.basesvcs.services.accounts.api.AccountServiceApiRouter;
import io.continual.basesvcs.services.accounts.impl.BaseAcctSvc;
import io.continual.basesvcs.services.http.HttpService;
import io.continual.builder.Builder.BuildFailure;
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
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class AccountApiService<I extends Identity,G extends Group> extends BaseAcctSvc<I,G>
{
	@SuppressWarnings("unchecked")
	public AccountApiService ( ServiceContainer sc, NvReadable settings ) throws BuildFailure, MissingReqdSettingException
	{
		final String localServiceName = settings.getString ( "localService" );
		fLocalService = sc.get ( localServiceName, AccountService.class );
		if ( fLocalService == null )
		{
			throw new BuildFailure ( "Cannot find 'localService'" );
		}

		final String httpServiceName = settings.getString ( "httpService" );
		final HttpService server = sc.get ( httpServiceName, HttpService.class );
		server.addRouter (
			"accountsApi",
			new AccountServiceApiRouter ( sc, settings, this )
		);
	}

	@Override
	public Path getAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException
	{
		return fLocalService.getAccountBasePath ( user );
	}

	@Override
	public Path setStandardAccountBasePath ( Identity user )
		throws IamSvcException, AccountItemDoesNotExistException
	{
		return fLocalService.setStandardAccountBasePath ( user );
	}

	@Override
	public boolean userExists ( String userId )
		throws IamSvcException
	{
		return fLocalService.userExists ( userId );
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias )
		throws IamSvcException
	{
		return fLocalService.userOrAliasExists ( userIdOrAlias );
	}

	@Override
	public I loadUser ( String userId )
		throws IamSvcException
	{
		return fLocalService.loadUser ( userId );
	}

	@Override
	public I loadUserOrAlias ( String userIdOrAlias )
		throws IamSvcException
	{
		return fLocalService.loadUserOrAlias ( userIdOrAlias );
	}

	@Override
	public List<String> findUsers ( String startingWith )
		throws IamSvcException
	{
		return fLocalService.findUsers ( startingWith );
	}

	@Override
	public I createUser ( String userId )
		throws IamIdentityExists, IamSvcException
	{
		return fLocalService.createUser ( userId );
	}

	@Override
	public I createAnonymousUser ()
		throws IamSvcException
	{
		return fLocalService.createAnonymousUser ();
	}

	@Override
	public void deleteUser ( String userId )
		throws IamSvcException
	{
		fLocalService.deleteUser ( userId );
	}

	@Override
	public void addAlias ( String userId, String alias )
		throws IamSvcException,
			IamBadRequestException
	{
		fLocalService.addAlias ( userId, alias );
	}

	@Override
	public void removeAlias ( String alias )
		throws IamBadRequestException,
			IamSvcException
	{
		fLocalService.removeAlias ( alias );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		return fLocalService.getAliasesFor ( userId );
	}

	@Override
	public boolean completePasswordReset ( String tag, String newPassword )
		throws IamSvcException
	{
		return fLocalService.completePasswordReset ( tag, newPassword );
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey )
		throws IamSvcException
	{
		return fLocalService.loadApiKeyRecord ( apiKey );
	}

	@Override
	public Collection<String> getAllUsers ()
		throws IamSvcException
	{
		return fLocalService.getAllUsers ();
	}

	@Override
	public Map<String, I> loadAllUsers ()
		throws IamSvcException
	{
		return fLocalService.loadAllUsers ();
	}

	@Override
	public I authenticate ( UsernamePasswordCredential upc )
		throws IamSvcException
	{
		return fLocalService.authenticate ( upc );
	}

	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		return fLocalService.createJwtToken ( ii );
	}

	@Override
	public JwtCredential parseJwtToken ( String token ) throws InvalidJwtToken, IamSvcException
	{
		return fLocalService.parseJwtToken ( token );
	}

	@Override
	public I authenticate ( JwtCredential jwt )
		throws IamSvcException
	{
		return fLocalService.authenticate ( jwt );
	}

	@Override
	public void invalidateJwtToken ( String token ) throws IamSvcException
	{
		fLocalService.invalidateJwtToken ( token );
	}

	@Override
	public I authenticate ( ApiKeyCredential akc )
		throws IamSvcException
	{
		return fLocalService.authenticate ( akc );
	}

	@Override
	public G createGroup ( String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		return fLocalService.createGroup ( groupDesc );
	}

	@Override
	public G createGroup ( String groupId, String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		return fLocalService.createGroup ( groupId, groupDesc );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		fLocalService.addUserToGroup ( groupId, userId );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist,
			IamGroupDoesNotExist
	{
		fLocalService.removeUserFromGroup ( groupId, userId );
	}

	@Override
	public Set<String> getUsersGroups ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		return fLocalService.getUsersGroups ( userId );
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId )
		throws IamSvcException,
			IamGroupDoesNotExist
	{
		return fLocalService.getUsersInGroup ( groupId );
	}

	@Override
	public G loadGroup ( String id )
		throws IamSvcException
	{
		return fLocalService.loadGroup ( id );
	}

	@Override
	public AccessControlList getAclFor ( Resource resource )
		throws IamSvcException
	{
		return fLocalService.getAclFor ( resource );
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation )
		throws IamSvcException
	{
		return fLocalService.canUser ( id, resource, operation );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration,
		TimeUnit durationTimeUnit, String nonce )
		throws IamSvcException
	{
		return fLocalService.createTag ( userId, appTagType, duration, durationTimeUnit, nonce );
	}

	@Override
	public String getUserIdForTag ( String tag )
		throws IamSvcException
	{
		return fLocalService.getUserIdForTag ( tag );
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType )
		throws IamSvcException
	{
		fLocalService.removeMatchingTag ( userId, appTagType );
	}

	@Override
	public void sweepExpiredTags ()
		throws IamSvcException
	{
		fLocalService.sweepExpiredTags ();
	}

	private final AccountService<I,G> fLocalService;
}
