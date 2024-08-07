package io.continual.services.model.impl.common;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import io.continual.services.model.core.ModelItemFilter;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelObject;
import io.continual.util.data.json.JsonPathEval;
import io.continual.util.naming.Path;

public abstract class SimpleModelQuery implements ModelQuery
{
	@Override
	public ModelQuery withPathPrefix ( Path path )
	{
		fPathPrefix = path;
		return this;
	}

	@Override
	public ModelQuery orderBy ( Comparator<ModelObject> comparator )
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
				return JsonPathEval.evaluateJsonPath ( JsonModelObject.modelObjectToJson ( mo ), jsonPath ).size() > 0;
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
				final String objVal = ModelObjectExprSource.evalToString ( mo, key );
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
				return val == ModelObjectExprSource.evalToLong ( mo, key, 0L );
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
				return val == ModelObjectExprSource.evalToBoolean ( mo, key );
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
				return val == ModelObjectExprSource.evalToDouble ( mo, key, 0.0 );
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

				final String objVal = ModelObjectExprSource.evalToString ( mo, key );
				return objVal != null && objVal.length () > 0 && objVal.contains ( val );
			}
		} );
		return this;
	}

	protected Path getPathPrefix () { return fPathPrefix; }
	protected int getPageSize () { return fPageSize; }
	protected int getPageNumber () { return fPageNumber; }
	protected List<Filter> getFilters () { return fFilters; }
	protected Comparator<ModelObject> getOrdering() { return fOrderBy; }

	Path fPathPrefix = Path.getRootPath ();
	Comparator<ModelObject> fOrderBy = null;
	int fPageSize = Integer.MAX_VALUE;
	int fPageNumber = 0;
	final LinkedList<Filter> fFilters = new LinkedList<> ();

	protected static interface Filter extends ModelItemFilter<ModelObject>
	{
	}
}
