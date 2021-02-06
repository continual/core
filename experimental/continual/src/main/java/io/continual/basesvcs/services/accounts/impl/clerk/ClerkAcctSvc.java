package io.continual.basesvcs.services.accounts.impl.clerk;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.accounts.AccountItemDoesNotExistException;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.basesvcs.services.accounts.impl.BaseAcctSvc;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
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
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.s3.S3IamDb;
import io.continual.services.ServiceContainer;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonEval;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.time.Clock;

public class ClerkAcctSvc extends BaseAcctSvc<CommonJsonIdentity,CommonJsonGroup>
{
	public ClerkAcctSvc ( final ServiceContainer sc, JSONObject settings ) throws IamSvcException
	{
		fClerk = new S3IamDb.Builder ()
			.withAccessKey ( JsonEval.evalToString ( settings, "accounts.aws.accessKey" ) )
			.withSecretKey ( JsonEval.evalToString ( settings, "accounts.aws.secretKey" ) )
			.withBucket ( JsonEval.evalToString ( settings, "bucketId" ) )
			.withPathPrefix ( JsonEval.evalToString ( settings, "pathPrefix" ) )
			.usingAclFactory ( new AclFactory ()
			{
				@Override
				public AccessControlList createDefaultAcl ( AclUpdateListener acll )
				{
					final AccessControlList acl = new AccessControlList ( acll );
					acl
						.permit ( AccountService.kSysAdminGroup, kReadOperation )
						.permit ( AccountService.kSysAdminGroup, kWriteOperation )
						.permit ( AccountService.kSysAdminGroup, kCreateOperation )
						.permit ( AccountService.kSysAdminGroup, kDeleteOperation )
					;
					return acl;
				}
			} )
			.build ()
		;

		final JSONObject jwt = settings.optJSONObject ( "jwt" );
		if ( jwt != null )
		{
			fJwtIssuer = jwt.optString ( "issuer", null );
			fJwtSecret = jwt.optString ( "sha256Key", null );
		}
		else
		{
			fJwtIssuer = null;
			fJwtSecret = null;
		}
	}

	@Override
	public Path getAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException
	{
		final String path = user.getUserData ( kData_BaseModelPath );
		if ( path == null )
		{
			log.warn ( "User [" + user.toString () + "] has no base path setting." );
			throw new AccountItemDoesNotExistException ( "User [" + user.toString () + "] has no base path setting." );
		}

		try
		{
			return Path.fromString ( path );
		}
		catch ( IllegalArgumentException e )
		{
			log.warn ( "User [" + user.toString () + "] has base path [" + path + "], which is illegal.");
			throw new AccountItemDoesNotExistException ( "User [" + user.toString () + "] has base path [" + path + "], which is illegal." );
		}
	}

	@Override
	public Path setStandardAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException
	{
		final Path path = Path.fromString ( "/accounts" ).makeChildItem ( Name.fromString ( user.getId () ) );
		user.putUserData ( kData_BaseModelPath, path.toString () );
		return path;
	}

	@Override
	public boolean userExists ( String userId )
		throws IamSvcException
	{
		return fClerk.userExists ( userId );
	}

	@Override
	public CommonJsonIdentity loadUser ( String userId )
		throws IamSvcException
	{
		return fClerk.loadUser ( userId );
	}

	@Override
	public List<String> findUsers ( String startingWith )
		throws IamSvcException
	{
		return fClerk.findUsers ( startingWith );
	}

	@Override
	public CommonJsonIdentity createUser ( String userId )
		throws IamIdentityExists,
			IamSvcException
	{
		return fClerk.createUser ( userId );
	}

	@Override
	public CommonJsonIdentity createAnonymousUser () throws IamSvcException
	{
		return fClerk.createAnonymousUser ();
	}

	@Override
	public void deleteUser ( String userId )
		throws IamSvcException
	{
		fClerk.deleteUser ( userId );
	}

	@Override
	public boolean completePasswordReset ( String tagId, String newPassword )
		throws IamSvcException
	{
		return fClerk.completePasswordReset ( tagId, newPassword );
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey )
		throws IamSvcException
	{
		return fClerk.loadApiKeyRecord ( apiKey );
	}

	@Override
	public Collection<String> getAllUsers ()
		throws IamSvcException
	{
		return fClerk.getAllUsers ();
	}

	@Override
	public Map<String, CommonJsonIdentity> loadAllUsers ()
		throws IamSvcException
	{
		return fClerk.loadAllUsers ();
	}

	@Override
	public CommonJsonIdentity authenticate ( UsernamePasswordCredential upc )
		throws IamSvcException
	{
		return fClerk.authenticate ( upc );
	}

	public String createJwtToken ( Identity ii ) throws IamSvcException
	{
		if ( fJwtIssuer == null || fJwtSecret == null )
		{
			// we're not configured to issue JWT tokens
			throw new IamSvcException ( "The authentication service is not configured to issue JWT tokens but the application has asked for one." );
		}

		// header
		final JSONObject header = new JSONObject ()
			.put ( "alg", "HS256" )
			.put ( "typ", "JWT" )
		;
		final String encodedHeader = TypeConvertor.base64UrlEncode ( header.toString () ); 

		// payload
		final JSONObject payload = new JSONObject ()
			.put ( "iss", fJwtIssuer )
			.put ( "sub", ii.getId () )
			.put ( "aud", new JSONArray ().put ( fJwtIssuer ) )
			.put ( "exp", ( Clock.now () + ( 24 * 60 * 60 * 1000 ) ) / 1000L )
		;
		final String encodedPayload = TypeConvertor.base64UrlEncode ( payload.toString () ); 

		final String headerAndPayload = encodedHeader + "." + encodedPayload;
		final byte[] sigBytes = Sha256HmacSigner.signToBytes ( headerAndPayload, fJwtSecret );
		final String signature = TypeConvertor.base64UrlEncode ( sigBytes );

		return headerAndPayload + "." + signature;
	}

	@Override
	public JwtCredential parseJwtToken ( String token ) throws InvalidJwtToken, IamSvcException
	{
		if ( fJwtSecret == null ) throw new InvalidJwtToken (); 
		final JwtCredential cred = new JwtCredential ( token, fJwtSecret );
		if ( !cred.forAudience ( fJwtIssuer ) ) throw new InvalidJwtToken ();
		return cred;
	}

	@Override
	public void invalidateJwtToken ( String token ) throws IamSvcException
	{
		fClerk.invalidateJwtToken ( token );
	}

	@Override
	public CommonJsonIdentity authenticate ( JwtCredential jwt )
		throws IamSvcException
	{
		return fClerk.authenticate ( jwt );
	}

	@Override
	public CommonJsonIdentity authenticate ( ApiKeyCredential akc )
		throws IamSvcException
	{
		return fClerk.authenticate ( akc );
	}

	@Override
	public CommonJsonGroup createGroup ( String groupDesc )
		throws IamSvcException
	{
		return fClerk.createGroup ( groupDesc );
	}

	@Override
	public CommonJsonGroup createGroup ( String groupId, String groupDesc )
		throws IamGroupExists,
			IamSvcException
	{
		return fClerk.createGroup ( groupId, groupDesc );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		fClerk.addUserToGroup ( groupId, userId );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist, IamGroupDoesNotExist
	{
		fClerk.removeUserFromGroup ( groupId, userId );
	}

	@Override
	public Set<String> getUsersGroups ( String userId )
		throws IamSvcException,
			IamIdentityDoesNotExist
	{
		return fClerk.getUsersGroups ( userId );
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist
	{
		return fClerk.getUsersInGroup ( groupId );
	}

	@Override
	public CommonJsonGroup loadGroup ( String id )
		throws IamSvcException
	{
		return fClerk.loadGroup ( id );
	}

	@Override
	public AccessControlList getAclFor ( Resource resource )
		throws IamSvcException
	{
		return fClerk.getAclFor ( resource );
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation )
		throws IamSvcException
	{
		return fClerk.canUser ( id, resource, operation );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration,
		TimeUnit durationTimeUnit, String nonce )
		throws IamSvcException
	{
		return fClerk.createTag ( userId, appTagType, duration, durationTimeUnit, nonce );
	}

	@Override
	public String getUserIdForTag ( String tag )
		throws IamSvcException
	{
		return fClerk.getUserIdForTag ( tag );
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType )
		throws IamSvcException
	{
		fClerk.removeMatchingTag ( userId, appTagType );
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		fClerk.sweepExpiredTags ();
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		return fClerk.userOrAliasExists ( userIdOrAlias );
	}

	@Override
	public CommonJsonIdentity loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		return fClerk.loadUserOrAlias ( userIdOrAlias );
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException
	{
		fClerk.addAlias ( userId, alias );
	}

	@Override
	public void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException
	{
		fClerk.removeAlias ( alias );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		return fClerk.getAliasesFor ( userId );
	}

	private final S3IamDb fClerk;

	private final String fJwtIssuer;
	private final String fJwtSecret;

	private static final String kData_BaseModelPath = "modelPath";

	private static final Logger log = LoggerFactory.getLogger ( ClerkAcctSvc.class );
}
