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

import io.continual.restHttp.HttpService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.service.ModelService;
import io.continual.util.nv.NvReadable;

public class ModelApiService extends SimpleService
{
	private static final String kSetting_ModelServiceName = "modelService";
	private static final String kDefault_ModelServiceName = "model";

	public ModelApiService ( ServiceContainer sc, NvReadable settings ) throws NvReadable.MissingReqdSettingException
	{
		final HttpService server = sc.get ( settings.getString ( "httpService" ), HttpService.class );

		server.addRouter (
			"modelApi",
			new ModelApiRouter (
				sc,
				settings,
				sc.get (
					settings.getString ( kSetting_ModelServiceName, kDefault_ModelServiceName ),
					ModelService.class
				)
			)
		);
	}
}
