package io.continual.services.processor.library.model.common.fetchers;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.model.common.ObjectFetcher;
import io.continual.util.naming.Path;

public class SingleObjectQuery extends BaseFetcher implements ObjectFetcher
{
	public SingleObjectQuery ( ServiceContainer sc, JSONObject config )
	{
		fPath = Path.fromString ( sc.getExprEval ().evaluateText ( config.getString ( "path" ) ) );
		fReturned = false;
	}

	@Override
	public boolean isEof ()
	{
		return fReturned;
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, Model model, long waitAtMost, TimeUnit waitAtMostTimeUnits, String pipeline ) throws ModelRequestException, ModelServiceException
	{
		if ( !fReturned )
		{
			fReturned = true;
			try
			{
				final ModelRequestContext mrc = model.getRequestContextBuilder ()
					.forUser ( spc.getOperator () )
					.build ()
				;
				final ModelObject mo = model.load ( mrc, fPath );
				return buildMessageAndRouting ( fPath, mo, pipeline );
			}
			catch ( ModelItemDoesNotExistException e )
			{
				spc.warn ( "Object " + fPath.toString () + " does not exist." );
			}
			catch ( BuildFailure | ModelServiceException | ModelRequestException e )
			{
				spc.fail ( e.getMessage () );
			}
		}
		return null;
	}

	private final Path fPath;
	private boolean fReturned;
}
