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

package io.continual.services.processor.library.model.sources;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.model.common.ModelConnector;
import io.continual.services.processor.library.model.common.ObjectFetcher;

public class ModelSource extends ModelConnector implements Source
{
	public ModelSource ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( modelFromConfig ( sc, config.getJSONObject ( "model" ) ), sc, config );
	}

	public ModelSource ( Model model, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( model );

		fFetcher = Builder.withBaseClass ( ObjectFetcher.class )
			.withClassNameInData ()
			.usingData ( new BuilderJsonDataSource ( config.getJSONObject ( "objectSetSpec" ) ) )
			.providingContext ( sc.getServiceContainer () )
			.build ()
		;

		fPipeline = config.getString ( "pipeline" );
	}

	@Override
	public boolean isEof () throws IOException
	{
		return fFetcher.isEof ();
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, long waitAtMost, TimeUnit waitAtMostTimeUnits ) throws IOException, InterruptedException
	{
		try
		{
			return fFetcher.getNextMessage ( spc, getModel(), waitAtMost, waitAtMostTimeUnits, fPipeline );
		}
		catch ( ModelRequestException | ModelServiceException x )
		{
			spc.warn ( "Couldn't fetch model objects. " + x.getMessage () );
		}
		return null;
	}

	@Override
	public void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// not supported
	}

	@Override
	public void requeue ( MessageAndRouting msgAndRoute )
	{
		// not supported
	}

	@Override
	public void close ()
	{
		// ignore
	}

	private final String fPipeline;
	private final ObjectFetcher fFetcher;
}
