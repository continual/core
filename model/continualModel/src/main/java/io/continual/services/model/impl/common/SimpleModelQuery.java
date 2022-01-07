package io.continual.services.model.impl.common;

import java.util.LinkedList;
import java.util.List;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonPathEval;
import io.continual.util.naming.Path;

public abstract class SimpleModelQuery implements ModelQuery
{
	protected static interface Filter
	{
		boolean matches ( ModelObject mo );
	}

	protected Path getPathPrefix () { return fPathPrefix; }
	protected int getPageSize () { return fPageSize; }
	protected int getPageNumber () { return fPageNumber; }
	protected List<Filter> getFilters () { return fFilters; }
	protected ModelObjectComparator getOrdering() { return fOrderBy; }

	@Override
	public ModelQuery withPathPrefix ( Path path )
	{
		fPathPrefix = path;
		return this;
	}

	@Override
	public ModelQuery orderBy ( ModelObjectComparator comparator )
	{
		fOrderBy = comparator;
		return this;
	}

	@Override
	public ModelQuery pageLimit ( int pageSize, int pageNumber )
	{
		fPageSize = Math.max ( 1, pageSize );
		fPageNumber = Math.max ( 0, pageNumber );
		return this;
	}

	@Override
	public ModelQuery matchingJsonPath ( String jsonPath )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				return JsonPathEval.evaluateJsonPath ( mo.getData (), jsonPath ).size() > 0;
			}
		} );
		return this;
	}

	@Override
	public ModelQuery withFieldValue ( String key, String val )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				final String objVal = JsonEval.evalToString ( mo.getData (), key );
				return (
					( val == null && objVal == null ) ||
					( val != null && objVal != null && val.equals ( objVal ) )
				);
			}
		} );
		return this;
	}

	@Override
	public ModelQuery withFieldValue ( String key, long val )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				return val == JsonEval.evalToLong ( mo.getData (), key, 0L );
			}
		} );
		return this;
	}

	@Override
	public ModelQuery withFieldValue ( String key, boolean val )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				return val == JsonEval.evalToBoolean ( mo.getData (), key );
			}
		} );
		return this;
	}

	@Override
	public ModelQuery withFieldValue ( String key, double val )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				return val == JsonEval.evalToDouble ( mo.getData (), key, 0.0 );
			}
		} );
		return this;
	}

	@Override
	public ModelQuery withFieldContaining ( String key, String val )
	{
		fFilters.add ( new Filter ()
		{
			@Override
			public boolean matches ( ModelObject mo )
			{
				if ( val == null ) return false;
				if ( val.length () == 0 ) return true;

				final String objVal = JsonEval.evalToString ( mo.getData (), key );
				return objVal != null && objVal.length () > 0 && objVal.contains ( val );
			}
		} );
		return this;
	}

	@Override
	public abstract ModelObjectList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException;

	Path fPathPrefix = Path.getRootPath ();
	ModelObjectComparator fOrderBy = null;
	int fPageSize = Integer.MAX_VALUE;
	int fPageNumber = 0;
	final LinkedList<Filter> fFilters = new LinkedList<> ();
}
