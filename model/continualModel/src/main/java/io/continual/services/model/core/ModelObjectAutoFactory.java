package io.continual.services.model.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.continual.services.model.core.data.ModelDataObjectAccess;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.util.naming.Path;

/**
 * A convenience class that locates a compliant constructor in the given class and uses it
 * to build model objects.
 * 
 * @param <T>
 */
public class ModelObjectAutoFactory<T> implements ModelObjectFactory<T>
{
	public ModelObjectAutoFactory ( Class<T> clazz ) throws ModelRequestException
	{
		try
		{
			fConstructor = clazz.getConstructor ( Path.class, ModelObjectMetadata.class, ModelDataObjectAccess.class );
		}
		catch ( NoSuchMethodException | SecurityException e )
		{
			throw new ModelRequestException ( e );
		}
	}

	@Override
	public T create ( Path path, ModelObjectMetadata metadata, ModelDataObjectAccess data ) throws ModelRequestException
	{
		try
		{
			return fConstructor.newInstance ( path, metadata, data );
		}
		catch ( IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	private final Constructor<T> fConstructor;
}
