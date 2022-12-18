package io.continual.iam;

import org.junit.Test;

public class IamAuthLogTest
{
	@Test
	public void testAuthenticationEvent ()
	{
		IamAuthLog.authenticationEvent ( "username" , "method" , "location" );
	}

	@Test
	public void testDebug ()
	{
		final String msg = "message";
		IamAuthLog.debug ( msg );
		IamAuthLog.debug ( msg , new Throwable ( msg ) );
		IamAuthLog.debug ( "Values {} was inserted between {} and {}." , 
				new Object[] { "2" , "1" , "3" } );
	}

	@Test
	public void testInfo ()
	{
		final String msg = "message";
		IamAuthLog.info ( msg );
		IamAuthLog.info ( msg , new Throwable ( msg ) );
		IamAuthLog.info ( "Values {} was inserted between {} and {}." , 
				new Object[] { "2" , "1" , "3" } );
	}
}
