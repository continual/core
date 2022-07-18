package io.continual.services.processor.library.model.sinks;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsCatalog.PathPopper;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.AclUpdate;
import io.continual.services.model.core.updaters.DataOverwrite;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.library.model.common.ModelConnector;
import io.continual.util.naming.Path;

public class ModelSink extends ModelConnector implements Sink
{
	public ModelSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( modelFromConfig ( sc, config.getJSONObject ( "model" ) ), sc, config );
	}

	public ModelSink ( Model model, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( model );
	}

	@Override
	public synchronized void init ()
	{
	}

	@Override
	public synchronized void close ()
	{
	}

	@Override
	public synchronized void flush ()
	{
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		final MetricsCatalog mc = context.getStreamProcessingContext ().getMetrics ();
		try ( PathPopper pp = mc.push ( "ModelSink" ))
		{
			final Message msg = context.getMessage ();

			// we look for the same fields populated by the modelSource - id, metadata, data
			final Path path = Path.fromString ( msg.getValueAsString ( "id" ) );

			final Model model = getModel ();
			final ModelRequestContext mrc = model.getRequestContextBuilder ()
				.forUser ( context.getStreamProcessingContext ().getOperator () )
				.build ()
			;

			// copy the ACL if provided
			AccessControlList newAcl = null;
			final JSONObject meta = msg.accessRawJson ().optJSONObject ( "metadata" );
			if ( meta != null )
			{
				newAcl = AccessControlList.deserialize ( meta.optJSONObject ( "acl" ), null );
			}

			model.store ( mrc, path,
				new AclUpdate ( newAcl ),
				new DataOverwrite ( msg.accessRawJson ().getJSONObject ( "data" ) )
			);
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			context.warn ( "Couldn't store object: " + e.getMessage () );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( ModelSink.class );
}
