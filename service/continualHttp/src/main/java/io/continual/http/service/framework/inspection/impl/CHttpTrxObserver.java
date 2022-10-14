package io.continual.http.service.framework.inspection.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.inspection.CHttpObserver;
import io.continual.http.service.framework.inspection.CHttpObserverMgr;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.time.Clock;

public class CHttpTrxObserver extends SimpleService implements CHttpObserverMgr
{
	public CHttpTrxObserver ( File baseDir )
	{
		fBaseDir = baseDir;
	}
	
	public CHttpTrxObserver ( ServiceContainer sc, JSONObject config )
	{
		fBaseDir = new File ( config.optString ( "baseDir", "./logs" ) );

		JsonVisitor.forEachElement ( config.optJSONArray ( "filters" ), new ArrayVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( JSONObject filter ) throws JSONException
			{
				final String logName = filter.getString ( "logName" );
				final String method = filter.optString ( "method", ".*" );
				final String path = filter.optString ( "path", ".*" );

				addFilter ( new Filter ()
				{
					@Override
					public String logName () { return logName; }

					@Override
					public boolean matches ( CHttpRequestContext ctx )
					{
						final CHttpRequest req = ctx.request ();
						final String reqMethod = req.getMethod ();
						final String reqPath = req.getPathInContext ();

						return ( reqMethod.matches ( method ) && reqPath.matches ( path ) );
					}
					
				} );
				return true;
			}
		});
	}

	public interface Filter
	{
		String logName ();
		boolean matches ( CHttpRequestContext ctx );
	}

	@Override
	public void consider ( CHttpRequestContext ctx )
	{
		for ( Filter f : fFilters )
		{
			if ( f.matches ( ctx ) )
			{
				try
				{
					final String name = f.logName () + "." + Clock.now () + ".log";
					ctx.install ( new TrxDumper ( new PrintWriter ( new FileOutputStream ( new File ( fBaseDir, name ), true ) ) ) );
				}
				catch ( IOException e )
				{
					log.warn ( "Couldn't install inspector on trx: " + e.getMessage () );
				}
				return;
			}
		}
	}

	public List<Filter> getFilters ()
	{
		return Collections.unmodifiableList ( fFilters );
	}

	public CHttpObserverMgr addFilter ( Filter f )
	{
		fFilters.add ( f );
		return this;
	}

	public CHttpObserverMgr removeFilter ( Filter f )
	{
		fFilters.remove ( f );
		return this;
	}

	private class TrxDumper implements CHttpObserver
	{
		TrxDumper ( PrintWriter to ) throws IOException
		{
			fOut = to;

			fOut.println (  );
			fOut.println ( "vvv" );
			fOut.println ( "at: " + Clock.now () );
		}

		@Override
		public CHttpObserver method ( String method )
		{
			fOut.println ( "method: " + method );
			return this;
		}

		@Override
		public CHttpObserver onUrl ( String url )
		{
			fOut.println ( "url: " + url );
			return this;
		}

		@Override
		public CHttpObserver queryString ( String qs )
		{
			fOut.println ( "query: " + qs );
			return this;
		}

		@Override
		public CHttpObserver contentTypeRequest ( String type )
		{
			fOut.println ( "contentType: " + type );
			return this;
		}

		@Override
		public CHttpObserver contentLengthRequest ( int length )
		{
			fOut.println ( "contentLength: " + length );
			return this;
		}

		@Override
		public CHttpObserver withHeaders ( HeaderLister hl )
		{
			final Map<String,List<String>> headers = hl.getHeaders ();
			final LinkedList<String> keys = new LinkedList<>();
			keys.addAll ( headers.keySet () );
			for ( String key : keys )
			{
				final List<String> values = headers.get ( key );
				final String val = values.size () == 1 ? values.get ( 0 ) : values.toString ();
				fOut.println ( key + ": " + val );
			}
			return this;
		}

		@Override
		public InputStream wrap ( ServletInputStream inputStream )
		{
			fInStream = new TracingInputStream ( inputStream, fOut );
			return fInStream;
		}

		@Override
		public PrintWriter wrap ( PrintWriter writer )
		{
			fPwStream = new TracingPrintWriter ( writer, fOut );
			return fPwStream;
		}

		@Override
		public OutputStream wrap ( ServletOutputStream outputStream )
		{
			fOutStream = new TracingOutputStream ( outputStream, fOut );
			return outputStream;
		}

		@Override
		public CHttpObserver replyWith ( int status, String msg )
		{
			checkReplyStart ();
			fOut.println ( "reply: " + status + " " + msg );
			return this;
		}

		@Override
		public CHttpObserver replyWith ( int code )
		{
			checkReplyStart ();
			fOut.println ( "reply: " + code );
			return this;
		}

		@Override
		public CHttpObserver replyHeader ( String key, String value )
		{
			checkReplyStart ();
			fOut.println ( "reply header: " + key + ": " + value );
			return this;
		}

		@Override
		public void closeTrx ()
		{
			if ( fInStream != null )
			{
				fInStream.flushLog ();
			}
			if ( fOutStream != null )
			{
				fOutStream.flushLog ();
			}
			if ( fPwStream != null )
			{
				fPwStream.flush ();
			}
			fOut.println ( "^^^" );
			fOut.close ();
		}

		private void checkReplyStart ()
		{
			if ( !fReplyStarted )
			{
				fOut.println ( "" );
				fOut.println ( "---" );
				fOut.println ( "" );
				fReplyStarted = true;
			}
		}
		
		private final PrintWriter fOut;
		private TracingInputStream fInStream;
		private TracingPrintWriter fPwStream;
		private TracingOutputStream fOutStream;
		private boolean fReplyStarted = false;
	}

	private class TracingInputStream extends InputStream
	{
		public TracingInputStream ( InputStream to, PrintWriter dump )
		{
			fTo = to;
			fLog = dump;
		}

	    public int read() throws IOException
	    {
	    	final int b = fTo.read ();
	    	if ( b > -1 ) out ( b );
	    	return b;
	    }

	    public int read(byte b[]) throws IOException
	    {
	    	final int result = fTo.read ( b );
	    	out ( b, 0, result );
	    	return result;
	    }

	    public int read(byte b[], int off, int len) throws IOException
	    {
	    	final int result = fTo.read ( b, off, len );
	    	out ( b, off, result );
	    	return result;
	    }

	    public long skip(long n) throws IOException
	    {
	    	out ( "skip " + n );
	    	return fTo.skip ( n );
	    }

	    public int available() throws IOException
	    {
	        return fTo.available ();
	    }

	    public void close() throws IOException
	    {
	    	flushLog ();
	    	fTo.close ();
	    }

		public synchronized void mark ( int readlimit )
	    {
			out ( "mark(" + readlimit + ")" );
	    	fTo.mark ( readlimit );
	    }

		public synchronized void reset () throws IOException
	    {
			out ( "reset()" );
	    	fTo.reset ();
	    }

	    public boolean markSupported()
	    {
	        return fTo.markSupported ();
	    }
	    
		private final InputStream fTo;
		private final PrintWriter fLog;

		private static final int kLineLength = 16;
		private StringBuilder fHexBytes = new StringBuilder ();
		private StringBuilder fPrintableBytes = new StringBuilder ();
		private int fPendingLength = 0;
		
		private void out ( int b )
		{
			final byte[] bs = new byte[] { (byte)b };
			out ( bs, 0, 1 );
		}

		private void out ( byte[] bytes, int off, int len )
		{
			for ( int i=off; i<off+len; i++ )
			{
				addByte ( bytes[i] );
			}
		}

		private void out ( String msg )
		{
			flushLog ();
			fLog.println ( msg );
		}

		private void addByte ( byte b )
		{
			final String hex = TypeConvertor.byteToHex ( b );
			fHexBytes.append ( hex ).append ( ' ' );

			if ( Character.isISOControl ( (int) b ) )
			{
				fPrintableBytes.append ( "." );
			}
			else
			{
				fPrintableBytes.append ( (char) b );
			}

			fPendingLength++;
			if ( fPendingLength >= kLineLength )
			{
				flushLog ();
			}
		}

		public void flushLog ()
		{
			if ( fPendingLength > 0 )
			{
				fLog.print ( fHexBytes.toString () );

				final int lacking = kLineLength - fPendingLength;
				for ( int i=0; i<lacking; i++ )
				{
					fLog.print ( "   " );	// 3 spaces
				}

				fLog.print ( "  " );
				fLog.println ( fPrintableBytes.toString () );
				fLog.flush ();

				fHexBytes = new StringBuilder ();
				fPrintableBytes = new StringBuilder ();
				fPendingLength = 0;
			}
		}
	}

	private class TracingOutputStream extends OutputStream
	{
		public TracingOutputStream ( OutputStream to, PrintWriter dump )
		{
			fTo = to;
			fLog = dump;
		}

		public void write ( int b ) throws IOException
		{
			out ( b );
			fTo.write ( b );
		}

		public void write ( byte b[] ) throws IOException
		{
			out ( b, 0, b.length );
			fTo.write ( b );
		}

		public void write ( byte b[], int off, int len ) throws IOException
		{
			out ( b, off, len );
			fTo.write ( b, off, len );
		}

		public void flush () throws IOException
		{
			fTo.flush ();
		}

		public void close () throws IOException
		{
			fTo.close ();
		}

		private final OutputStream fTo;
		private final PrintWriter fLog;

		private static final int kLineLength = 16;
		private StringBuilder fHexBytes = new StringBuilder ();
		private StringBuilder fPrintableBytes = new StringBuilder ();
		private int fPendingLength = 0;
		
		private void out ( int b )
		{
			final byte[] bs = new byte[] { (byte)b };
			out ( bs, 0, 1 );
		}

		private void out ( byte[] bytes, int off, int len )
		{
			for ( int i=off; i<off+len; i++ )
			{
				addByte ( bytes[i] );
			}
		}

		private void addByte ( byte b )
		{
			final String hex = TypeConvertor.byteToHex ( b );
			fHexBytes.append ( hex ).append ( ' ' );

			if ( Character.isISOControl ( (int) b ) )
			{
				fPrintableBytes.append ( "." );
			}
			else
			{
				fPrintableBytes.append ( (char) b );
			}

			fPendingLength++;
			if ( fPendingLength >= kLineLength )
			{
				flushLog ();
			}
		}

		public void flushLog ()
		{
			if ( fPendingLength > 0 )
			{
				fLog.print ( fHexBytes.toString () );

				final int lacking = kLineLength - fPendingLength;
				for ( int i=0; i<lacking; i++ )
				{
					fLog.print ( "   " );	// 3 spaces
				}

				fLog.print ( "  " );
				fLog.println ( fPrintableBytes.toString () );

				fHexBytes = new StringBuilder ();
				fPrintableBytes = new StringBuilder ();
				fPendingLength = 0;
			}
		}
	}

	private class TracingPrintWriter extends PrintWriter
	{
		public TracingPrintWriter ( PrintWriter to, PrintWriter dump )
		{
			super ( to );

			fLog = dump;
		}

		@Override
		public void write ( String s, int off, int len )
		{
			fLog.write ( s, off, len );
			super.write ( s, off, len );
		}

		@Override
		public void println ()
		{
			fLog.println ();
			super.println ();
		}

		@Override
	    public void close ()
	    {
	    	fLog.close ();
	    	super.close ();
	    }

		@Override
		public void flush ()
		{
	    	fLog.flush ();
	    	super.flush ();
		}
		
		private final PrintWriter fLog;
	}

	private final File fBaseDir;
	private final LinkedList<Filter> fFilters = new LinkedList<> ();

	private static final Logger log = LoggerFactory.getLogger ( CHttpTrxObserver.class );
}
