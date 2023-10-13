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
import java.util.Map;

import io.continual.util.standards.HttpHeaders;

/**
 * A response to a request.
 */
public interface CHttpResponse
{
	/**
	 * send an error using the servlet container's error page system
	 * 
	 * @param err
	 * @param msg
	 */
	void sendError ( int err, String msg );

	void sendStatusAndMessage ( int status, String msg );

	/**
	 * set the status code for the reply
	 * 
	 * @param code
	 */
	CHttpResponse setStatus ( int code );

	/**
	 * Get the status code set on the response
	 * @return the status code
	 */
	int getStatusCode ();

	CHttpResponse setContentType ( String mimeType );

	CHttpResponse send ( String content ) throws IOException;

	/**
	 * send an error response with the given body
	 * 
	 * @param err
	 * @param content
	 * @param mimeType
	 */
	@Deprecated
	default void sendErrorAndBody ( int err, String content, String mimeType )
	{
		sendStatusAndBody ( err, content, mimeType );
	}

	/**
	 * send a status code and the given body
	 * 
	 * @param statusCode
	 * @param content
	 * @param mimeType
	 */
	void sendStatusAndBody ( int statusCode, String content, String mimeType );

	/**
	 * Get a stream (PrintWriter) for a text response using the text/html content type
	 * and UTF-8 encoding.
	 * 
	 * @return a PrintWriter
	 * @throws IOException
	 */
	PrintWriter getStreamForTextResponse ()
		throws IOException;

	/**
	 * Get a stream (PrintWriter) for a text response using the given content type
	 * and UTF-8 encoding.
	 * 
	 * @param contentType
	 * @return a PrintWriter
	 * @throws IOException
	 */
	PrintWriter getStreamForTextResponse ( String contentType )
		throws IOException;

	/**
	 * Get a stream (PrintWriter) for a text response using the given content type
	 * and given encoding.
	 * 
	 * @param contentType
	 * @param encoding
	 * @return a PrintWriter
	 * @throws IOException
	 */
	PrintWriter getStreamForTextResponse ( String contentType, String encoding )
		throws IOException;

	/**
	 * get a stream for a binary response with content type application/octet-stream
	 * @return an OutputStream
	 * @throws IOException
	 */
	OutputStream getStreamForBinaryResponse ()
		throws IOException;

	/**
	 * get a stream for a binary response with the given content type
	 * @return an OutputStream
	 * @throws IOException
	 */
	OutputStream getStreamForBinaryResponse ( String contentType )
		throws IOException;

	CHttpResponse writeHeader ( String headerName, String headerValue );

	default CHttpResponse writeHeader ( HttpHeaders headerName, String headerValue )
	{
		return writeHeader ( headerName.toString (), headerValue );
	}

	CHttpResponse writeHeader ( String headerName, String headerValue, boolean overwrite );

	default CHttpResponse writeHeader ( HttpHeaders headerName, String headerValue, boolean overwrite )
	{
		return writeHeader ( headerName.toString (), headerValue, overwrite );
	}

	/**
	 * redirect the to the app-relative url
	 * 
	 * @param url
	 */
	void redirect ( String url );

	void redirect ( Class<?> cls, String method );

	void redirect ( Class<?> cls, String method, Map<String, Object> args );

	/**
	 * redirect to the exact url
	 * 
	 * @param url
	 */
	void redirectExactly ( String url );
}
