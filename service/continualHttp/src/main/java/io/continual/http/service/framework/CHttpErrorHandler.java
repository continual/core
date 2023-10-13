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

package io.continual.http.service.framework;

import org.slf4j.Logger;

import io.continual.http.service.framework.context.CHttpRequestContext;

/**
 * You can register an error handler with the request router.
 */
public interface CHttpErrorHandler
{
	/**
	 * Handle the error. Do not throw out of this method!
	 * @param ctx
	 * @param cause
	 */
	void handle ( CHttpRequestContext ctx, Throwable cause );

	/**
	 * Instantiate a redirecting error handler
	 * @param path
	 * @return an error handler that redirects to the given path
	 */
	static CHttpErrorHandler redirect ( final String path ) { return redirect ( path, null ); }

	/**
	 * Instantiate a redirecting error handler
	 * @param path
	 * @param warningTo
	 * @return an error handler that redirects to the given path
	 */
	static CHttpErrorHandler redirect ( final String path, Logger warningTo )
	{
		return new CHttpErrorHandler ()
		{
			public void handle ( CHttpRequestContext ctx, Throwable cause )
			{
				ctx.response ().redirect ( path );
				if ( warningTo != null )
				{
					warningTo.warn ( cause.getMessage (), cause );
				}
			}
		};
	}
}
