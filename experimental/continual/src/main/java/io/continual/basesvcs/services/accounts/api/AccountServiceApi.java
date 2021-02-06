package io.continual.basesvcs.services.accounts.api;

import org.json.JSONObject;

import io.continual.basesvcs.model.user.UserContext;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.basesvcs.services.http.HttpServlet;
import io.continual.basesvcs.tools.ApiContextHelper;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.util.http.standards.HttpStatusCodes;
import io.continual.util.http.standards.MimeTypes;

/**
 * API handlers for the account service
 * 
 * @author peter
 */
public class AccountServiceApi extends ApiContextHelper
{
	public AccountServiceApi ( AccountService<?,?> theService )
	{
		fService = theService;
	}

/*
	public Path getAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException
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
	public boolean userExists ( String userId ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return false;
	}
*/
	public void getUser ( DrumlinRequestContext context, final String userId ) throws IamSvcException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( DrumlinRequestContext context, HttpServlet servlet, final UserContext user )
			{
				try
				{
					final Identity userRecord = fService.loadUser ( userId );
					if ( userRecord == null )
					{
						ApiContextHelper.sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "No such user." );
					}

					final JSONObject result = new JSONObject ()
						.put ( "user", new JSONObject ()
							.put ( "id",  userRecord.getId () ) )
					;
					sendStatusOk ( context, result );
				}
				catch ( IamSvcException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k503_serviceUnavailable )
							.put ( "message", e.getMessage () )
							.toString (),
						MimeTypes.kAppJson );
				}
				return null;
			}
		} );
	}
/*
	public CommonJsonIdentity loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
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

*/
	private final AccountService<?,?> fService;
}
