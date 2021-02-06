package io.continual.iam.apiserver.endpoints;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.HttpServlet;
import io.continual.util.data.json.JsonVisitor;

public class IamApiHandler extends BaseEndpoint
{
	public IamApiHandler ( IamServiceManager<?, ?> accts  )
	{
		fAccts = accts;
	}

	public void getUsers ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Collection<String> users = fAccts.getIdentityManager ().getAllUsers ();
					final JSONArray usersJson = JsonVisitor.listToArray ( users );
					sendJson ( context, new JSONObject ().put ( "users", usersJson ) );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void getUser ( CHttpRequestContext context, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Identity user = fAccts.getIdentityManager().loadUser ( userId );
					if ( user == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
					}
					else
					{
						sendJson ( context, renderUser ( user ) );
					}
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void createUser ( CHttpRequestContext context, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final JSONObject body = readBody ( context );
					final String username = body.getString ( "username" );

					final Identity user = fAccts.getIdentityManager ().createUser ( username );

					final JSONArray groups = body.optJSONArray ( "groups" );
					if ( groups != null )
					{
						for ( int i=0; i<groups.length (); i++ )
						{
							try
							{
								fAccts.getAccessManager ().addUserToGroup ( groups.getString ( i ), user.getId () );
							}
							catch ( IamIdentityDoesNotExist e )
							{
								// this shouldn't happen!
								throw new IamSvcException ( e );
							}
						}
					}

					sendJson ( context, renderUser ( user ) );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				catch ( IamIdentityExists e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k409_conflict, "User exists." );
				}
				catch ( IamGroupDoesNotExist e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Group does not exist." );
				}
				return null;
			}
		} );
	}

	public void setPassword ( CHttpRequestContext context, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Identity user = fAccts.getIdentityManager().loadUser ( userId );
					if ( user == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
					}
					else
					{
						final JSONObject body = readBody ( context );
						final String pwd = body.getString ( "password" );
						user.setPassword ( pwd );

						sendJson ( context, renderUser ( user ) );
					}
				}
				catch ( JSONException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process this request." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void setData ( CHttpRequestContext context, String userId, String dataKey ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Identity user = fAccts.getIdentityManager().loadUser ( userId );
					if ( user == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
					}
					else
					{
						final JSONObject body = readBody ( context );
						final String value = body.getString ( "value" );
						user.putUserData ( dataKey, value );

						sendJson ( context, renderUser ( user ) );
					}
				}
				catch ( JSONException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process this request." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void removeData ( CHttpRequestContext context, String userId, String dataKey ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Identity user = fAccts.getIdentityManager().loadUser ( userId );
					if ( user == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
					}
					else
					{
						user.removeUserData ( dataKey );

						sendJson ( context, renderUser ( user ) );
					}
				}
				catch ( JSONException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process this request." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void setEnabled ( CHttpRequestContext context, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Identity user = fAccts.getIdentityManager().loadUser ( userId );
					if ( user == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
					}
					else
					{
						final JSONObject body = readBody ( context );
						user.enable ( body.getBoolean ( "enabled" ) );

						sendJson ( context, renderUser ( user ) );
					}
				}
				catch ( JSONException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process this request." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void createGroup ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final JSONObject body = readBody ( context );
					final String groupName = body.getString ( "group" );
					final Group group = fAccts.getAccessManager ().createGroup ( groupName, groupName );
					sendJson ( context, renderGroup ( group ) );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				catch ( IamGroupExists e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k409_conflict, "The group exists." );
				}
				return null;
			}
		} );
	}

	public void getGroups ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Collection<String> groups = fAccts.getAccessManager ().getAllGroups ();
					final JSONArray groupsJson = JsonVisitor.listToArray ( groups );
					sendJson ( context, new JSONObject ().put ( "groups", groupsJson ) );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void getGroup ( CHttpRequestContext context, String groupId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					final Group group = fAccts.getAccessManager().loadGroup ( groupId );
					if ( group == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Group not found." );
					}
					else
					{
						sendJson ( context, renderGroup ( group ) );
					}
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void addUserToGroup ( CHttpRequestContext context, String groupId, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					fAccts.getAccessManager ().addUserToGroup ( groupId, userId );
					sendStatusOkNoContent ( context );
				}
				catch ( IamGroupDoesNotExist x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Group not found." );
				}
				catch ( IamIdentityDoesNotExist x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	public void deleteUserFromGroup ( CHttpRequestContext context, String groupId, String userId ) throws IamSvcException
	{
		handleWithContextApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext uc )  throws IOException
			{
				try
				{
					fAccts.getAccessManager ().removeUserFromGroup ( groupId, userId );
					sendStatusOkNoContent ( context );
				}
				catch ( IamGroupDoesNotExist x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Group not found." );
				}
				catch ( IamIdentityDoesNotExist x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "User not found." );
				}
				catch ( IamSvcException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the IAM service." );
				}
				return null;
			}
		} );
	}

	private final IamServiceManager<?,?> fAccts;

	private JSONObject renderUser ( Identity user ) throws IamSvcException
	{
		final Set<String> groupIds = user.getGroupIds ();
		final JSONArray groupArr = JsonVisitor.listToArray ( groupIds );

		final Collection<String> apiKeys = user.loadApiKeysForUser ();
		final JSONArray apiKeysArr = JsonVisitor.listToArray ( apiKeys );

		final Map<String,String> data = user.getAllUserData ();
		final JSONObject dataObj = JsonVisitor.mapOfStringsToObject ( data );

		return new JSONObject ()
			.put ( "id", user.getId () )
			.put ( "enabled", user.isEnabled () )
			.put ( "groups", groupArr )
			.put ( "apiKeys", apiKeysArr )
			.put ( "data", dataObj )
		;
	}

	private JSONObject renderGroup ( Group group ) throws IamSvcException
	{
		final Set<String> users = group.getMembers ();
		final JSONArray usersArr = JsonVisitor.listToArray ( users );

		final Map<String,String> data = group.getAllUserData ();
		final JSONObject dataObj = JsonVisitor.mapOfStringsToObject ( data );

		return new JSONObject ()
			.put ( "id", group.getId () )
			.put ( "name", group.getName () )
			.put ( "users", usersArr )
			.put ( "data", dataObj )
		;
	}
}
