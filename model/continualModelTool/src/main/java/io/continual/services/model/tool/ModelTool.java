package io.continual.services.model.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.DataMerge;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class ModelTool extends ConfiguredConsole
{
	private static final String kScript = "file";

	@Override
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kScript );

		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		final String scriptFile = clp.getString ( kScript );
		if ( scriptFile == null ) throw new MissingReqdSettingException ( kScript );

		final ServiceContainer sc = new ServiceContainer ();
		
		try ( InputStream fis = new FileInputStream ( new File ( scriptFile ) ) )
		{
			final JSONObject script = new JSONObject ( new CommentedJsonTokener ( fis ) );

			final JSONObject modelInitData = script.getJSONObject ( "model" );
			final Model model =  Builder.withBaseClass ( Model.class )
				.withClassNameInData ()
				.usingData ( new BuilderJsonDataSource ( modelInitData ) )
				.providingContext ( sc )
				.build ();

			final ModelRequestContext mrc = model.getRequestContextBuilder ()
				.forUser ( new CommonJsonIdentity ( script.optString ( "username", "admin" ), new JSONObject (), null ) )
				.build ()
			;

			JsonVisitor.forEachElement ( script.getJSONArray ( "updates" ), new ArrayVisitor<JSONObject,ModelServiceException> ()
			{
				@Override
				public boolean visit ( JSONObject update ) throws JSONException, ModelServiceException
				{
					final String pathText = update.getString ( "path" );
					final JSONObject patch = update.optJSONObject ( "patch" );
					if ( patch != null )
					{
						final Path objPath = Path.fromString ( pathText );
						try
						{
							model.store ( mrc, objPath, new DataMerge ( patch ) );
						}
						catch ( ModelRequestException | ModelServiceException e )
						{
							log.warn ( "Update failed: " + e.getMessage () );
						}
					};
					
					return true;
				}
			} );
		}
		catch ( JSONException | IOException | BuildFailure | ModelServiceException e )
		{
			throw new StartupFailureException ( e );
		}

		return null;
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

	private static final Logger log = LoggerFactory.getLogger ( ModelTool.class );
}
