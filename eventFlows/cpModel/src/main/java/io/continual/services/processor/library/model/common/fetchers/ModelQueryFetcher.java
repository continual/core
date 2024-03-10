package io.continual.services.processor.library.model.common.fetchers;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.model.common.ObjectFetcher;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.naming.Path;

public class ModelQueryFetcher extends BaseFetcher implements ObjectFetcher
{
	public ModelQueryFetcher ( ServiceContainer sc, JSONObject config )
	{
		final ExpressionEvaluator ee = sc.getExprEval ();
		
		fPathPrefix = readPath ( ee.evaluateText ( config.optString ( "pathPrefix", null ) ) );
		fJsonPath = ee.evaluateText ( config.optString ( "jsonPath", null ) );

		fPageSize = ee.evaluateTextToInt ( config.opt ( "pageSize" ), -1 );

		fResultSet = null;
	}

	@Override
	public boolean isEof ()
	{
		// we're at EOF if we have a result set that's empty, unless we are paging data

		// if we have no result set, we need to run the query
		if ( fResultSet == null ) return false;

		// if we have a result set but we're paging through data, we need to run the query
		if ( isPaging () ) return false;

		// if we have a result set but we're not paging, we're EOF if we have nothing left
		return !fResultSet.hasNext ();
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, Model model, long waitAtMost, TimeUnit waitAtMostTimeUnits, String pipeline ) throws ModelRequestException, ModelServiceException
	{
		try
		{
			if ( fResultSet == null || ( isPaging() && !fResultSet.hasNext () ) )
			{
				// build up the query
				ModelQuery q = model.startQuery ();
				if ( fPathPrefix != null )
				{
					q = q.withPathPrefix ( fPathPrefix );
				}
				if ( fJsonPath != null )
				{
					q = q.matchingJsonPath ( fJsonPath );
				}
				if ( fPageSize > -1 )
				{
					q = q.pageLimit ( fPageSize, fLastPageNumber++ );
				}

				// execute the query
				final ModelRequestContext mrc = model.getRequestContextBuilder ()
					.forUser ( spc.getOperator () )
					.build ()
				;
				fResultSet = q.execute ( mrc ).iterator ();

				// if we get no results, turn off paging
				if ( !fResultSet.hasNext () )
				{
					fPageSize = -1;
				}
			}

			// out of results?
			if ( !fResultSet.hasNext () ) return null;

			// get the next object
			final ModelObjectAndPath<BasicModelObject> mo = fResultSet.next ();
			return buildMessageAndRouting ( mo.getPath (), mo.getObject (), pipeline );
		}
		catch ( BuildFailure | ModelServiceException | ModelRequestException e )
		{
			spc.fail ( e.getMessage () );
		}
		return null;
	}

	private final Path fPathPrefix;
	private final String fJsonPath;
	private int fPageSize;
	
	private int fLastPageNumber = 0;

	private Iterator<ModelObjectAndPath<BasicModelObject>> fResultSet;

	private boolean isPaging ()
	{
		return fPageSize > -1;
	}
	
	private static Path readPath ( String s )
	{
		return s == null ? null : Path.fromString ( s );
	}
}
