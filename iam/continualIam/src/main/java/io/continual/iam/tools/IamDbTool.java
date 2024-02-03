package io.continual.iam.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Vector;

import org.json.JSONException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.console.shell.ConsoleLooper;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.console.shell.StdCommandList;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.time.Clock;

public abstract class IamDbTool<I extends Identity, G extends Group> extends ConsoleProgram
{
	public IamDbTool ()
	{
		fDb = null;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs cmdLine ) throws NvReadable.MissingReqdSettingException, NvReadable.InvalidSettingValueException, StartupFailureException
	{
		return new ConsoleLooper (
			new String[] { "iam db tool" },
			"> ",
			". ",
			new commandSet ()
		);
	}

	private IamDb<I,G> fDb;

	protected abstract IamDb<I,G> createDb ( Vector<String> args, PrintStream outTo ) throws IamSvcException, BuildFailure;

	private class commandSet extends StdCommandList 
	{
		public commandSet ()
		{
			registerCommand ( new IamDbCmd ( "connect", false )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( fDb != null ) fDb.close ();

					try
					{
						fDb = createDb ( args, outTo );
						workspace.put ( "iamDb", fDb );
					}
					catch ( IamSvcException | BuildFailure e )
					{
						outTo.println ( "Problem connecting to IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "createUser", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: createUser <userId>" );
						return;
					}

					try
					{
						fDb.createUser ( args.elementAt(0) );
						outTo.println ( "ok." );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamIdentityExists e )
					{
						outTo.println ( "The identity already exists." );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "enableUser", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: enableUser <userId>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt(0) );
						if ( i != null )
						{
							i.enable ( true );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "no such user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "disableUser", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: disableUser <userId>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt(0) );
						if ( i != null )
						{
							i.enable ( false );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "no such user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "createGroup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () < 1 || args.size () > 2 )
					{
						outTo.println ( "usage: createGroup [<groupId>] <groupName>" );
						return;
					}

					try
					{
						if ( args.size () == 1 )
						{
							final Group g = fDb.createGroup ( args.elementAt(0) );
							outTo.println ( "Group " + g.getId () + " created." );
						}
						else
						{
							final Group g = fDb.createGroup ( args.elementAt (0), args.elementAt(1) );
							outTo.println ( "Group " + g.getId () + " created." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamGroupExists e )
					{
						outTo.println ( "Group exists: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "addToGroup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 2 )
					{
						outTo.println ( "usage: addToGroup <userId> <groupId>" );
						return;
					}

					try
					{
						fDb.addUserToGroup ( args.elementAt(1), args.elementAt(0) );
						outTo.println ( "ok." );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamIdentityDoesNotExist | IamGroupDoesNotExist e )
					{
						outTo.println ( e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "removeFromGroup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 2 )
					{
						outTo.println ( "usage: removeFromGroup <userId> <groupId>" );
						return;
					}

					try
					{
						fDb.removeUserFromGroup ( args.elementAt(1), args.elementAt(0) );
						outTo.println ( "ok." );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamIdentityDoesNotExist | IamGroupDoesNotExist e )
					{
						outTo.println ( e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "listGroups", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: listGroups <userId>" );
						return;
					}

					try
					{
						for ( String user : fDb.getUsersGroups ( args.elementAt ( 0 ) ) )
						{
							outTo.println ( user );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamIdentityDoesNotExist e )
					{
						outTo.println ( e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "listUsers", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 0 )
					{
						outTo.println ( "usage: listUsers" );
						return;
					}

					try
					{
						for ( String user : fDb.getAllUsers () )
						{
							outTo.println ( user );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "findUsers", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: findUsers <startingWith>" );
						return;
					}

					try
					{
						for ( String user : fDb.findUsers ( args.elementAt ( 0 ) ) )
						{
							outTo.println ( user );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "setData", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 3 )
					{
						outTo.println ( "usage: setData <userId> <name> <value>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							i.putUserData ( args.elementAt ( 1 ), args.elementAt ( 2 ) );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "setGroupData", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 3 )
					{
						outTo.println ( "usage: setGroupData <groupId> <name> <value>" );
						return;
					}

					try
					{
						final Group i = fDb.loadGroup ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							i.putUserData ( args.elementAt ( 1 ), args.elementAt ( 2 ) );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "Couldn't find group." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "clearData", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 2 )
					{
						outTo.println ( "usage: clearData <userId> <name>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							i.removeUserData ( args.elementAt ( 1 ) );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "setPassword", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 2 )
					{
						outTo.println ( "usage: setPassword <userId> <password>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							i.setPassword ( args.elementAt ( 1 ) );
							outTo.println ( "ok." );
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "createApiKey", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: createApiKey <userId>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							final ApiKey key = i.createApiKey ();
							outTo.println ( "   key: " + key.getKey () );
							outTo.println ( "secret: " + key.getSecret () );
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "restoreApiKey", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					final long nowMs = Clock.now ();

					if ( args.size () != 3 )
					{
						outTo.println ( "usage: restoreApiKey <userId> <apiKey> <apiSecret>" );
						return;
					}

					try
					{
						fDb.restoreApiKey ( new ApiKey ()
						{
							@Override
							public String getUserId () { return args.elementAt ( 0 ); }

							@Override
							public String getKey () { return args.elementAt ( 1 ); }

							@Override
							public String getSecret () { return args.elementAt ( 2 ); }

							@Override
							public long getCreationTimestamp () { return nowMs; }
						} );
					}
					catch ( IamSvcException | IamBadRequestException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "getUser", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: getUser <userId>" );
						return;
					}

					try
					{
						final Identity i = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							outTo.println ( "Enabled: " + i.isEnabled () );
							outTo.println ();

							outTo.println ( "API Keys" );
							for ( String apiKey : i.loadApiKeysForUser () )
							{
								outTo.println ( "\t" + apiKey );
							}
							outTo.println ();
							
							outTo.println ( "Data" );
							final Map<String,String> data = i.getAllUserData ();
							for ( Entry<String,String> e : data.entrySet () )
							{
								outTo.println ( "\t" + e.getKey() + ": " + e.getValue () );
							}
							outTo.println ();

							outTo.println ( "Groups" );
							for ( String group : i.getGroupIds () )
							{
								outTo.println ( "\t" + group );
							}
							outTo.println ();
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "getGroup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: getGroup <groupId>" );
						return;
					}

					try
					{
						final Group i = fDb.loadGroup ( args.elementAt ( 0 ) );
						if ( i != null )
						{
							outTo.println ( "Data" );
							final Map<String,String> data = i.getAllUserData ();
							for ( Entry<String,String> e : data.entrySet () )
							{
								outTo.println ( "\t" + e.getKey() + ": " + e.getValue () );
							}

							outTo.println ( "Users" );
							for ( String group : i.getMembers () )
							{
								outTo.println ( "\t" + group );
							}
						}
						else
						{
							outTo.println ( "Couldn't find user." );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "listAcl", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: listAcl <resourceId>" );
						return;
					}

					try
					{
						final String resName = args.elementAt ( 0 );
						final AccessControlList acl = fDb.getAclFor ( new Resource ()
							{
								@Override public String getId () { return resName; }
							}
						);

						if ( acl == null )
						{
							outTo.println ( "No ACL for " + resName );
						}
						else
						{
							outTo.println ( acl.asJson ().toString ( 4 ) );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "grant", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 3 )
					{
						outTo.println ( "usage: grant <resource> <userOrGroupId> <operation>" );
						return;
					}

					final String resource = args.elementAt ( 0 );
					final String userOrGroup = args.elementAt ( 1 );
					final String access = args.elementAt ( 2 );

					try
					{
						final AccessControlList acl = fDb.getAclFor ( new Resource ()
						{
							@Override
							public String getId ()
							{
								return resource;
							}
						} );
						
						final Identity i = fDb.loadUser ( userOrGroup );
						if ( i != null )
						{
							acl.permit ( userOrGroup, access );
						}
						else
						{
							final Group g = fDb.loadGroup ( userOrGroup );
							if ( g != null )
							{
								acl.permit ( userOrGroup, access );
							}
							else
							{
								outTo.println ( "No user or group named '" + userOrGroup + "' was found." );
							}
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "revoke", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 3 )
					{
						outTo.println ( "usage: revoke <resource> <userOrGroupId> <operation>" );
						return;
					}

					final String resource = args.elementAt ( 0 );
					final String userOrGroup = args.elementAt ( 1 );
					final String access = args.elementAt ( 2 );

					try
					{
						final AccessControlList acl = fDb.getAclFor ( new Resource ()
						{
							@Override
							public String getId ()
							{
								return resource;
							}
						} );
						
						final Identity i = fDb.loadUser ( userOrGroup );
						if ( i != null )
						{
							acl.clear ( userOrGroup, access );
						}
						else
						{
							final Group g = fDb.loadGroup ( userOrGroup );
							if ( g != null )
							{
								acl.clear ( userOrGroup, access );
							}
							else
							{
								outTo.println ( "No user or group named '" + userOrGroup + "' was found." );
							}
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "sweepExpiredTags", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 0 )
					{
						outTo.println ( "usage: sweepExpiredKeys" );
						return;
					}

					try
					{
						fDb.sweepExpiredTags ();
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "report", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 0 )
					{
						outTo.println ( "usage: report" );
						return;
					}

					try
					{
						final Map<String,? extends I> users = fDb.loadAllUsers ();

						final LinkedList<String> userList = new LinkedList<String> ();
						userList.addAll ( users.keySet () );
						Collections.sort ( userList );

						outTo.println ( "userId,acctId,enabled" );

						for ( String userId : userList )
						{
							final I user = users.get ( userId );
							if ( user == null )
							{
								outTo.println ( "WARN: " + userId + " has null user record" );
								continue;
							}
							final String groupId = user.getUserData ( "acctId" );

							final StringBuilder sb = new StringBuilder ();
							sb
								.append ( userId )
								.append ( "," )
								.append ( groupId )
								.append ( "," )
								.append ( user.isEnabled () )
							;
							outTo.println ( sb.toString () );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "canUser", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, final Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 3 )
					{
						outTo.println ( "usage: <user> <resource> <op>" );
						return;
					}

					try
					{
						final boolean response = fDb.canUser ( 
							args.elementAt ( 0 ),
							new Resource ()
							{
								@Override
								public String getId ()
								{
									return args.elementAt ( 1 );
								}
							},
							args.elementAt ( 2 )
						);
						outTo.println ( response );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "listGroup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, final Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: listGroup <groupId>" );
						return;
					}

					try
					{
						for ( String userId : fDb.getUsersInGroup ( args.elementAt ( 0 ) ) )
						{
							outTo.println ( userId );
						}
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
					catch ( IamGroupDoesNotExist e )
					{
						outTo.println ( "Group does not exist: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "addAlias", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 2 )
					{
						outTo.println ( "usage: addAlias <userId> <alias>" );
						return;
					}

					try
					{
						fDb.addAlias ( args.elementAt ( 0 ), args.elementAt ( 1 ) );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Service problem: " + e.getMessage () );
					}
					catch ( IamBadRequestException e )
					{
						outTo.println ( "Request problem: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "removeAlias", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: removeAlias <alias>" );
						return;
					}

					try
					{
						fDb.removeAlias ( args.elementAt ( 0 ) );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Service problem: " + e.getMessage () );
					}
					catch ( IamBadRequestException e )
					{
						outTo.println ( "Request problem: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "backup", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: backup <toFile>" );
						return;
					}

					try ( FileOutputStream fos = new FileOutputStream ( new File ( args.elementAt ( 0 ) ) ) )
					{
						new IamDbBackup<I,G> ( fDb ).backupTo ( fos );
					}
					catch ( IOException e )
					{
						outTo.println ( "Couldn't write file: " + e.getMessage () );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "restore", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () != 1 )
					{
						outTo.println ( "usage: restore <fromFile>" );
						return;
					}

					try ( FileInputStream fis = new FileInputStream ( new File ( args.elementAt ( 0 ) ) ) )
					{
						new IamDbBackup<I,G> ( fDb ).restoreFrom ( fis );
					}
					catch ( JSONException | IOException e )
					{
						outTo.println ( "Couldn't read file: " + e.getMessage () );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );

			registerCommand ( new IamDbCmd ( "createToken", true )
			{
				@Override
				protected void execute ( IamDb<?,?> db, Vector<String> args,
					HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
						throws UsageException, NvReadable.MissingReqdSettingException
				{
					if ( args.size () < 1 || args.size () > 2 )
					{
						outTo.println ( "usage: createToken <user> [<durationHrs>]" );
						return;
					}

					final long duration = args.size () == 2 ? Long.parseLong ( args.elementAt ( 1 ) ) : 0;

					try
					{
						final I user = fDb.loadUser ( args.elementAt ( 0 ) );
						if ( user == null )
						{
							outTo.println ( "User " + args.elementAt ( 0 ) + " not found." );
							return;
						}

						final String token = fDb.createJwtToken ( user, duration, TimeUnit.HOURS );
						outTo.println ( token );
					}
					catch ( JSONException e )
					{
						outTo.println ( "Couldn't read file: " + e.getMessage () );
					}
					catch ( IamSvcException e )
					{
						outTo.println ( "Problem with IAM DB: " + e.getMessage () );
					}
				}
			} );
		}
	}

	public abstract class IamDbCmd extends SimpleCommand
	{
		protected IamDbCmd ( String name, boolean reqDb )
		{
			super ( name );
			fReqDb = reqDb;
		}

		@Override
		protected final ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
			throws UsageException, MissingReqdSettingException
		{
			final IamDb<?,?> db = getDb ( workspace );
			if ( fReqDb && db == null )
			{
				outTo.println ( "Use connect to connect to an IAM DB" );
				return ConsoleLooper.InputResult.kReady;
			}

			final Vector<String> args = p.getFileArguments ();
			execute ( db, args, workspace, p, outTo );

			return ConsoleLooper.InputResult.kReady;
		}

		protected abstract void execute ( IamDb<?,?> db, Vector<String> args,
			HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo )
				throws UsageException, MissingReqdSettingException;
		
		private final boolean fReqDb;

		private IamDb<?,?> getDb ( HashMap<String,Object> workspace )
		{
			final Object o = workspace.get ( "iamDb" );
			if ( o instanceof IamDb )
			{
				return (IamDb<?,?>) o;
			}
			return null;
		}
	}
}
