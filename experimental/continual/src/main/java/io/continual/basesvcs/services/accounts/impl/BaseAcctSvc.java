package io.continual.basesvcs.services.accounts.impl;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

import io.continual.basesvcs.services.accounts.AccountItemDoesNotExistException;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.services.SimpleServiceWithCli;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.naming.Path;

public abstract class BaseAcctSvc<I extends Identity,G extends Group> extends SimpleServiceWithCli
	implements AccountService<I,G>
{
	protected BaseAcctSvc ()
	{
		super ();

		// exists
		super.register ( new SimpleCommand ( "exists", "exists <userId>", "Check if a user exists." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp ) { clp.requireFileArguments ( 1 ); }

			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String userId = p.getFileArguments ().elementAt ( 0 );
					outTo.println ( userExists ( userId ) );
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException | IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// load
		super.register ( new SimpleCommand ( "loadUser", "loadUser <userId>",
			"Load a user record from the accounts service." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp )
			{
				clp.requireFileArguments ( 1 );
			}
			
			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String userId = p.getFileArguments ().elementAt ( 0 );
					final I ii = loadUser ( userId );
					if ( ii == null )
					{
						outTo.println ( "not found" );
					}
					else
					{
						outTo.println ( dump ( ii ) );
					}
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException e )
				{
					outTo.println ( e.getMessage () );
				}
				catch ( IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// find
		super.register ( new SimpleCommand ( "find", "find <userIdPrefix>", "Find matching users." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp ) { clp.requireFileArguments ( 1 ); }

			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String prefix = p.getFileArguments ().elementAt ( 0 );
					outTo.println ( findUsers ( prefix ) );
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException | IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// create
		super.register ( new SimpleCommand ( "createUser", "createUser <userId>", "Create a user." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp ) { clp.requireFileArguments ( 1 ); }

			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String userId = p.getFileArguments ().elementAt ( 0 );
					final Identity ii = createUser ( userId );
					outTo.println ( ii.getId () );
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException | IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				catch ( IamIdentityExists e )
				{
					outTo.println ( "User exists: " + e.getMessage () );
				}
				return null;
			}
		} );

		// setdata
		super.register ( new SimpleCommand ( "setUserData", "setUserData <userId> <field> <value>", "Set a data field for a user." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp ) { clp.requireFileArguments ( 3 ); }

			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String userId = p.getFileArguments ().elementAt ( 0 );
					final String field = p.getFileArguments ().elementAt ( 1 );
					final String data = p.getFileArguments ().elementAt ( 2 );
					final Identity ii = loadUser ( userId );
					ii.putUserData ( field, data );
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException | IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// acctPath
		super.register ( new SimpleCommand ( "acctPath", "acctBasePath <userId>",
			"Get the path for the given user account in the name service." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp )
			{
				clp.requireFileArguments ( 1 );
			}
			
			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final String userId = p.getFileArguments ().elementAt ( 0 );
					final Identity ii = loadUser ( userId );
					if ( ii == null )
					{
						outTo.println ( "not found" );
						return InputResult.kReady;
					}

					final Path path = getAccountBasePath ( ii );
					outTo.println ( path.toString () );
					return InputResult.kReady;
				}
				catch ( IllegalArgumentException e )
				{
					outTo.println ( e.getMessage () );
				}
				catch ( IamSvcException e )
				{
					outTo.println ( e.getMessage () );
				}
				catch ( AccountItemDoesNotExistException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );
	}

	private String dump ( I ii ) throws IamSvcException
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( ii.getId () )
			.append ( "\n" )
			.append ( "groups:\n" )
		;
		for ( String groupId : ii.getGroupIds () )
		{
			sb
				.append ( "\t")
				.append ( groupId )
				.append ( "\n" );
		}
		sb.append ( "data:\n" );
		for ( Entry<String, String> data : ii.getAllUserData ().entrySet () )
		{
			sb
				.append ( "\t")
				.append ( data.getKey () )
				.append ( ": " )
				.append ( data.getValue () )
				.append ( "\n" );
		}
		sb.append ( "\n" );
		return sb.toString ();
	}
}
