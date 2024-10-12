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

package io.continual.http.app.servers.routeInstallers;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.iam.access.AccessException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.util.standards.HttpStatusCodes;

public class TypicalApiServiceRouteInstaller extends BaseRouteInstaller
{
	public TypicalApiServiceRouteInstaller ()
	{
		this ( true, true );
	}

	public TypicalApiServiceRouteInstaller ( boolean withCors, boolean withStdErrHandlers )
	{
		super ( withCors );

		if ( withStdErrHandlers )
		{
			registerErrorHandler ( CHttpRequestRouter.noMatchingRoute.class, HttpStatusCodes.k404_notFound, "Not found. See the API docs." );
			registerErrorHandler ( JSONException.class, HttpStatusCodes.k400_badRequest, "Bad request. See the API docs." );
			registerErrorHandler ( AccessException.class, HttpStatusCodes.k403_forbidden, "Forbidden." );
			registerErrorHandler ( IamSvcException.class, HttpStatusCodes.k503_serviceUnavailable );
			registerErrorHandler ( Throwable.class, HttpStatusCodes.k500_internalServerError, "There was a problem at the server.", log );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( TypicalApiServiceRouteInstaller.class );
}
