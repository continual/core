/*
 *	Copyright 2024, Continual.io
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

package io.continual.iam.apiserver.endpoints;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.standards.HttpStatusCodes;

public class Health extends TypicalRestApiEndpoint<Identity>
{
	public Health ( ServiceContainer sc, JSONObject settings ) throws BuildFailure
	{
		super ( sc, settings );
	}

	public void getHealth ( CHttpRequestContext context )
	{
		context.response ().setStatus ( HttpStatusCodes.k204_noContent );
	}
}
