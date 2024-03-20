package io.continual.services.model.core.data;

import java.util.Set;
import java.util.TreeSet;

import io.continual.services.model.core.ModelObjectAutoFactory;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

public interface ModelObjectRelnReader
{
	/**
	 * Get a set of remote side object paths.
	 * @return a set of 0 or more paths
	 * @throws ModelRequestException
	 * @throws ModelServiceException 
	 */
	Set<Path> getRemoteObjects () throws ModelRequestException, ModelServiceException;

	/**
	 * Get a set of remote side objects
	 * @param <T>
	 * @param clazz
	 * @return a set of 0 or more objects
	 * @throws ModelRequestException
	 * @throws ModelServiceException 
	 */
	default <T,K> Set<T> getRemoteObjects ( Class<T> clazz ) throws ModelRequestException, ModelServiceException
	{
		return getRemoteObjects ( new ModelObjectAutoFactory<T,K> ( clazz ) );
	}

	/**
	 * Get a set of remote side objects
	 * @param <T>
	 * @param factory
	 * @return a set of 0 or more objects
	 * @throws ModelRequestException
	 * @throws ModelServiceException 
	 */
	<T,K> Set<T> getRemoteObjects ( ModelObjectFactory<T,K> factory ) throws ModelRequestException, ModelServiceException;

	/**
	 * Return a generic empty accessor
	 * @return an empty accessor
	 */
	static ModelObjectRelnReader emptyRelnAccessor ()
	{
		return new ModelObjectRelnReader ()
		{
			@Override
			public Set<Path> getRemoteObjects () { return new TreeSet<> (); }

			@Override
			public <T,K> Set<T> getRemoteObjects ( ModelObjectFactory<T,K> factory ) { return new TreeSet<> (); }
		};
	}

	/**
	 * Get the size of the related object set
	 * @return the size of the related object set
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	default int size () throws ModelRequestException, ModelServiceException
	{
		return getRemoteObjects().size ();
	}
}
