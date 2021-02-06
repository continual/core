package io.continual.basesvcs.services.storage.std;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.basesvcs.services.storage.StorageIoException;
import io.continual.basesvcs.services.storage.StorageService;
import io.continual.basesvcs.services.storage.StorageSourceStream;
import io.continual.basesvcs.services.storage.util.StringDataSourceStream;
import io.continual.services.SimpleServiceWithCli;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.data.StreamTools;

public abstract class BaseStorageService extends SimpleServiceWithCli implements StorageService
{
	protected BaseStorageService ()
	{
		// load
		super.register ( new SimpleCommand ( "load", "load <inode>", "Load data for a node." )
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
					final StorageInode inode = StorageInode.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final StorageSourceStream sss = load ( inode );
					if ( sss != null )
					{
						workspace.put ( "lastStorageLoad", sss );
						outTo.println ( "ok" );
					}
					else
					{
						outTo.println ( "not found" );
					}
				}
				catch ( StorageIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// dump
		super.register ( new SimpleCommand ( "dump", "dump <inode>", "Dump data for a node to the console." )
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
					final StorageInode inode = StorageInode.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final StorageSourceStream sss = load ( inode );
					if ( sss != null )
					{
						StreamTools.copyStream ( sss.read (), outTo, 4096, false );
						outTo.println ();
					}
					else
					{
						outTo.println ( "not found" );
					}
				}
				catch ( StorageIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				catch ( IOException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// remove
		super.register ( new SimpleCommand ( "remove", "remove <inode>", "Remove an inode from storage. This can cause dangling name references in the name service!" )
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
					final StorageInode inode = StorageInode.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					remove ( inode );
					outTo.println ( "ok" );
				}
				catch ( StorageIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// copy
		super.register ( new SimpleCommand ( "copy", "copy <inode>", "Copy an inode to a new independent inode." )
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
					final StorageInode inode = StorageInode.fromString ( p.getFileArguments ().elementAt ( 0 ) );
					final StorageInode newNode = copy ( inode );
					outTo.println ( newNode.toString () );
				}
				catch ( StorageIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );

		// create
		super.register ( new SimpleCommand ( "create", "create <data>", "Create a new node with the given data." )
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
					final StorageInode newNode = create ( new StringDataSourceStream ( p.getFileArguments ().elementAt ( 0 ) ) );
					outTo.println ( newNode.toString () );
				}
				catch ( StorageIoException e )
				{
					outTo.println ( e.getMessage () );
				}
				return null;
			}
		} );
	}
}
