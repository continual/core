package io.continual.services.model.core;

public interface ModelItemFilter<T>
{
	boolean matches ( T obj );
}
