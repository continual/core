package io.continual.restHttp;

public class AuthListSingleton
{
	@SuppressWarnings("rawtypes")
	public synchronized static AuthList getAuthList ()
	{
		if ( fSingleton == null )
		{
			fSingleton = new AuthList ();
		}
		return fSingleton;
	}

	public synchronized static void initAuthList ( AuthList<?> al )
	{
		if ( fSingleton != null )
		{
			throw new IllegalStateException ( "AuthList already constructed." );
		}
		fSingleton = al;
	}

	private static AuthList<?> fSingleton;
}
