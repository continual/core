package io.continual.services.model.impl.common;

import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.util.data.TypeConvertor;
import io.continual.util.naming.Path;

public class BasicModelRelnInstance implements ModelRelationInstance
{
	public static BasicModelRelnInstance fromId ( String id )
	{
		final String[] parts = id.split ( ":" );
		if ( parts.length != 3 )
		{
			throw new IllegalArgumentException ( "Not a valid model relation ID." );
		}

		final Path from = Path.fromString ( new String ( TypeConvertor.base64Decode ( parts[0] ) ) );
		final String name = new String ( TypeConvertor.base64Decode ( parts[1] ) );
		final Path to = Path.fromString ( new String ( TypeConvertor.base64Decode ( parts[2] ) ) );

		return new BasicModelRelnInstance ( from, name, to );
	}

	public BasicModelRelnInstance ( ModelRelation mr )
	{
		this ( mr.getFrom (), mr.getName (), mr.getTo () );
	}

	public BasicModelRelnInstance ( Path from, String name, Path to )
	{
		fFrom = from;
		fName = name;
		fTo = to;
		fId = makeId ( from, name, to ); 
	}

	@Override
	public Path getFrom () { return fFrom; }

	@Override
	public Path getTo () { return fTo; }

	@Override
	public String getName () { return fName; }

	@Override
	public String getId () { return fId; }

	private final String fId;
	private final Path fFrom;
	private final String fName;
	private final Path fTo;

	private static String makeId ( Path from, String name, Path to )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( TypeConvertor.base64Encode ( from.toString () ) );
		sb.append ( ":" );
		sb.append ( TypeConvertor.base64Encode ( name ) );
		sb.append ( ":" );
		sb.append ( TypeConvertor.base64Encode ( to.toString () ) );
		return sb.toString ();
	}
}
