/*
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

package io.continual.http.service.framework.routing.playish;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;

public class StaticJavaEntryAction implements CHttpPlayishRouteHandler
{
	public StaticJavaEntryAction ( String action, List<String> args, Collection<String> packages )
	{
		fAction = action;
		fArgs = args;
		fMethod = null;

		processAction ( packages );
	}

	@Override
	public String toString ()
	{
		return fAction;
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> addlArgs ) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		final Object[] methodArgs = new Object[addlArgs.size ()+1];
		methodArgs[0] = context;
		int i=1;
		for ( String arg : addlArgs )
		{
			methodArgs[i++] = arg; 
		}
		fMethod.invoke ( null, methodArgs );
	}

	@Override
	public boolean actionMatches ( String fullPath )
	{
		return fAction.equals ( fullPath );
	}

	private String fAction;
	private final List<String> fArgs;
	private Method fMethod;

	private void processAction ( Collection<String> packages )
	{
		final int lastDot = fAction.lastIndexOf ( "." );
		if ( lastDot < 0 )
		{
			throw new IllegalArgumentException ( "The action string should have at least \"class.method\"." );
		}

		final String className = fAction.substring ( 0, lastDot );
		final String methodName = fAction.substring ( lastDot + 1 );

		try
		{
			final Class<?> c = locateClass ( className, packages );
			fAction = c.getName () + "." + methodName;

			final Class<?>[] s = new Class<?>[ fArgs.size () + 1 ];
			s[0] = CHttpRequestContext.class;
			for ( int i=1; i<=fArgs.size(); i++ )
			{
				s[i] = String.class;
			}
			fMethod = c.getMethod ( methodName, s );
			if ( !Modifier.isStatic ( fMethod.getModifiers () ) )
			{
				throw new IllegalArgumentException ( methodName + " is not static." );
			}
		}
		catch ( ClassNotFoundException | SecurityException | NoSuchMethodException e )
		{
			throw new IllegalArgumentException ( e );
		}
	}

	private Class<?> locateClass ( String name, Collection<String> packages ) throws ClassNotFoundException
	{
		// try it straight...
		Class<?> result = tryClass ( name );
		if ( result == null )
		{
			// try the package list
			for ( String pkg : packages )
			{
				result = tryClass ( pkg + "." + name );
				if ( result != null ) break;
			}
		}
		if ( result == null )
		{
			throw new ClassNotFoundException ( name );
		}
		return result;
	}

	private Class<?> tryClass ( String name )
	{
		Class<?> result = null;
		try
		{
			result = Class.forName ( name );
			log.debug ( "class [" + name + "] located" );
		}
		catch ( ClassNotFoundException e )
		{
			log.debug ( "class [" + name + "] not found" );
		}
		return result;
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticJavaEntryAction.class );
}
