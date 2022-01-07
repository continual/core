package io.continual.services.processor.library.modelio.processors;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.DataMerge;
import io.continual.services.model.core.updaters.DataOverwrite;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.util.Setter;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.services.processor.engine.model.StreamProcessingContext.NoSuitableObjectException;
import io.continual.services.processor.library.modelio.services.ModelService;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;

public class ModelUpdate implements Processor
{
	public ModelUpdate ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fUpdates = JsonUtil.clone ( config.getJSONArray ( "updates" ) );
	}
	
	@Override
	public void process ( MessageProcessingContext context )
	{
		try
		{
			final ModelService modelSvc = context.getStreamProcessingContext ().getReqdNamedObject ( "model", ModelService.class );
			final Model model = modelSvc.getModel ();
			final ModelRequestContext mrc = model.getRequestContextBuilder ()
				.forUser ( context.getStreamProcessingContext ().getOperator () )
				.build ()
			;

			for ( int i=0; i<fUpdates.length (); i++ )
			{
				final JSONObject updateBlock = fUpdates.getJSONObject ( i );
				final String pathText = context.evalExpression ( updateBlock.getString ( "path" ) );
				final Path path = Path.fromString ( pathText );

				// now decide which operation...

				if ( updateBlock.has ( "delete" ) )
				{
					// (we don't care what the value is)
					model.remove ( mrc, path );
				}
				else if ( updateBlock.has ( "deleteFields" ) )
				{
					final JSONArray data = updateBlock.getJSONArray ( "deleteFields" );
					final JSONArray evaled = Setter.evaluate ( context, data, context.getMessage () );

					final ModelObject mo = model.load ( mrc, path );
					final JSONObject modelData = mo.getData ();

					for ( int j=0; j<evaled.length (); j++ )
					{
						final String field = evaled.getString ( j );
						final List<String> parts = JsonEval.splitPath ( field );
						final JSONObject container = JsonEval.getContainerOf ( modelData, field );
						container.remove ( parts.get ( parts.size () - 1 ) );
					}

					model.store ( mrc, path, new DataOverwrite ( modelData ) );
				}
				else if ( updateBlock.has ( "patch" ) )
				{
					final JSONObject data = updateBlock.getJSONObject ( "patch" );
					final JSONObject evaled = Setter.evaluate ( context, data, context.getMessage () );
					model.store ( mrc, path, new DataMerge ( evaled ) );
				}
				else if ( updateBlock.has ( "put" ) )
				{
					final JSONObject data = updateBlock.getJSONObject ( "put" );
					final JSONObject evaled = Setter.evaluate ( context, data, context.getMessage () );
					model.store ( mrc, path, new DataOverwrite ( evaled ) );
				}
			}
		}
		catch ( JSONException | NoSuitableObjectException | ModelRequestException | ModelServiceException | BuildFailure e )
		{
			context.warn ( e.getMessage () );
		}
	}

	private final JSONArray fUpdates;
}
