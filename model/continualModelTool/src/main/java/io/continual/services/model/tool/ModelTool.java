package io.continual.services.model.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.iam.IamService;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.json.CommonJsonDbObject;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.console.shell.ConsoleLooper;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class ModelTool extends ConfiguredConsole
{
	private ModelTool ()
	{
		fModels = new HashMap<> ();
	}

	@Override
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		return this;
	}

	private class CopyCommand extends SimpleCommand
	{
		protected CopyCommand ()
		{
			super ( "copy", "copy <fromMode>:<fromPath> <toModel>:<toPath>" );
		}

		@Override
		protected void setupParser ( CmdLineParser clp )
		{
			super.setupParser ( clp );

			clp.requireFileArguments ( 2 );
		}

		private String[] split ( String s )
		{
			final int colon = s.indexOf ( ':' );
			if ( colon < 0 ) throw new IllegalArgumentException ( s + " is not in format model:path" );

			return new String[]
			{
				s.substring ( 0, colon ),
				s.substring ( colon + 1 )
			};
		}
		
		@Override
		protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) throws UsageException, MissingReqdSettingException
		{
			try
			{
				final String from = prefs.getFileArguments ().get ( 0 );
				final String to = prefs.getFileArguments ().get ( 1 );

				final String[] fromParts = split ( from );
				final String[] toParts = split ( to );

				final ModelInfo src = fModels.get ( fromParts[0] );
				if ( src == null ) throw new IllegalArgumentException ( "No such model " + fromParts[0] );
				final ModelInfo tgt = fModels.get ( toParts[0] );
				if ( tgt == null ) throw new IllegalArgumentException ( "No such model " + toParts[0] );

				final ModelRequestContext srcMrc = src.fModel.getRequestContextBuilder ()
					.forUser ( src.fIdentity )
					.build ()
				;

				final ModelRequestContext tgtMrc = tgt.fModel.getRequestContextBuilder ()
					.forUser ( tgt.fIdentity )
					.build ()
				;

				final TreeSet<ModelRelation> relnsToMake = new TreeSet<> ();

				final Path srcRoot = Path.fromString ( fromParts[1] );
				final Path tgtRoot = Path.fromString ( toParts[1] );

				copyTree ( src, srcMrc, tgt, tgtMrc, srcRoot, tgtRoot, srcRoot, tgtRoot, relnsToMake, outTo );

				for ( ModelRelation mr : relnsToMake )
				{
					if ( tgt.fModel.exists ( tgtMrc, mr.getFrom () ) && tgt.fModel.exists ( tgtMrc, mr.getTo () ) )
					{
						tgt.fModel.relate ( tgtMrc, mr );
					}
				}
			}
			catch ( BuildFailure | ModelServiceException | ModelRequestException x )
			{
				outTo.println ( x.getMessage () );
			}
			return null;
		}

		private Path translatePath ( Path input, Path baseSrc, Path baseTgt )
		{
			final Path childPart = input.makePathWithinParent ( baseSrc );
			return baseTgt.makeChildPath ( childPart );
		}

		private void copyTree ( ModelInfo src, ModelRequestContext srcMrc, ModelInfo tgt, ModelRequestContext tgtMrc, Path baseSrc, Path baseTgt, Path srcPath, Path tgtPath, Set<ModelRelation> mrs, PrintStream outTo ) throws ModelItemDoesNotExistException, ModelSchemaViolationException, ModelServiceException, ModelRequestException
		{
			for ( Path srcObj : src.fModel.listChildrenOfPath ( srcMrc, srcPath ) )
			{
				outTo.println ( srcObj.toString () );

				final Path tgtChild = translatePath ( srcPath, baseSrc, baseTgt );

				final ModelObject mo = src.fModel.load ( srcMrc, srcObj );
				if ( !mo.getMetadata ().getLockedTypes ().contains ( CommonJsonDbObject.kStdType_ObjectContainer ) )
				{
					tgt.fModel.store ( tgtMrc, tgtChild, mo.getData () );

					for ( ModelRelation mr : src.fModel.selectRelations ( srcObj ).getRelations ( srcMrc ) )
					{
						final Path mrFr = mr.getFrom ();
						final Path mrTo = mr.getTo ();

						// if this relation is contained within the subgraph we're copying...
						if ( mrFr.startsWith ( baseSrc ) && mrTo.startsWith ( baseSrc ) )
						{
							final Path from = translatePath ( mrFr, baseSrc, baseTgt );
							final Path to = translatePath ( mrTo, baseSrc, baseTgt );
							mrs.add ( ModelRelation.from ( from, mr.getName(), to ) );
						}
						// else: not in our subgraph
					}
				}
				copyTree ( src, srcMrc, tgt, tgtMrc, baseSrc, baseTgt, srcObj, tgtChild, mrs, outTo );
			}
		}
	};

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		final String configFile = clp.getString ( kConfigFile );

		final ServiceContainer sc = new ServiceContainer ();

		try ( InputStream fis = new FileInputStream ( new File ( configFile ) ) )
		{
			final JSONObject config = new JSONObject ( new CommentedJsonTokener ( fis ) );

			@SuppressWarnings("unchecked")
			final IamService<Identity,Group> accounts = Builder.withBaseClass ( IamService.class )
				.withClassNameInData ()
				.usingData ( new BuilderJsonDataSource ( config.getJSONObject ( "accounts" ) ) )
				.providingContext ( sc )
				.build ()
			;

			JsonVisitor.forEachElement ( config.optJSONObject ( "models" ), new ObjectVisitor<JSONObject,BuildFailure> ()
			{
				@Override
				public boolean visit ( String modelName, JSONObject modelInitData ) throws JSONException, BuildFailure
				{
					try
					{
						final Model model =  Builder.withBaseClass ( Model.class )
							.withClassNameInData ()
							.usingData ( new BuilderJsonDataSource ( modelInitData ) )
							.providingContext ( sc )
							.build ()
						;

						final Identity identity = accounts.getIdentityDb ().authenticate ( new UsernamePasswordCredential (
							modelInitData.getString ( "username" ),
							modelInitData.getString ( "password" )
						) );
						if ( identity == null )
						{
							throw new BuildFailure ( modelName + " authentication failed." );
						}

						fModels.put ( modelName, new ModelInfo ( model, identity ) );

						return true;
					}
					catch ( IamSvcException x )
					{
						throw new BuildFailure ( x );
					}
				}
			} );
		}
		catch ( JSONException | IOException | BuildFailure e )
		{
			throw new StartupFailureException ( e );
		}

		return new ConsoleLooper.Builder ()
			.addCommand ( new CopyCommand () )
			.build ()
		;
	}

	public static void main ( String[] args )
	{
		try
		{
			new ModelTool().runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			System.err.println ( e.getMessage () );
		}
	}

	private class ModelInfo
	{
		public ModelInfo ( Model m, Identity i )
		{
			fModel = m;
			fIdentity = i;
		}

		public final Model fModel;
		public final Identity fIdentity;
	}
	
	final HashMap<String,ModelInfo> fModels;
}
