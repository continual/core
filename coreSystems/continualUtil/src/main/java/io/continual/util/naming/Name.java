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
package io.continual.util.naming;

/**
 * A name is a component of a path. It may not contain a path separator.
 */
public class Name implements Comparable<Name>
{
	/**
	 * Return a name from a string. 
	 * @param name as string
	 * @return a name
	 * @throws IllegalArgumentException if the name contains a path separator
	 */
	public static Name fromString ( String name )
	{
		return new Name ( name );
	}

	/**
	 * Check if the name matches the given expression.
	 * @param nameExpression a regular expression
	 * @return true if the name matches the given regex
	 */
	public boolean matches ( String nameExpression )
	{
		return fName.matches ( nameExpression );
	}

	/**
	 * Compare the given name to this name.
	 */
	@Override
	public int compareTo ( Name o )
	{
		return fName.compareTo ( o.fName );
	}

	@Override
	public String toString ()
	{
		return fName;
	}

	Name ( String name )
	{
		if ( name.indexOf ( '/' ) > -1 )
		{
			throw new IllegalArgumentException ( "Name string [" + name + "] contains a path separator." );
		}
		fName = name;
	}

	private final String fName;
}
