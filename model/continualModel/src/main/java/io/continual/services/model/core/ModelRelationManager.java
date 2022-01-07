package io.continual.services.model.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.util.naming.Path;

/**
 * The model relation manager tracks relationships between objects. Model implementations should normally use
 * the relation manager provided in the ModelRequestContext to ensure that relations work properly across
 * model instances.<br>
 * <br>
 * Path arguments are always scoped within the model. That is, if a model is mounted into a server at
 * path /foo, and the user wants to relate /foo/bar to /foo/baz, the relate call will see from=/bar and to=/baz.
 */
@Deprecated
public interface ModelRelationManager
{
	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param reln
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default void relate ( ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		relate ( Collections.singletonList ( reln ) );
	}

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param relns
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	void relate ( Collection<ModelRelation> relns ) throws ModelServiceException, ModelRequestException;

	/**
	 * Remove a relation between two objects.
	 * @param reln
	 * @return true if the relationship existed 
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	boolean unrelate ( ModelRelation reln ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get all related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default List<ModelRelation> getRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		result.addAll ( getInboundRelations ( forObject ) );
		result.addAll ( getOutboundRelations ( forObject ) );
		return result;
	}

	/**
	 * Get inbound related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations, with getTo set to forObject
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getInboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get outbound related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations, with getFrom set to forObject
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getOutboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get inbound related objects with a given name from a given object
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getTo set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getInboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get outbound related objects with a given name from a given object
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getFrom set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getOutboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException;
}
