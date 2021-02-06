package io.continual.services.model.tools;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Vector;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.core.impl.commonJsonDb.SimpleDataObject;
import io.continual.services.model.core.impl.file.FileBasedModel;
import io.continual.services.model.core.impl.s3.S3Model;
import io.continual.services.model.service.impl.s3.S3ModelRequestContext;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.console.shell.ConsoleLooper;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class ModelTools extends ConfiguredConsole
{
	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		return new ConsoleLooper.Builder ()
			.presentHeaderLine ( "ModelTools" )

			.addCommand ( new Open () )
			.addCommand ( new Get () )
			.addCommand ( new Put () )

			.build ()
		;
	}

	@Override
	protected void cleanup ()
	{
		super.cleanup ();

		closeModel ( null );
	}

	private class Open extends SimpleCommand
	{
		public Open () { super ( "open", "open <type> <args...>" ); }

		@Override
		protected void setupParser ( CmdLineParser clp )
		{
			clp.requireMinFileArguments ( 2 );
		}

		@Override
		protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) throws UsageException, MissingReqdSettingException
		{
			try
			{
				final Vector<String> args = prefs.getFileArguments ();
				final String type = args.get ( 0 );
				if ( type.trim ().equalsIgnoreCase ( "file" ) )
				{
					if ( args.size () < 4 )
					{
						outTo.println ( "For 'file' models, provide <acctId>, <modelId>, and <baseDir>." );
						return InputResult.kReady;
					}

					closeModel ( outTo );
					fModel = new FileBasedModel ( args.get ( 1 ), args.get ( 2 ), args.get ( 3 ) );
				}
				else if ( type.trim ().equalsIgnoreCase ( "s3" ) )
				{
					if ( args.size () < 5 || args.size () > 7 )
					{
						outTo.println ( "For 's3' models, provide <acctId>, <modelId>, <awsKey>, <awsSecret>, <bucketId> and optionally <prefix>." );
						return InputResult.kReady;
					}

					closeModel ( outTo );
					final String prefix = args.size () > 6 ? args.get ( 5 ) : "";
					fModel = new ZookeeperModel ( args.get ( 1 ), args.get ( 2 ), args.get ( 3 ), args.get ( 4 ), args.get ( 5 ), prefix );
				}
				else
				{
					outTo.println ( "Unrecognized model type: " + type + "." );
				}
			}
//			catch ( JSONException e )
//			{
//				return InputResult.kQuit;
//			}
			catch ( BuildFailure e )
			{
				outTo.println ( "There was a problem opening the model: " + e.getMessage () );
			}

			return InputResult.kReady;
		}
	}

	private class Get extends SimpleCommand
	{
		public Get () { super ( "get", "get <key>" ); }

		@Override
		protected void setupParser ( CmdLineParser clp )
		{
			clp.requireMinFileArguments ( 1 );
		}

		@Override
		protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) throws UsageException, MissingReqdSettingException
		{
			Path path = Path.fromString ( "/" );	// placeholder 
			try
			{
				if ( fModel == null )
				{
					outTo.println ( "Open a model first." );
					return InputResult.kReady;
				}

				final Vector<String> args = prefs.getFileArguments ();
				path = Path.fromString ( args.get ( 0 ) );

				final ModelRequestContext mrc = new S3ModelRequestContext ( (Identity) null );
				final ModelObject result = fModel.load ( mrc, path );

				outTo.println ( result.asJson () );
			}
			catch ( ModelItemDoesNotExistException x )
			{
				outTo.println ( path.toString () + " does not exist" );
			}
			catch ( ModelServiceIoException | ModelServiceRequestException e )
			{
				outTo.println ( e.getMessage () );
			}

			return InputResult.kReady;
		}
	}

	private class Put extends SimpleCommand
	{
		public Put () { super ( "put", "put <key> <value>" ); }

		@Override
		protected void setupParser ( CmdLineParser clp )
		{
			clp.requireMinFileArguments ( 1 );
		}

		@Override
		protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) throws UsageException, MissingReqdSettingException
		{
			Path path = Path.fromString ( "/" );	// placeholder 
			try
			{
				if ( fModel == null )
				{
					outTo.println ( "Open a model first." );
					return InputResult.kReady;
				}

				final Vector<String> args = prefs.getFileArguments ();
				path = Path.fromString ( args.get ( 0 ) );

				final ModelRequestContext mrc = new S3ModelRequestContext ( (Identity) null );
				final ModelObject data = new SimpleDataObject ( "", "{test:123}" );
				fModel.store ( mrc, path, data );

				outTo.println ( "ok" );
			}
			catch ( ModelItemDoesNotExistException x )
			{
				outTo.println ( path.toString () + " does not exist" );
			}
			catch ( ModelServiceIoException | ModelServiceRequestException e )
			{
				outTo.println ( e.getMessage () );
			}

			return InputResult.kReady;
		}
	}

	private Model fModel;

	private void closeModel ( PrintStream outTo )
	{
		if ( fModel != null ) 
		{
			if ( outTo != null ) { outTo.println ( "Closing open model..." ); }
			fModel.requestFinish ();
			fModel = null;
		}
	}
	
	private ModelTools ()
	{
	}

	public static void main ( String[] args ) throws UsageException, LoadException, MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		new ModelTools ()
			.runFromMain ( args )
		;
	}
}
