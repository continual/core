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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.app.servers.endpoints.TypicalUiEndpoint.NoLoginException;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.templating.ContinualTemplateEngine.TemplateParseException;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;

public class TypicalUiServiceRouteInstaller extends BaseRouteInstaller
{
	public TypicalUiServiceRouteInstaller ()
	{
		super ( false );

		final Logger log = LoggerFactory.getLogger ( this.getClass ().getName () );

		registerErrorHandler ( CHttpRequestRouter.noMatchingRoute.class, CHttpErrorHandler.redirect ( "/" ) );
		registerErrorHandler ( NoLoginException.class, CHttpErrorHandler.redirect ( "/" ) );
		registerErrorHandler ( TemplateNotFoundException.class, CHttpErrorHandler.redirect ( "/" ) );
		registerErrorHandler ( TemplateParseException.class, CHttpErrorHandler.redirect ( "/" ) );
		registerErrorHandler ( IamSvcException.class, CHttpErrorHandler.redirect ( "/", log ) );
		registerErrorHandler ( Throwable.class, CHttpErrorHandler.redirect ( "/", log ) );
	}
}
