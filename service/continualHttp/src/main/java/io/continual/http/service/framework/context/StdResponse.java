/*
/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.service.framework.context;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.inspection.CHttpObserver;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.util.http.standards.HttpMethods;
import io.continual.util.standards.MimeTypes;

class StdResponse implements CHttpResponse
{
	public StdResponse ( HttpServletRequest req, HttpServletResponse r, CHttpRequestRouter rr, CHttpObserver inspector )
	{
		fRequest = req;
		fResponseEntityAllowed = !(req.getMethod ().equalsIgnoreCase ( HttpMethods.HEAD ));
		fResponse = r;
		fRouter = rr;
		fInspector = inspector;
	}

	@Override
	public void sendErrorAndBody ( int err, String content, String mimeType )
	{
		try
		{
			setStatus ( err );
			getStreamForTextResponse ( mimeType ).println ( content );
		}
		catch ( IOException e )
		{
			log.warn ( "Error sending error response: " + e.getMessage () );
		}
	}

	@Override
	public void sendError ( int err, String msg )
	{
		sendStatusAndMessage ( err, msg );
	}
	
	@Override
	public void sendStatusAndMessage ( int status, String msg )
	{
		try
		{
			fInspector.replyWith ( status, msg );
			fResponse.sendError ( status, msg );
		}
		catch ( IOException e )
		{
			log.error ( "Error sending response: " + e.getMessage () );
		}
	}

	@Override
	public CHttpResponse setStatus ( int code )
	{
		fInspector.replyWith ( code );
		fResponse.setStatus ( code );
		return this;
	}

	@Override
	public int getStatusCode ()
	{
		return fResponse.getStatus ();
	}

	@Override
	public CHttpResponse setContentType ( String mimeType )
	{
		fInspector.replyHeader ( "ContentType", mimeType );
		fResponse.setContentType ( mimeType );
		return this;
	}

	@Override
	public CHttpResponse send ( String content ) throws IOException
	{
		final PrintWriter pw = new PrintWriter ( fResponse.getWriter () );
		pw.print ( content );
		pw.close ();
		return this;
	}

	@Override
	public CHttpResponse writeHeader ( String headerName, String headerValue )
	{
		writeHeader ( headerName, headerValue, false );
		return this;
	}

	@Override
	public CHttpResponse writeHeader ( String headerName, String headerValue, boolean overwrite )
	{
		fInspector.replyHeader ( headerName, headerValue );

		if ( overwrite )
		{
			fResponse.setHeader ( headerName, headerValue );
		}
		else
		{
			fResponse.addHeader ( headerName, headerValue );
		}
		return this;
	}

	@Override
	public OutputStream getStreamForBinaryResponse () throws IOException
	{
		return getStreamForBinaryResponse ( MimeTypes.kAppGenericBinary );
	}

	@Override
	public OutputStream getStreamForBinaryResponse ( String contentType ) throws IOException
	{
		fResponse.setContentType ( contentType );

		OutputStream os ;
		if ( fResponseEntityAllowed )
		{
			os = fInspector.wrap ( fResponse.getOutputStream () );
		}
		else
		{
			os = new NullStream ();
		}
		return os;
	}

	@Override
	public PrintWriter getStreamForTextResponse ()
		throws IOException
	{
		return getStreamForTextResponse ( "text/html" );
	}

	@Override
	public PrintWriter getStreamForTextResponse ( String contentType ) throws IOException
	{
		return getStreamForTextResponse ( contentType, "UTF-8" );
	}

	@Override
	public PrintWriter getStreamForTextResponse ( String contentType, String encoding ) throws IOException
	{
		fResponse.setContentType ( contentType );
		fResponse.setCharacterEncoding ( encoding );

		PrintWriter pw ;
		if ( fResponseEntityAllowed )
		{
			pw = fInspector.wrap ( fResponse.getWriter () );
		}
		else
		{
			pw = new PrintWriter ( new NullWriter () );
		}
		return pw;
	}

	@Override
	public void redirect ( String url )
	{
		redirectExactly ( CHttpRequestContext.servletPathToFullPath ( url, fRequest ) );
	}

	@Override
	public void redirect ( Class<?> cls, String method, CHttpConnection forSession )
	{
		redirect ( cls, method, new HashMap<String, Object> (), forSession );
	}

	@Override
	public void redirect ( Class<?> cls, String method, Map<String, Object> args, CHttpConnection forSession )
	{
		String localUrl = fRouter.reverseRoute ( cls, method, args, forSession );
		if ( localUrl == null )
		{
			log.error ( "No reverse route for " + cls.getName () + "::" + method + " with " + (args == null ? 0 : args.size () ) + " args." );
			localUrl = "/";
		}
		redirect ( localUrl );
	}

	@Override
	public void redirectExactly ( String url )
	{
		try
		{
			fResponse.sendRedirect ( url );
		}
		catch ( IOException e )
		{
			log.error ( "Error sending redirect: " + e.getMessage () );
		}
	}

	private final HttpServletRequest fRequest;
	private final boolean fResponseEntityAllowed;
	private final HttpServletResponse fResponse;
	private final CHttpRequestRouter fRouter;
	private final CHttpObserver fInspector;

	private static org.slf4j.Logger log = LoggerFactory.getLogger ( StdResponse.class );

	private static class NullWriter extends Writer
	{
        /**
         *
         * @param cbuf
         * @param off
         * @param len
         */
		@Override
		public void write ( char[] cbuf, int off, int len )
		{
		}

		@Override
		public void flush ()
		{
		}

		@Override
		public void close ()
		{
		}
	}

	private static class NullStream extends OutputStream
	{
		@Override
		public void write ( int b ) {}
	}
}
