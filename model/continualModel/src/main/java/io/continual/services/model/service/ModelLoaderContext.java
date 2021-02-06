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

package io.continual.services.model.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable;

/**
 * The ModelLoaderContext provides context to objects being loaded into memory and is
 * used in the model object JSON deserializers.
 */
public class ModelLoaderContext
{
	public ModelLoaderContext ( ServiceContainer sc, NvReadable settings )
	{
		fSettings = settings;
		fContainer = sc;
	}

	/**
	 * Get the settings for this service.
	 * @return a settings object
	 */
	public NvReadable getSettings () { return fSettings; }

	/**
	 * Get the service container that holds this service
	 * @return a service container
	 */
	public ServiceContainer getServiceContainer () { return fContainer; }

	/**
	 * Get a service based on the given setting name for it. (Note that it's not the name of
	 * the service being presented -- it's the name of a setting that says which service to use.)
	 * 
	 * @param settingName
	 * @param asClass
	 * @return a service instance
	 */
	public synchronized <T> T getServiceBySettingName ( String settingName, Class<T> asClass )
	{
		try
		{
			final String name = fSettings.getString ( settingName );
			return fContainer.get ( name, asClass );
		}
		catch ( NvReadable.MissingReqdSettingException e )
		{
			log.error ( "Missing service name setting " + settingName + "." );
			throw new RuntimeException ( "This server is misconfigured.", e );
		}
	}

	private final NvReadable fSettings;
	private final ServiceContainer fContainer;
	private static final Logger log = LoggerFactory.getLogger ( ModelLoaderContext.class );
}
