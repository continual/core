package io.continual.util.checks;

public class Strings
{
	public static void throwIfEmpty ( String s, String msg )
	{
		if ( s == null || s.length () == 0 )
		{
			throw new IllegalArgumentException ( msg );
		}
	}
}
