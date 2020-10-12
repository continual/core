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

import java.io.File;
import java.util.ArrayList;

/**
 * A path identifies an item in a model. A path starts with the model's
 * unique ID.
 */
public class Path implements Comparable<Path>
{
	/**
	 * Construct a path from a string. The path string must be absolute -- that is,
	 * it must start with a path separator.
	 * @param path
	 * @return a Path
	 */
	public static Path fromString ( String path )
	{
		return new Path ( path );
	}

	/**
	 * Get the root path ( "/" )
	 * @return the root path
	 */
	public static Path getRootPath ()
	{
		return Path.fromString ( "/" );
	}

	/**
	 * Get the unique name for this path as a resource
	 */
	public String getId ()
	{
		return toString ();
	}

	@Override
	public int compareTo ( Path o )
	{
		return this.toString ().compareTo ( o.toString () );
	}

	/**
	 * Is this path equivalent to the root path?
	 * @return true if this is the root path
	 */
	public boolean isRootPath ()
	{
		return getRootPath().compareTo ( this ) == 0;
	}
	
	/**
	 * Get the parent path of this path. If this is the top-level container, getParentPath() returns null
	 * @return the parent path, or null
	 */
	public Path getParentPath ()
	{
		final File parent = fFile.getParentFile ();
		return parent == null ? null : new Path ( parent.getAbsolutePath () );
	}

	/**
	 * Get the name of the item within its parent container.
	 * @return the name in this container
	 */
	public Name getItemName ()
	{
		return Name.fromString ( fFile.getName () );
	}

	/**
	 * Does this full path start with the given text?
	 * @param text
	 * @return true if the path starts with the given text
	 */
	public boolean startsWith ( String text )
	{
		return toString().startsWith ( text );
	}

	/**
	 * Is this path contained by the given path?
	 * @param parentPath
	 * @return true if this path is equal to or contained by the given path
	 */
	public boolean startsWith ( Path parentPath )
	{
		if ( parentPath.equals ( this ) ) return true;

		final Path parent = getParentPath ();
		return parent != null && parent.startsWith ( parentPath );
	}

	/**
	 * Make a child item's path given this item and a child name.
	 * @param name
	 * @return the child's path
	 */
	public Path makeChildItem ( Name name )
	{
		return new Path ( this, name );
	}

	/**
	 * Make the given path a path below the current path.
	 * @param childPath
	 * @return a new path
	 */
	public Path makeChildPath ( Path childPath )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( toString () )
			.append ( childPath.toString () )
		;
		return Path.fromString ( sb.toString () );
	}

	/**
	 * Break a path into its name segments. /foo/bar = [ "foo", "bar" ] and
	 * the root path (/) = [].
	 * @return an array of names
	 */
	public Name[] getSegments ()
	{
		final ArrayList<Name> result = new ArrayList<> ();

		File f = fFile;
		while ( !f.toString ().equals ( "/" ) )
		{
			final String name = f.getName ();
			result.add ( 0, Name.fromString ( name ) );
			f = f.getParentFile ();
		}

		return result.toArray ( new Name[result.size ()] );
	}
	
	/**
	 * Get the number of name segments in this path. "/foo" = 1, "/foo/bar" = 2
	 * @return the number of name segments in this path
	 */
	public int depth ()
	{
		final Path parent = getParentPath ();
		if ( parent == null ) return 1;
		return 1 + parent.depth ();
	}

	/**
	 * Get the string representation of this path
	 */
	@Override
	public String toString ()
	{
		return fFile.toString ();
	}

	@Override
	public int hashCode ()
	{
		return fFile.toString().hashCode ();
	}

	@Override
	public boolean equals ( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass () != obj.getClass () )
			return false;
		Path other = (Path) obj;

		if ( fFile == null && other.fFile != null ) return false;
		if ( fFile != null && other.fFile == null ) return false;

		return fFile.getAbsolutePath ().equals ( other.fFile.getAbsolutePath() );
	}

	private Path ( String path )
	{
		fFile = new File ( path );
		if ( !fFile.isAbsolute () )
		{
			throw new IllegalArgumentException ( path + " is not absolute." );
		}
	}

	private Path ( Path path, Name child )
	{
		fFile = new File ( path.toString (), child.toString () );
		if ( !fFile.isAbsolute () )
		{
			throw new IllegalArgumentException ( path + " is not absolute." );
		}
	}

	private final File fFile;
}
