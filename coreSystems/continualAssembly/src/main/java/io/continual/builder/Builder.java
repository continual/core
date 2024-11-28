/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.builder;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.prefs.Preferences;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.builder.sources.BuilderPrefsDataSource;
import io.continual.builder.sources.BuilderReadableDataSource;
import io.continual.builder.sources.BuilderStringDataSource;
import io.continual.util.nv.NvReadable;

public class Builder<T>
{
	public static class BuildFailure extends Exception
	{
		public BuildFailure ( Throwable t ) { super ( t ); }
		public BuildFailure ( String msg ) { super ( msg ); }
		public BuildFailure ( String msg, Throwable t ) { super ( msg, t ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Construct a build for a given base class.
	 * @param base
	 */
	private Builder ( Class<T> base )
	{
		fBase = base;
		fClassName = null;
		fClassNameInData = true;
		fClassLoader = null;
		fData = null;
		fContext = null;
		fContextClass = null;
	}

	/**
	 * Typical build from JSON data. This call uses no context object or search path.
	 * 
	 * @param base
	 * @param data
	 * @return an instance of the base class
	 * @throws BuildFailure
	 */
	public static <T> T fromJson ( Class<T> base, JSONObject data ) throws BuildFailure
	{
		return withBaseClass ( base )
			.withClassNameInData ()
			.usingData ( new BuilderJsonDataSource ( data ) )
			.build ();
	}

	/**
	 * Typical build from JSON data with a context. No search path is used.
	 * 
	 * @param base
	 * @param data
	 * @param context
	 * @return an instance of the base class
	 * @throws BuildFailure
	 */
	public static <T> T fromJson ( Class<T> base, JSONObject data, Object context ) throws BuildFailure
	{
		return withBaseClass ( base )
			.withClassNameInData ()
			.usingData ( new BuilderJsonDataSource ( data ) )
			.providingContext ( context )
			.build ();
	}

	/**
	 * construct a builder
	 * @param base
	 * @return
	 */
	public static <T> Builder<T> withBaseClass ( Class<T> base )
	{
		return new Builder<T> ( base );
	}

	/**
	 * Set the class to use
	 * @param classname
	 * @return this builder
	 */
	public Builder<T> usingClassName ( String classname )
	{
		fClassName = classname;
		fClassNameInData = false;
		return this;
	}

	/**
	 * Pull the classname from the data object
	 * @return this builder
	 */
	public Builder<T> withClassNameInData ()
	{
		fClassName = null;
		fClassNameInData = true;
		return this;
	}

	/**
	 * Specify a Java ClassLoader to use to load the requested class.
	 * @param loader
	 * @return this builder
	 */
	public Builder<T> usingClassLoader ( ClassLoader loader )
	{
		fClassLoader = loader;
		return this;
	}

	/**
	 * Construct the object using the given data source
	 * @param dataSource
	 * @return this builder
	 */
	public Builder<T> usingData ( BuilderDataSource dataSource )
	{
		fData = dataSource;
		return this;
	}

	/**
	 * Convenience method equivalent to "usingData ( new BuilderStringDataSource ( data ) )"
	 * @param data
	 * @return this builder
	 */
	public Builder<T> fromString ( String data )
	{
		return usingData ( new BuilderStringDataSource ( data ) );
	}

	/**
	 * Convenience method equivalent to "usingData ( new BuilderSettingsDataSource ( data ) )"
	 * @param data
	 * @return this builder
	 */
	public Builder<T> usingData ( Preferences data )
	{
		return usingData ( new BuilderPrefsDataSource ( data ) );
	}

	/**
	 * Convenience method equivalent to "usingData ( new BuilderSettingsDataSource ( data ) )"
	 * @param data
	 * @return this builder
	 */
	public Builder<T> usingData ( NvReadable data )
	{
		return usingData ( new BuilderReadableDataSource ( data ) );
	}

	/**
	 * Convenience method equivalent to "usingData ( new BuilderJsonDataSource ( data ) )"
	 * @param data
	 * @return this builder
	 */
	public Builder<T> usingData ( JSONObject data )
	{
		return usingData ( new BuilderJsonDataSource ( data ) );
	}

	/**
	 * Convenience method equivalent to "usingData ( new BuilderJsonDataSource ( data ) )"
	 * @param data
	 * @return this builder
	 */
	public Builder<T> readingJsonData ( InputStream data )
	{
		return usingData ( new BuilderJsonDataSource ( data ) );
	}

	/**
	 * If provided, the context object is passed to the constructor of the target
	 * class as its first argument OR passed to a fromJson/fromSettings/fromString class
	 * as the last argument, provided one of these can be found.
	 * 
	 * @param context
	 * @return the builder
	 */
	public Builder<T> providingContext ( Object context )
	{
		fContext = context;
		fContextClass = context.getClass ();
		return this;
	}

	/**
	 * Allow the caller to load fully qualified classnames.
	 * @return this
	 */
	public Builder<T> allowFullClassnames ()
	{
		fRestrictSearchToPath = false;
		return this;
	}
	
	/**
	 * Do not allow the caller to load fully qualified classnames. In this case, you
	 * must provide search path(s) under which the classes are found.
	 * @return this
	 */
	public Builder<T> restrictFullClassnames ()
	{
		fRestrictSearchToPath = true;
		return this;
	}

	/**
	 * If provided, the target class name will be sought first as a simple 
	 * string, then appended to the package name provided here. This call
	 * can be made multiple times to establish a list.
	 * @param packageName
	 * @return the builder
	 */
	public Builder<T> searchingPath ( String packageName )
	{
		if ( fSearchPath == null )
		{
			fSearchPath = new LinkedList<String> ();
		}
		fSearchPath.add ( packageName );

		return this;
	}
	
	/**
	 * Add a number of package names in a single call. Iterates the collection
	 * calling searchingPath for each entry.
	 * @param packageNames
	 * @return the builder
	 */
	public Builder<T> searchingPaths ( Collection<String> packageNames )
	{
		for ( String pkgName : packageNames )
		{
			searchingPath ( pkgName );
		}
		return this;
	}

	/**
	 * Build an instance from a classname and the given data. The instance is
	 * initialized in one these ways, in the following order:<br>
	 * <br>
	 * FIXME this list needs an update
	 * calling a constructor with the settings object as a single parameter<br>
	 * calling a static method 'fromSettings'/'fromJson' that takes a settings/JSONObject instance and returns and instance<br>
	 * calling a default constructor, then calling 'fromSettings' with the settings instance<br>
	 * <br>
	 * Note that the class is found prior to attempting initialization. If the search path
	 * finds packageA.Foo ahead of packageB.Foo, only packageA.Foo is considered, even if
	 * packageA.Foo can't be initialized but packageB.Foo might have been.
	 * 
	 * @return an instance
	 * @throws BuildFailure
	 */
	public T build () throws BuildFailure
	{
		try
		{
			// which class?
			String cn = null;
			if ( fClassNameInData && fData != null )
			{
				cn = fData.getClassNameFromData ();
			}
			else if ( !fClassNameInData && fClassName != null )
			{
				cn = fClassName;
			}
			if ( cn == null )
			{
				throw new BuildFailure ( "No class name provided." );
			}

			if ( fData != null )
			{
				return build ( cn );
			}
			else
			{
				return findClass ( cn ).getDeclaredConstructor().newInstance ();
			}
		}
		catch ( ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException e )
		{
			throw new BuildFailure ( e );
		}
	}

	private Class<? extends T> findClass ( String className ) throws ClassNotFoundException
	{
		// possibly try the full class name
		if ( !fRestrictSearchToPath )
		{
			try
			{
				log.trace ( "Builder looking for " + className + " as " + fBase.getName () );
				return classForName ( className ).asSubclass ( fBase );
			}
			catch ( java.lang.ClassCastException x )
			{
				log.warn ( "{} does not implement {}.", className, fBase.getName () );
				throw x;
			}
			catch ( ClassNotFoundException x1 )
			{
				// if the class wasn't found as-is, and we don't have a search path, throw
				if ( fSearchPath == null )
				{
					log.trace ( "Didn't find " + className + ". No additional search path. Restricted to path: " + fRestrictSearchToPath );
					throw x1;
				}
			}
		}

		// here, we either are restricted to search path or the name isn't a good (full) classname.
		for ( String path : fSearchPath )
		{
			final StringBuilder sb = new StringBuilder ();
			sb.append ( path );
			if ( !path.endsWith(".") ) sb.append ( '.' );
			sb.append ( className );
			final String newClassName = sb.toString ();

			log.trace ( "Builder looking for " + newClassName + " as " + fBase.getName () );

			try
			{
				return classForName ( newClassName ).asSubclass ( fBase );
			}
			catch ( java.lang.ClassCastException x )
			{
				log.warn ( "{} does not implement {}.", className, fBase.getName () );
			}
			catch ( ClassNotFoundException x2 )
			{
				// ignore
				log.trace ( "Didn't find " + newClassName + " (or it's not a " + fBase.getName () + ")." );
			}
		}

		// still not found, bail out
		log.trace ( "Didn't find " + className + ", even after using search path." );
		throw new ClassNotFoundException ( className );
	}

	@SuppressWarnings("unchecked")
	private <D> T build ( String className ) throws BuildFailure
	{
		try
		{
			// find the target class
			Class<? extends T> c = findClass ( className );

			// get the name of the init method on the target class
			final String initerName = fData.getIniterName ();

			// get the class that the init method expects as a data source
			final Class<?> initerDataClass = fData.getIniterClass ();
			
			// try a static init method that'll take the context class and the data class
			for ( Method m : c.getMethods () )
			{
				// if this method is named properly and returns an instance we can use...
				if ( m.getName ().equals ( initerName ) && fBase.isAssignableFrom ( m.getReturnType () ) )
				{
					final boolean isStatic = Modifier.isStatic ( m.getModifiers () );

					// if it just has the data class as arg, try that
					final Class<?>[] params = m.getParameterTypes ();
					if ( params.length == 1 && params[0].isAssignableFrom ( initerDataClass ) )
					{
						if ( isStatic )
						{
							return (T) m.invoke ( null, fData.getInitData () );
						}
						else
						{
							T t = c.getDeclaredConstructor().newInstance ();
							m.invoke ( t, fData.getInitData () );
							return t;
						}
					}
					else if ( params.length == 2 &&
						params[0].isAssignableFrom ( initerDataClass ) &&
						fContextClass != null &&
						params[1].isAssignableFrom ( fContextClass )
					)
					{
						if ( isStatic )
						{
							return (T) m.invoke ( null, fData.getInitData (), fContext );
						}
						else
						{
							T t = c.getDeclaredConstructor().newInstance ();
							m.invoke ( t, fData.getInitData (), fContext );
							return t;
						}
					}
				}
			}

			// next try a constructor with the data and context...
			if ( fContext != null )
			{
				Class<?> contextClassToTry = fContextClass;
				while ( contextClassToTry != null )
				{
					try
					{
						final Constructor<? extends T> cc = c.getConstructor ( contextClassToTry, initerDataClass );
						return cc.newInstance ( fContext, fData.getInitData () );
					}
					catch ( NoSuchMethodException e )
					{
						// move on
						contextClassToTry = contextClassToTry.getSuperclass ();
					}
				}

				// no acceptable base class found; try interfaces
				for ( Class<?> iface : fContextClass.getInterfaces () )
				{
					try
					{
						final Constructor<? extends T> cc = c.getConstructor ( iface, initerDataClass );
						return cc.newInstance ( fContext, fData.getInitData () );
					}
					catch ( NoSuchMethodException e )
					{
						// go to next interface
					}
				}
			}

			// next try a constructor with just the data
			try
			{
				final Constructor<? extends T> cc = c.getConstructor ( initerDataClass );
				return cc.newInstance ( fData.getInitData () );
			}
			catch ( NoSuchMethodException e )
			{
				// move on
			}

			// finally, we'll take a no-arg constructor
			try
			{
				final Constructor<? extends T> cc = c.getConstructor ();
				return cc.newInstance ();
			}
			catch ( NoSuchMethodException e )
			{
				// move on
			}

			// out of options
			throw new BuildFailure ( "Could not find a suitable constructor/creator for class [" + className + "]" );
		}
		catch ( IllegalArgumentException e )
		{
			throw new BuildFailure ( e );
		}
		catch ( InvocationTargetException e )
		{
			final Throwable target = e.getTargetException ();
			if ( target instanceof BuildFailure )
			{
				final BuildFailure f = (BuildFailure) target;
				throw f;
			}
			if ( target == null )
			{
				throw new BuildFailure ( e );
			}
			throw new BuildFailure ( target );
		}
		catch ( ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | SecurityException e )
		{
			throw new BuildFailure ( e );
		}
	}

	private Class<?> classForName ( String className ) throws ClassNotFoundException
	{
		if ( fClassLoader != null )
		{
			return Class.forName ( className, true, fClassLoader );
		}
		return Class.forName ( className );
	}

	private final Class<T> fBase;
	private String fClassName;
	private boolean fClassNameInData;
	private ClassLoader fClassLoader;
	private BuilderDataSource fData;
	private Object fContext;
	private Class<? extends Object> fContextClass;

	private boolean fRestrictSearchToPath = false;
	private LinkedList<String> fSearchPath;

	private static final Logger log = LoggerFactory.getLogger ( Builder.class );
}
