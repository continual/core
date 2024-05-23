package io.continual.services.model.core;

import java.util.Set;

import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

/**
 * Build a traversal across the model.
 */
public interface ModelTraversal
{
	/**
	 * Start at a given model node.
	 * @param p the path of the starting point node
	 * @return this traversal
	 */
	ModelTraversal startAt ( Path p );

	/**
	 * Start at a given model node.
	 * @param paths 
	 * @return this traversal
	 */
	ModelTraversal startWith ( Set<Path> paths );

	/**
	 * Traverse the given outbound relationship
	 * @param relation
	 * @return this traversal
	 */
	ModelTraversal traverseOutbound ( String relation );

	/**
	 * Traverse the given inbound relationship
	 * @param relation
	 * @return this traversal
	 */
	ModelTraversal traverseInbound ( String relation );

	/**
	 * Label the current set of objects for exclusion later.
	 * @param label
	 * @return this traversal
	 */
	ModelTraversal labelSet ( String label );

	/**
	 * Remove objects from the named set from the current set.
	 * @param label
	 * @return this traversal
	 */
	ModelTraversal excludeSet ( String label );

	/**
	 * Filter the current set of objects. 
	 * @param filter
	 * @return this traversal
	 */
	ModelTraversal filterSet ( ModelItemFilter<ModelObject> filter );

	/**
	 * Execute the traversal
	 * @param context a model request context
	 * @return a path list
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	ModelPathList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException;
}
