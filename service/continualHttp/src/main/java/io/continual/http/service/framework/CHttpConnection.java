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

package io.continual.http.service.framework;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.servlet.ServletException;

/**
 * The connection represents a session between a client system and the server.
 */
public interface CHttpConnection
{
	/**
	 * Called when the servlet associates this connection to a client system.
	 * @param ws
	 * @param dcc
	 * @throws ServletException
	 */
	void onSessionCreate ( CHttpServlet ws, CHttpConnectionContext dcc ) throws ServletException;

	/**
	 * Called when the connection is closing.
	 */
	void onSessionClose ();

	/**
	 * Called when the session receives client activity.
	 */
	void noteActivity ();

	/**
	 * Called when the servlet requires the connection to build a context for use by
	 * a renderer.
	 * @param context
	 */
	void buildTemplateContext ( HashMap<String, Object> context );

	/**
	 * serialize the session for out of process storage
	 */
	ByteArrayInputStream serialize ();

	/**
	 * deserialize the session to load from out of process storage
	 * @param sessionData Data written by serialize()
	 */
	void deserialize ( ByteArrayInputStream sessionData );
}
