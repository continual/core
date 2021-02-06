package io.continual.basesvcs.services.naming.std;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

import io.continual.basesvcs.services.naming.NameService;
import io.continual.basesvcs.services.naming.NamingIoException;
import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.services.SimpleServiceWithCli;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.naming.Path;

public abstract class BaseNameService extends SimpleServiceWithCli implements NameService
{
	protected BaseNameService ()
	{
		// lookup
		super.register ( new SimpleCommand ( "lookup", "lookup <path>", "Look up a path in the name service." )
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
					final Path nodeId = Path.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final StorageInode inode = lookup ( nodeId );
					outTo.println ( inode == null ? "(not found)" : inode.toString () );
				}
				catch ( NamingIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				
				return null;
			}
		} );

		// getChildren
		super.register ( new SimpleCommand ( "getChildren", "getChildren <path>", "List children of a path." )
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
					final Path nodeId = Path.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final Set<Path> children = getChildren ( nodeId );
					for ( Path child : children )
					{
						outTo.println ( child.getItemName().toString () );
					}
				}
				catch ( NamingIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				
				return null;
			}
		} );

		// store
		super.register ( new SimpleCommand ( "store", "store <path> <storageInode>", "Store an inode from storage to a path." )
		{
			@Override
			protected void setupParser ( CmdLineParser clp )
			{
				clp.requireFileArguments ( 2 );
			}
			
			@Override
			protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
			{
				try
				{
					final Path nodeId = Path.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final StorageInode inode = StorageInode.fromString ( p.getFileArguments ().elementAt ( 1 ) );

					store ( nodeId, inode );
					outTo.println ( "ok" );
				}
				catch ( NamingIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				
				return null;
			}
		} );

		// remove
		super.register ( new SimpleCommand ( "remove", "remove <path>",
			"Remove the entry for <path> from the naming service. Use caution; this can orphan storage." )
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
					final Path nodeId = Path.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					remove ( nodeId );
					outTo.println ( "ok" );
				}
				catch ( NamingIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				
				return null;
			}
		} );
	}
}
