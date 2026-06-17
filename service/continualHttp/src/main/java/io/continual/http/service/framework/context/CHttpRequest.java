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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.continual.util.data.TypeConvertor;
import io.continual.util.standards.HttpHeaders;

/**
 * A request made to a servlet.
 */
public interface CHttpRequest
{
	/**
	 * Get the request's URL
	 * @return the URL
	 */
	String getUrl ();

	/**
	 * Get the query string from the HTTP request, or null if there's no query
	 * @return the query string or null
	 */
	default String getQueryString () { return null; }

	/**
	 * Get the HTTP method for this request. 
	 * @return the HTTP method
	 */
	default String getMethod () { return "GET"; }

	/**
	 * get the request's path within the servlet context
	 * @return the request path within the servlet context
	 */
	String getPathInContext ();

	/**
	 * Get the first value (of 1 or more) for a given header name.
	 * @param header
	 * @return null if the header does not exist, or the first value otherwise
	 */
	default String getFirstHeader ( String header )
	{
		final List<String> vals = getHeader ( header );
		return vals.size () > 0 ? vals.get ( 0 ) : null;
	}

	/**
	 * Get the first value (of 1 or more) for a given header name.
	 * @param header
	 * @return null if the header does not exist, or the first value otherwise
	 */
	default String getFirstHeader ( HttpHeaders header )
	{
		return getFirstHeader ( header.toString () );
	}

	/**
	 * Get all values for a given header.
	 * @param header
	 * @return a list of 0 or more values
	 */
	default List<String> getHeader ( String header )
	{
		for ( Map.Entry<String, List<String>> e : getAllHeaders ().entrySet () )
		{
			if ( e.getKey ().equalsIgnoreCase ( header ) )
			{
				return e.getValue () == null ? new LinkedList<> () : e.getValue ();
			}
		}
		return new LinkedList<> ();
	}

	/**
	 * Get all values for a given header.
	 * @param header
	 * @return a list of 0 or more values
	 */
	default List<String> getHeader ( HttpHeaders header )
	{
		return getHeader ( header.toString () );
	}

	/**
	 * Get all headers.
	 * @return a map of headers
	 */
	Map<String,List<String>> getAllHeaders ();

	/**
	 * Get the parameter map for this request.
	 * @return a map of name/value pairs.
	 */
	Map<String, String[]> getParameterMap ();

	/**
	 * get a parameter by name
	 * @param key
	 * @return null, or the value of the named parameter
	 */
	String getParameter ( String key );

	/**
	 * get a parameter by name. If the parameter does not exist on this request,
	 * return the default value provided.
	 * 
	 * @param key
	 * @param defVal
	 * @return the value of the parameter, or the default value
	 */
	default String getParameter ( String key, String defVal )
	{
		final String val = getParameter ( key );
		return val == null ? defVal : val;
	}

	/**
	 * Get a parameter as an integer.
	 * @param key
	 * @param defVal
	 * @return an integer parameter, or the default value
	 */
	default int getIntParameter ( String key, int defVal )
	{
		final String val = getParameter ( key );
		return val == null ? defVal : TypeConvertor.convertToInt ( val, defVal );
	}

	/**
	 * Get a parameter as an long.
	 * @param key
	 * @param defVal
	 * @return an long parameter, or the default value
	 */
	default long getLongParameter ( String key, long defVal )
	{
		final String val = getParameter ( key );
		return val == null ? defVal : TypeConvertor.convertToLong ( val, defVal );
	}

	/**
	 * Get a parameter as a double.
	 * @param key
	 * @param defVal
	 * @return a double value
	 */
	default double getDoubleParameter ( String key, double defVal )
	{
		final String val = getParameter ( key );
		return val == null ? defVal : TypeConvertor.convertToDouble ( val, defVal );
	}

	/**
	 * Get a parameter as a boolean.
	 * @param key
	 * @param defVal
	 * @return a boolean value
	 */
	default boolean getBooleanParameter ( String key, boolean defVal )
	{
		final String val = getParameter ( key );
		return val == null ? defVal : TypeConvertor.convertToBooleanBroad ( val );
	}

	/**
	 * Get a parameter as a char.
	 * @param key
	 * @param defVal
	 * @return a character value
	 */
	default char getCharParameter ( String key, char defVal )
	{
		final String val = getParameter ( key );
		return val == null || val.isBlank () ? defVal : val.trim ().charAt ( 0 );
	}
	
	/**
	 * Change the value of a parameter on this request. (Generally used by validators.)
	 * @param fieldName
	 * @param defVal
	 */
	void changeParameter ( String fieldName, String defVal );

	/**
	 * Get the content type header for this request.
	 * @return
	 */
	String getContentType ();

	/**
	 * Get the content length for this request.
	 * @return the number of bytes of content in this request
	 */
	int getContentLength ();

	/**
	 * get the content as an input stream
	 * @return the body of the request as an input stream
	 * @throws IOException
	 */
	InputStream getBodyStream () throws IOException;

	/**
	 * get the content of the request as text.
	 * @return a buffered reader on the content of this request.
	 * @throws IOException
	 */
	BufferedReader getBodyStreamAsText () throws IOException;

	/**
	 * get the address of the requesting agent
	 * @return the address of the requesting agent
	 */
	String getActualRemoteAddress ();

	/**
	 * get the address of the requesting agent, using proxy headers to determine the actual client
	 * when behind a load-balancer/reverse-proxy
	 * @return the address of the requesting agent
	 */
	String getBestRemoteAddress ();

	/**
	 * Get the actual connected port number
	 * @return the actual connected client port number
	 */
	int getActualRemotePort ();

	/**
	 * Get the best connected port number, respecting standard proxy headers
	 * @return a client port number
	 */
	int getBestRemotePort ();
	
	/**
	 * return true if the request (and response) was made over a secure transport
	 * @return true if the request was made over a secure transport
	 */
	boolean isSecure ();
}
