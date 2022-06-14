package io.continual.browserDriver;

import java.io.Closeable;
import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.impl.nvWriteableTable;

public class HttpProxy implements Closeable
{
	public HttpProxy ( NvReadable settings )
	{
		fSettings = settings;
//		fProxy = new BrowserUpProxyServer ();
//		fCurrentHar = null;
	}

	/**
	 * Start the proxy
	 */
	public synchronized void start ()
	{
//		fProxy.setTrustAllServers ( true );
//
//		final int explicitPortSetting = fSettings.getInt ( "proxy.port", -1 );
//		if ( explicitPortSetting > 0 )
//		{
//			fProxy.start ( explicitPortSetting );
//		}
//		else
//		{
//			fProxy.start ();
//		}
	}

	/**
	 * close the proxy
	 */
	@Override
	public synchronized void close ()
	{
//		if ( fServer != null ) fServer.stop ();
//
//		final Har har = fProxy.getHar ();
//		if ( har != null )
//		{
//			try
//			{
//				final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
//				har.writeTo ( baos );
//				baos.close ();
//	
//				final ByteArrayInputStream bais = new ByteArrayInputStream ( baos.toByteArray () );
//				final JSONObject o = new JSONObject ( new JSONTokener ( bais ) );
//	
//				final FileWriter fw = new FileWriter ( "/tmp/test.har" );
//				fw.write ( o.toString ( 4 ) );
//				fw.close ();
//			}
//			catch ( IOException e )
//			{
//				log.warn ( e.getMessage (), e );
//			}
//		}
//		fProxy.stop ();
	}

	/**
	 * Get the port on which the proxy is listening, assuming it was started.
	 * @return the port number
	 */
	public synchronized int getPort ()
	{
//		return fProxy.getPort ();
		return -1;
	}
	
	/**
	 * Start timing
	 * @param name the HAR name
	 */
	public synchronized void startTiming ( String name )
	{
//		if ( fCurrentHar != null )
//		{
//			throw new IllegalStateException ( "Stop prior timing before starting another." );
//		}
//
//		fProxy.newHar ( name );
//		fCurrentHar = fProxy.getHar ();
	}

	/**
	 * Stop timing and return the HAR data
	 * @return a JSON representation of the HAR data
	 * @throws IOException on a problem writing the HAR to the byte array
	 */
	public synchronized JSONObject stopTiming () throws IOException
	{
//		if ( fCurrentHar == null )
//		{
//			throw new IllegalStateException ( "Start timing before stopping it." );
//		}
//
//		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
//		try
//		{
//			fCurrentHar.writeTo ( baos );
//			fCurrentHar = null;
//		}
//		finally
//		{
//			baos.close ();
//		}
//
//		try ( final ByteArrayInputStream bais = new ByteArrayInputStream ( baos.toByteArray () ) )
//		{
//			return new JSONObject ( new JSONTokener ( bais ) );
//		}
		return new JSONObject ();
	}

	public static void main ( String[] args )
	{
		final nvWriteableTable settings = new nvWriteableTable ();
		if ( args.length > 0 )
		{
			settings.set ( "proxy.port", args[0] );
		}

		try ( final HttpProxy p = new HttpProxy ( settings ) )
		{
			p.start ();

			System.out.println ( "Waiting for keyboard input..." );
			System.in.read ();
		}
		catch ( IOException e )
		{
			System.err.println ( e.getMessage () );
		}
	}

	private final NvReadable fSettings;
//	private final BrowserMobProxy fProxy;
//	private final BrowserUpProxyServer fProxy;

//	private HttpProxyServer fServer;
//	private Har fCurrentHar;

	private static final Logger log = LoggerFactory.getLogger ( HttpProxy.class );
}
