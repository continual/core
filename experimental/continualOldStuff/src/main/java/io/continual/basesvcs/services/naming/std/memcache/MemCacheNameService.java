package io.continual.basesvcs.services.naming.std.memcache;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.naming.NameService;
import io.continual.basesvcs.services.naming.NamingIoException;
import io.continual.basesvcs.services.naming.std.BaseNameService;
import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.services.ServiceContainer;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.CachedData;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.Transcoder;

/**
 * A naming service running against memcached, with a backing service for cache misses.
 * (This service implementation is really a caching wrapper over another service.)
 * 
 * @author peter
 */
public class MemCacheNameService extends BaseNameService
{
	public static final String kSetting_MemcachedServers = "names.memcache.servers";
	private static final String kDefault_MemcachedServers = "localhost:11211";

	public static final String kSetting_BackingService = "names.memcache.backingService";
	public static final String kSetting_PassThru = "passthru";

	public MemCacheNameService ( final ServiceContainer sc, NvReadable settings ) throws NvReadable.MissingReqdSettingException, IOException
	{
		fPassThru = settings.getBoolean ( kSetting_PassThru, false );
		if ( fPassThru )
		{
			log.warn ( "MemCacheNameService is inactive and passing through to backing service." );
		}

		fClient = fPassThru ? null :
			new MemcachedClient ( AddrUtil.getAddresses ( settings.getString ( kSetting_MemcachedServers, kDefault_MemcachedServers ) ) );

		fBackingService = sc.get ( settings.getString ( kSetting_BackingService ), NameService.class );

		fTranscoder = new Transcoder<StorageInode> ()
		{
			@Override
			public boolean asyncDecode ( CachedData d )
			{
				return false;
			}

			@Override
			public CachedData encode ( StorageInode node )
			{
				try
				{
					final JSONObject o = new JSONObject ()
						.put ( "svc", node.getServiceName () )
						.put ( "id", node.getId () )
					;
					final String json = o.toString ();
					final byte[] bytes = json.getBytes ( "UTF-8" );
					return new CachedData ( 0, bytes, bytes.length );
				}
				catch ( JSONException | UnsupportedEncodingException e )
				{
					throw new RuntimeException ( e );
				}
			}

			@Override
			public StorageInode decode ( CachedData d )
			{
				try
				{
					final String data = new String ( d.getData (), "UTF-8" );
					final JSONObject o = new JSONObject ( data );
					final String svc = o.getString ( "svc" );
					final String id = o.getString ( "id" );
					return StorageInode.fromName ( svc, id );
				}
				catch ( IOException e )
				{
					log.warn ( "Couldn't read JSON back from memcache. " + e.getMessage(), e );
				}
				return null;
			}

			@Override
			public int getMaxSize ()
			{
				return Integer.MAX_VALUE;
			}
		};
	}

	@Override
	public synchronized StorageInode lookup ( Path nodeId ) throws NamingIoException
	{
		StorageInode result = null;

		final String key = nodeId.toString ();

		if ( fClient != null )
		{
			final Future<StorageInode> f = fClient.asyncGet ( key, fTranscoder );
			try
			{
				result = f.get ( 5, TimeUnit.SECONDS );
			}
			catch ( TimeoutException e )
			{
				f.cancel ( false );
				result = null;
			}
			catch ( InterruptedException | ExecutionException e )
			{
				throw new NamingIoException ( e );
			}
		}

		if ( result == null )
		{
			if ( fClient != null )
			{
				log.info ( "Cache miss on " + key + "; going to backing service." );
			}

			result = fBackingService.lookup ( nodeId );
			if ( result != null )
			{
				store ( nodeId, result );
			}
		}
		return result;
	}

	@Override
	public synchronized Set<Path> getChildren ( Path path ) throws NamingIoException
	{
		return fBackingService.getChildren ( path );
	}

	@Override
	public synchronized void store ( Path nodeId, StorageInode inode ) throws NamingIoException
	{
		fBackingService.store ( nodeId, inode );

		if ( fClient != null )
		{
			final String key = nodeId.toString ();
			log.info ( "Caching " + key + "=" + inode.toString () );
			fClient.set ( key, 60 * 60 * 24, inode, fTranscoder );
		}
	}

	@Override
	public synchronized void remove ( Path nodeId ) throws NamingIoException
	{
		fBackingService.remove ( nodeId );

		if ( fClient != null )
		{
			final String key = nodeId.toString ();
			log.info ( "Removing " + key );
			fClient.delete ( key );
		}
	}

	@Override
	protected void onStopRequested ()
	{
		if ( fClient != null )
		{
			fClient.shutdown ( 5, TimeUnit.SECONDS );
		}
	}

	private final boolean fPassThru;
	private final MemcachedClient fClient;
	private final NameService fBackingService;
	private final Transcoder<StorageInode> fTranscoder;

	private static final Logger log = LoggerFactory.getLogger ( MemCacheNameService.class );
}
