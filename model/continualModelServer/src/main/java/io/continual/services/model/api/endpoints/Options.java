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

package io.continual.services.model.api.endpoints;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.CorsOptionsRouter;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.standards.HttpStatusCodes;

public class Options extends TypicalRestApiEndpoint<Identity>
{
	public Options ( ServiceContainer sc, JSONObject settings ) throws BuildFailure
	{
		super ( sc, settings );
	}

	public void listOptions ( CHttpRequestContext context, final String path )
	{
		CorsOptionsRouter.setupCorsHeaders ( context );

		final CHttpResponse reply = context.response ();
		reply.setStatus ( HttpStatusCodes.k204_noContent );
	}
}
