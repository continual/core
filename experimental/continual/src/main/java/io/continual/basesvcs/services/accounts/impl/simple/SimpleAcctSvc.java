package io.continual.basesvcs.services.accounts.impl.simple;

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
import io.continual.basesvcs.services.accounts.impl.BaseAcctSvc;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import io.continual.services.ServiceContainer;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.TypeConvertor;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.time.Clock;

public class SimpleAcctSvc extends BaseAcctSvc<CommonJsonIdentity,CommonJsonGroup>
{
	public SimpleAcctSvc ( final ServiceContainer sc, JSONObject config ) throws IamSvcException
	{
		fUsers = config.optJSONObject ( "db" );
		if ( fUsers == null ) throw new IamSvcException ( "No user map provided." );

		fDb = new JsonDocDb ( fUsers );

		fJwtSecret = config.optString ( "jwtSha256Key", null );
		fJwtIssuer = config.optString ( "jwtIssuer", null );
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
	public boolean userExists ( String userId ) throws IamSvcException
	{
		return fDb.userExists ( userId );
	}

	@Override
	public CommonJsonIdentity loadUser ( String userId ) throws IamSvcException
	{
		return fDb.loadUser ( userId );
	}

	@Override
	public List<String> findUsers ( String startingWith ) throws IamSvcException
	{
		return fDb.findUsers ( startingWith );
	}

	@Override
	public CommonJsonIdentity createUser ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public CommonJsonIdentity createAnonymousUser () throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public void deleteUser ( String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public boolean completePasswordReset ( String tagId, String newPassword ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException
	{
		return fDb.loadApiKeyRecord ( apiKey );
	}

	@Override
	public Collection<String> getAllUsers () throws IamSvcException
	{
		return fDb.getAllUsers ();
	}
	
	@Override
	public Map<String, CommonJsonIdentity> loadAllUsers () throws IamSvcException
	{
		return fDb.loadAllUsers ();
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
		return new JwtCredential ( token, fJwtSecret );
	}

	@Override
	public void invalidateJwtToken ( String token ) throws IamSvcException
	{
		fDb.invalidateJwtToken ( token );
	}

	@Override
	public CommonJsonIdentity authenticate ( JwtCredential jwt ) throws IamSvcException
	{
		return fDb.authenticate ( jwt );
	}

	@Override
	public CommonJsonIdentity authenticate ( UsernamePasswordCredential upc ) throws IamSvcException
	{
		return fDb.authenticate ( upc );
	}

	@Override
	public CommonJsonIdentity authenticate ( ApiKeyCredential akc ) throws IamSvcException
	{
		return fDb.authenticate ( akc );
	}

	@Override
	public CommonJsonGroup createGroup ( String groupDesc ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public CommonJsonGroup createGroup ( String groupId, String groupDesc ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public void addUserToGroup ( String groupId, String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		return fDb.getUsersGroups ( userId );
	}

	@Override
	public Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist
	{
		return fDb.getUsersInGroup ( groupId );
	}

	@Override
	public CommonJsonGroup loadGroup ( String id ) throws IamSvcException
	{
		return fDb.loadGroup ( id );
	}

	@Override
	public AccessControlList getAclFor ( Resource resource ) throws IamSvcException
	{
		return fDb.getAclFor ( resource );
	}

	@Override
	public boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException
	{
		return fDb.canUser ( id, resource, operation );
	}

	@Override
	public String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public String getUserIdForTag ( String tag ) throws IamSvcException
	{
		return fDb.getUserIdForTag ( tag );
	}

	@Override
	public void removeMatchingTag ( String userId, String appTagType ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public void sweepExpiredTags () throws IamSvcException
	{
		fDb.sweepExpiredTags ();
	}

	@Override
	public boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException
	{
		return fDb.userOrAliasExists ( userIdOrAlias );
	}

	@Override
	public CommonJsonIdentity loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException
	{
		return fDb.loadUserOrAlias ( userIdOrAlias );
	}

	@Override
	public void addAlias ( String userId, String alias ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public void removeAlias ( String alias ) throws IamSvcException
	{
		throw new IamSvcException ( "The auth db is static." );
	}

	@Override
	public Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist
	{
		return fDb.getAliasesFor ( userId );
	}

	private final JSONObject fUsers;
	private final JsonDocDb fDb;

	private final String fJwtIssuer;
	private final String fJwtSecret;

	private static final Logger log = LoggerFactory.getLogger ( SimpleAcctSvc.class );

	private static final String kData_BaseModelPath = "model";
}
