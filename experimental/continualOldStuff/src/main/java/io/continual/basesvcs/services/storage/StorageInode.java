package io.continual.basesvcs.services.storage;

/**
 * A mapping from name to actual storage location.
 * @author peter
 *
 */
public class StorageInode
{
	/**
	 * Create an inode entry from a service name and node ID
	 * @param svcName
	 * @param id
	 * @return an inode instance
	 */
	public static StorageInode fromName ( String svcName, String id )
	{
		return new StorageInode ( svcName, id );
	}

	/**
	 * Construct from a string written as svc:id
	 * @param s
	 * @return a storage node
	 */
	public static StorageInode fromString ( String s )
	{
		final int colon = s.indexOf ( ':' );
		if ( colon < 0 ) throw new IllegalArgumentException ( "use 'svc:id'" );

		final String svc = s.substring ( 0, colon );
		final String id = s.substring ( colon+1 );
		if ( svc.length () == 0 || id.length () == 0 )  throw new IllegalArgumentException ( "use 'svc:id'" );

		return fromName ( svc, id );
	}
	
	@Override
	public String toString ()
	{
		return fSvc + ":" + fId;
	}

	public String getServiceName () { return fSvc; }

	public String getId () { return fId; }

	/**
	 * Create a storage inode from service name and node id
	 * @param svcName
	 * @param id
	 */
	StorageInode ( String svcName, String id )
	{
		fSvc = svcName;
		fId = id;
	}

	private final String fSvc;
	private final String fId;
}
