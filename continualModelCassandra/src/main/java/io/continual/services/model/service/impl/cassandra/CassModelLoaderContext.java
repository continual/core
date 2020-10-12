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

package io.continual.services.model.service.impl.cassandra;

import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.service.ModelLoaderContext;
import io.continual.util.nv.NvReadable;

/**
 * Additional context for S3 models
 */
class CassModelLoaderContext extends ModelLoaderContext
{
	public CassModelLoaderContext ( ServiceContainer sc, CassandraModelService svc, NvReadable settings )
	{
		this ( sc, svc, settings, null );
	}

	private CassModelLoaderContext ( ServiceContainer sc, CassandraModelService svc, NvReadable settings, ModelObjectPath path  )
	{
		super ( sc, settings );

		fModelService = svc;
		fPath = path;
	}

	/**
	 * Get the model service
	 * @return a model service
	 */
	public CassandraModelService getModelService ()
	{
		return fModelService;
	}

	/**
	 * Get the path associated with this load operation
	 * @return a path
	 */
	public ModelObjectPath getPath ()
	{
		return fPath;
	}
	
	public CassModelLoaderContext withPath ( ModelObjectPath p )
	{
		return new CassModelLoaderContext (
			getServiceContainer (),
			fModelService,
			getSettings (),
			p
		);
	}

	private final ModelObjectPath fPath;
	private final CassandraModelService fModelService;
}
