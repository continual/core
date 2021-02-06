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

import java.util.concurrent.TimeUnit;

/**
 * Context provided to a connection when the servlet associates a client call.
 */
public interface CHttpConnectionContext
{
	/**
	 * Get the remote agent's address. If actual is false, X-Forwarded-For is
	 * used if present. The port is included in this string, like "address:port". 
	 * @param actual
	 * @return the remote agent's address.
	 */
	String getRemoteAddress ( boolean actual );

	/**
	 * If the connection should timeout after inactivity, call setInactiveExpiration on
	 * the connection after it's setup.
	 * @param units
	 * @param tu
	 */
	void setInactiveExpiration ( long units, TimeUnit tu );
}
