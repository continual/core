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

package io.continual.http.service.framework.routing;

import java.util.Map;

/**
 * A route source is a collection of routes that are requested by verb (e.g. GET) and
 * a path. An app can have any number of route sources. During request handling,
 * each route source is tested in order via getRouteFor(). If the route source returns
 * a {@link CHttpRouteInvocation}, it's used to handle the request.
 */
public interface CHttpRouteSource
{
	/**
	 * Return the route handler for a given verb and path or null.
	 * @param verb
	 * @param path
	 * @return a route invocation or null
	 */
	CHttpRouteInvocation getRouteFor ( String verb, String path );

	/**
	 * Code in this system can create a URL to get to a specific class + method by asking
	 * the router to find a reverse-route. If this route source has routes that point to
	 * static entry points, it should implement an override that returns the correct URL.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @param args
	 * @return null, or a URL to get to the entry point
	 */
	String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args );
}
