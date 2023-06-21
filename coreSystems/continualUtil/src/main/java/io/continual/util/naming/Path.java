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

import java.util.ArrayList;
import java.util.List;

/**
 * A path identifies an item in a model. A path starts with the model's
 * unique ID.
 */
public class Path implements Comparable<Path>
{
	/**
	 * Construct a path from a string. The path string must be absolute -- that is,
	 * it must start with a path separator.
	 * @param path a path
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
		return Path.fromString ( kSepStr );
	}

	/**
	 * Get the unique name for this path as a resource
	 * @return the unique name for this path as a resource
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
		return fParent;
	}

	/**
	 * Get the name of the item within its parent container.
	 * @return the name in this container
	 */
	public Name getItemName ()
	{
		return fName;
	}

	/**
	 * Does this full path start with the given text?
	 * @param text the text 
	 * @return true if the path starts with the given text
	 */
	public boolean startsWith ( String text )
	{
		return toString().startsWith ( text );
	}

	/**
	 * Is this path contained by the given path?
	 * @param parentPath the parent path
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
	 * @param name the name of the child item
	 * @return the child's path
	 */
	public Path makeChildItem ( Name name )
	{
		return new Path ( this, name );
	}

	/**
	 * Make the given path a path below the current path.
	 * @param childPath the path of the child item
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
	 * Make a new path that's the path within the given parent path. For example, if this
	 * is "/foo/bar" and parentPath is "/foo", the result is "/bar".
	 * @param parentPath
	 * @return a new path
	 */
	public Path makePathWithinParent ( Path parentPath )
	{
		// special case the root path
		if ( parentPath.isRootPath () ) return this;

		// if our target is within the parent path, just return what's left after the parent is removed
		if ( !startsWith ( parentPath ) )
		{
			throw new IllegalArgumentException ( "The target path [" + toString () +
				"] is not a child of [" + parentPath.toString () +"]." );
		}
		final String substr = toString ().substring ( parentPath.toString ().length () );
		return substr.length () == 0 ? getRootPath() : Path.fromString ( substr );
	}
	
	/**
	 * Break a path into its name segments. /foo/bar = [ "foo", "bar" ] and
	 * the root path (/) = [].
	 * @return an array of names
	 */
	public Name[] getSegments ()
	{
		final List<Name> segments = getSegmentList();
		return segments.toArray ( new Name[segments.size ()] );
	}

	/**
	 * Break a path into its name segments. /foo/bar = [ "foo", "bar" ] and
	 * the root path (/) = [].
	 * @return a list of names
	 */
	public List<Name> getSegmentList ()
	{
		final List<Name> segments = fParent == null ? new ArrayList<> () : fParent.getSegmentList ();
		if ( fName != null )
		{
			segments.add ( fName );
		}
		return segments;
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
		if ( fParent == null && fName == null ) return "/";

		final String parentPart = fParent == null ? "" : fParent.toString ();

		final StringBuilder sb = new StringBuilder ();
		sb.append ( parentPart );

		if ( !parentPart.endsWith ( "/" ) )
		{
			sb.append ( "/" );
		}
		sb.append ( fName == null ? "" : fName.toString () );

		return sb.toString ();
	}

	@Override
	public int hashCode ()
	{
		return toString().hashCode ();
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
		return toString().equals ( other.toString() );
	}

	private Path ( String path )
	{
		if ( path == null || !path.startsWith ( "/" ) )
		{
			throw new IllegalArgumentException ( "Path string must be absolute." );
		}

		// clean up collapsible segments
		while ( path.contains  ( "//" ) || path.contains ( "/./" ) )
		{
			path = path
				.replaceAll ( "//", "/" )
				.replaceAll ( "/\\./", "/" )
			;
		}

		// ignore a trailing /. (which the loop above won't catch)
		while ( path.endsWith ( "/." ) )
		{
			path = path.substring ( 0, path.length () - 2 );
		}

		// root path is a special case. The path could now be empty
		if ( path.length () == 0 || path.equals ( "/" ) )
		{
			fParent = null;
			fName = null;
			return;
		}

		// split on the last slash...
		final int lastSlash = path.lastIndexOf ( '/' );
		if ( lastSlash == 0 )
		{
			fParent = getRootPath ();
			fName = buildName ( path.substring ( 1 ) );
		}
		else
		{
			final String parentPart = path.substring ( 0, lastSlash );
			final String namePart = path.substring ( lastSlash + 1 );

			fParent = new Path ( parentPart );
			fName = buildName ( namePart );
		}
	}

	private Path ( Path path, Name child )
	{
		fParent = path;
		fName = child;
	}

	private static Name buildName ( String name )
	{
		if ( name.equals ( ".." ) )
		{
			throw new IllegalArgumentException ( "Path components may not contain '..'." );
		}
		return Name.fromString ( name );
	}

	// if both are null, this is the root path
	private final Path fParent;
	private final Name fName;

	private static final char kSepChar = '/';
	private static final String kSepStr = "" + kSepChar;
}
