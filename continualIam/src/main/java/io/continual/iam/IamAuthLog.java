package io.continual.iam;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IamAuthLog
{
	public static void authenticationEvent ( String username, String method, String location )
	{
		info ( "AUTH: {}", new JSONObject()
			.put("username", username )
			.put("method", method )
			.put("location", location )
			.toString()
		);
	}
	
	public static void debug ( String msg )
	{
		getLogger().debug ( msg );
	}

	public static void debug ( String msg, Throwable t )
	{
		getLogger().debug ( msg, t );
	}

	public static void debug ( String fmt, Object... parts )
	{
		getLogger().debug ( fmt, parts );
	}

	public static void info ( String msg )
	{
		getLogger().info ( msg );
	}

	public static void info ( String msg, Throwable t )
	{
		getLogger().info ( msg, t );
	}

	public static void info ( String fmt, Object... parts )
	{
		getLogger().info ( fmt, parts );
	}


	public static Logger getLogger ()
	{
		return sfLog;
	}

	private static Logger sfLog = LoggerFactory.getLogger ( "io.continual.iam.authEvents" );
}
