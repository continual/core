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

package io.continual.services.model.service.impl.s3;

import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.service.ModelLoaderContext;
import io.continual.util.nv.NvReadable;

/**
 * Additional context for S3 models
 */
class S3ModelLoaderContext extends ModelLoaderContext
{
	public S3ModelLoaderContext ( ServiceContainer sc, S3ModelService svc, S3Interface s3, NvReadable settings )
	{
		this ( sc, svc, s3, settings, null );
	}

	private S3ModelLoaderContext ( ServiceContainer sc, S3ModelService svc, S3Interface s3, NvReadable settings, ModelObjectPath path  )
	{
		super ( sc, settings );

		fS3ModelService = svc;
		fS3Interface = s3;
		fPath = path;
	}

	/**
	 * Get the S3 interface
	 * @return an S3 interface
	 */
	public S3Interface getS3Interface ()
	{
		return fS3Interface;
	}

	/**
	 * Get the S3 model service
	 * @return an S3 model service
	 */
	public S3ModelService getS3ModelService ()
	{
		return fS3ModelService;
	}

	/**
	 * Get the path associated with this load operation
	 * @return a path
	 */
	public ModelObjectPath getPath ()
	{
		return fPath;
	}
	
	public S3ModelLoaderContext withPath ( ModelObjectPath p )
	{
		return new S3ModelLoaderContext (
			getServiceContainer (),
			fS3ModelService,
			fS3Interface,
			getSettings (),
			p
		);
	}

	private final ModelObjectPath fPath;
	private final S3ModelService fS3ModelService;
	private final S3Interface fS3Interface;
}
