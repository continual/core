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

package io.continual.services.model.api;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.api.endpoints.AuthApiHandler;
import io.continual.services.model.api.endpoints.Health;
import io.continual.services.model.api.endpoints.MetricsApiHandler;
import io.continual.services.model.api.endpoints.ModelApi;
import io.continual.services.model.api.endpoints.Options;
import io.continual.services.model.core.exceptions.ModelAccessException;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.service.ModelService;
import io.continual.util.standards.HttpStatusCodes;

@SuppressWarnings("deprecation")
public class ModelApiService extends SimpleService
{
	private static final String kSetting_HttpServiceName = "httpService";
	private static final String kDefault_HttpServiceName = "http";

	private static final String kSetting_ModelServiceName = "modelService";
	private static final String kDefault_ModelServiceName = "model";

	public ModelApiService ( ServiceContainer sc, JSONObject settings ) throws BuildFailure 
	{
		final CHttpService server = sc.getReqd ( settings.optString ( kSetting_HttpServiceName, kDefault_HttpServiceName ), CHttpService.class );
		final ModelService ms = sc.getReqd ( settings.optString ( kSetting_ModelServiceName, kDefault_ModelServiceName ), ModelService.class );

		final ModelApi modelApi = new ModelApi ( sc, settings, ms );
		final AuthApiHandler authApi = new AuthApiHandler ( sc, settings );

		server.addRouteInstaller (
			new TypicalApiServiceRouteInstaller ()
				.registerRoutes ( "authRoutes.conf", ModelApiService.class, authApi )
				.registerRoutes ( "modelApi.conf", ModelApiService.class, modelApi )
				.registerRoutes ( "metrics.conf", ModelApiService.class, new MetricsApiHandler ( sc, settings ) )
				.registerRoutes ( "options.conf", ModelApiService.class, new Options ( sc, settings ) )
				.registerRoutes ( "health.conf", ModelApiService.class, new Health ( sc, settings ) )
				.registerErrorHandler ( ModelAccessException.class, HttpStatusCodes.k403_forbidden )
				.registerErrorHandler ( ModelItemDoesNotExistException.class, HttpStatusCodes.k404_notFound )
				.registerErrorHandler ( ModelRequestException.class, HttpStatusCodes.k400_badRequest )
		);
	}
}
