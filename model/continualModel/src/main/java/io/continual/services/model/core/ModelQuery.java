package io.continual.services.model.core;

import java.util.Comparator;

import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

/**
 * Build a query on the model
 */
public interface ModelQuery
{
	/**
	 * Optionally limit the search to objects with IDs in the given path
	 * @param path a path prefix
	 * @return this query
	 */
	ModelQuery withPathPrefix ( Path path );

	/**
	 * Limit the results to objects that return a non-empty set for the
	 * given JSON path expression. The JSON path expression is evaluated
	 * on each potential object in set, so "$" is the top-level node in
	 * each object.
	 * @param jsonPath a JSON path to match
	 * @return this query
	 */
	ModelQuery matchingJsonPath ( String jsonPath );

	/**
	 * Limit the results to objects with the given value in the given field.
	 * @param key A JSON key. Use dots to separate subobjects, brackets for arrays.
	 * @param val a field value
	 * @return this query
	 */
	ModelQuery withFieldValue ( String key, String val );

	/**
	 * Limit the results to objects with the given value in the given field.
	 * @param key A JSON key. Use dots to separate subobjects, brackets for arrays.
	 * @param val a field value
	 * @return this query
	 */
	ModelQuery withFieldValue ( String key, long val );

	/**
	 * Limit the results to objects with the given value in the given field.
	 * @param key A JSON key. Use dots to separate subobjects, brackets for arrays.
	 * @param val a field value
	 * @return this query
	 */
	ModelQuery withFieldValue ( String key, boolean val );

	/**
	 * Limit the results to objects with the given value in the given field.
	 * @param key A JSON key. Use dots to separate subobjects, brackets for arrays.
	 * @param val a field value
	 * @return this query
	 */
	ModelQuery withFieldValue ( String key, double val );

	/**
	 * Limit the results to objects with the given value contained in the given field.
	 * @param key A JSON key. Use dots to separate subobjects, brackets for arrays.
	 * @param val a field value
	 * @return this query
	 */
	ModelQuery withFieldContaining ( String key, String val );

	/**
	 * Order the result set with the given comparator
	 * @param comparator a comparator
	 * @return this query
	 */
	ModelQuery orderBy ( Comparator<ModelObject> comparator );

	/**
	 * Limit the result set to the given page size and start at page number. At most
	 * page size objects are returned. 
	 * 
	 * @param pageSize the number of objects to return (at most)
	 * @param pageNumber the 0-based page number 
	 * @return this query
	 */
	ModelQuery pageLimit ( int pageSize, int pageNumber );

	/**
	 * Limit the result set to the given page size and start at page number. At most
	 * page size objects are returned. 
	 * 
	 * @param pageSize the number of objects to return (at most)
	 * @param pageNumber the 0-based page number 
	 * @return this query
	 */
	default ModelQuery pageLimit ( PageRequest pr )
	{
		return pageLimit ( pr.getRequestedPage (), pr.getRequestedPageSize () );
	}
	
	/**
	 * Execute the query
	 * @param context a model request context
	 * @return a object list
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	default ModelObjectList<BasicModelObject> execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
	{
		return execute (
			context,
			new ModelObjectAutoFactory<BasicModelObject,Object> ( BasicModelObject.class ),
			new DataAccessor<BasicModelObject> ()
			{
				@Override
				public ModelObject getDataFrom ( BasicModelObject obj ) { return obj.getData (); }
			},
			null
		);
	}

	/**
	 * Data accessor
	 * @param <T>
	 */
	interface DataAccessor<T>
	{
		ModelObject getDataFrom ( T obj );
	}
	
	/**
	 * Execute the query
	 * @param context a model request context
	 * @param factory
	 * @param accessor
	 * @param userContext
	 * @return a object list
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	<T,K> ModelObjectList<T> execute ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException;
}
