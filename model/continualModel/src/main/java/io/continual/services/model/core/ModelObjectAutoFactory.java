package io.continual.services.model.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.continual.services.model.core.exceptions.ModelRequestException;

/**
 * A convenience class that locates a compliant constructor in the given class and uses it
 * to build model objects. The constructor must take a ModelObjectFactory.ObjectCreateContext argument.
 * 
 * @param <T>
 */
public class ModelObjectAutoFactory<T,K> implements ModelObjectFactory<T,K>
{
	public ModelObjectAutoFactory ( Class<T> clazz ) throws ModelRequestException
	{
		try
		{
			fConstructor = clazz.getConstructor ( ModelObjectFactory.ObjectCreateContext.class );
		}
		catch ( NoSuchMethodException | SecurityException e )
		{
			throw new ModelRequestException ( "ModelObjectAutoFactory couldn't locate a constructor for " +
				clazz.getCanonicalName () +
				" with param " + ObjectCreateContext.class.getCanonicalName (), e );
		}
	}

	@Override
	public T create ( ModelObjectFactory.ObjectCreateContext<K> data ) throws ModelRequestException
	{
		try
		{
			return fConstructor.newInstance ( data );
		}
		catch ( IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	private final Constructor<T> fConstructor;
}
