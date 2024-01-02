package io.continual.services.model.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.continual.util.data.TypeConvertor;
import io.continual.util.naming.Path;

public interface ModelRelationInstance extends ModelRelation
{
	/**
	 * Get a unique ID for this relation. The ID is defined within the scope of the model that created it,
	 * but this class provides a straightforward and reliable implementation for models that don't benefit
	 * from a more complex implementation.
	 * 
	 * @return a unique ID string
	 */
	String getId ();

	/**
	 * Build a relation instance from two paths and a relation name.
	 * @param from
	 * @param reln
	 * @param to
	 * @return a model relation instance
	 */
	static ModelRelationInstance from ( final Path from, final String reln, final Path to )
	{
		return from ( ModelRelation.from ( from, reln, to ) );
	}

	/**
	 * Build a relation instance from two paths and a relation name.
	 * @param mr a model relation
	 * @return a model relation instance
	 */
	static ModelRelationInstance from ( final ModelRelation mr )
	{
		final String id = makeId ( mr );

		return new ModelRelationInstance ()
		{
			@Override
			public String getId () { return id; }

			@Override
			public Path getFrom () { return mr.getFrom (); }

			@Override
			public Path getTo () { return mr.getTo (); }

			@Override
			public String getName () { return mr.getName (); }

			@Override
			public String toString ()
			{
				return new StringBuilder ()
					.append ( getFrom () )
					.append ( " --" )
					.append ( getName () )
					.append ( "-> " )
					.append ( getTo () )
					.append ( " (" )
					.append ( getId () )
					.append ( ")" )
					.toString ()
				;
			}

			@Override
			public int hashCode () { return mr.hashCode (); }
			
			@Override
			public boolean equals ( Object that ) { return mr.equals ( that ); }

			@Override
			public int compareTo ( ModelRelation o ) { return mr.compareTo ( o ); }
		};
	}

	/**
	 * Build a relation instance from an ID created by this implementation.
	 * @param id
	 * @return a model relation
	 */
	static ModelRelationInstance from ( final String id ) throws IllegalArgumentException
	{
		try ( final ByteArrayInputStream bais = new ByteArrayInputStream ( TypeConvertor.base64Decode ( id ) ) )
		{
			final JSONObject json = new JSONObject ( new JSONTokener ( bais ) );
			return from (
				Path.fromString ( json.getString ( "from" ) ),
				json.getString ( "name" ),
				Path.fromString ( json.getString ( "to" ) )
			);
		}
		catch ( JSONException x )
		{
			throw new IllegalArgumentException ( x );
		}
		catch ( IOException x )
		{
			throw new RuntimeException ( x );
		}
	}

	/**
	 * Generate an ID given a model relation
	 * @param mr
	 * @return an ID string
	 */
	static String makeId ( ModelRelation mr )
	{
		return TypeConvertor.base64Encode ( new JSONObject ()
			.put ( "from", mr.getFrom ().toString () )
			.put ( "name", mr.getName () )
			.put ( "to", mr.getTo ().toString () )
			.toString ()
		).trim ();
	}
}
