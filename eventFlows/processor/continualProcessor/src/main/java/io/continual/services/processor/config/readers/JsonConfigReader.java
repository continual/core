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

package io.continual.services.processor.config.readers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.resources.ResourceLoader;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.library.filters.Any;
import io.continual.services.processor.engine.library.processors.Set;
import io.continual.services.processor.engine.library.sinks.NullSink;
import io.continual.services.processor.engine.library.sources.NullSource;
import io.continual.services.processor.engine.model.Filter;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Processor;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Rule;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.service.ProcessingService;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class JsonConfigReader implements ConfigReader
{
	/**
	 * Read a program from a set of named resources containing JSON configurations
	 * @param resNames
	 * @return a program
	 * @throws ConfigReadException
	 */
	@Override
	public Program read ( String[] resNames ) throws ConfigReadException
	{
		try
		{
			final Program p = new Program ();
			final ServiceContainer sc = new ServiceContainer (); 
			for ( String resName : resNames )
			{
				final InputStream res = ResourceLoader.load ( resName );
				if ( res == null )
				{
					throw new ConfigReadException ( "Couldn't locate [" + resName + "]." );
				}
				readInto ( sc, JsonUtil.readJsonObject ( res ), p );
			}
			return p;
		}
		catch ( IOException e )
		{
			throw new ConfigReadException ( e );
		}
	}

	/**
	 * Read a program from an input stream containing a JSON configuration
	 * @param is
	 * @return a program
	 * @throws ConfigReadException
	 */
	@Override
	public Program read ( InputStream is ) throws ConfigReadException
	{
		if ( is == null )
		{
			throw new ConfigReadException ( "Can't read a null stream." );
		}
		return read ( JsonUtil.readJsonObject ( is ) );
	}

	/**
	 * Read a program from a JSON object
	 * @param obj
	 * @return a program
	 * @throws ConfigReadException
	 */
	public Program read ( JSONObject obj ) throws ConfigReadException
	{
		return read ( new ServiceContainer (), obj );
	}

	/**
	 * Read a program from a JSON object
	 * @param sc
	 * @param obj
	 * @return
	 * @throws ConfigReadException
	 */
	public Program read ( ServiceContainer sc, JSONObject obj ) throws ConfigReadException
	{
		if ( obj == null )
		{
			throw new ConfigReadException ( "Can't read a null object." );
		}

		final Program p = new Program ();
		readInto ( sc, obj, p );
		return p;
	}

	public static List<String> getStandardPackages ()
	{
		final ArrayList<String> pkgs = new ArrayList<> ();

		// add standard packages
		pkgs.add ( "io.continual.services.processor.engine.library.filters" );
		pkgs.add ( "io.continual.services.processor.engine.library.processors" );
		pkgs.add ( "io.continual.services.processor.engine.library.sinks" );
		pkgs.add ( "io.continual.services.processor.engine.library.sources" );

		return pkgs;
	}

	public void readInto ( ServiceContainer sc, JSONObject programJson, Program p ) throws ConfigReadException
	{		
		final ArrayList<String> pkgs = new ArrayList<> ();
		pkgs.addAll ( getStandardPackages() );

		// read program packages
		JsonVisitor.forEachElement ( programJson.optJSONArray ( "packages" ), new ArrayVisitor<String,ConfigReadException> ()
		{
			@Override
			public boolean visit ( String pkgName ) throws ConfigReadException
			{
				pkgs.add ( pkgName + ".services" );
				pkgs.add ( pkgName + ".filters" );
				pkgs.add ( pkgName + ".processors" );
				pkgs.add ( pkgName + ".sources" );
				pkgs.add ( pkgName + ".sinks" );
				log.info ( "\twith package {}...", pkgName );
				return true;
			}
		} );

		// build context
		final ConfigLoadContext clc = new ConfigLoadContext ()
		{
			@Override
			public ServiceContainer getServiceContainer ()
			{
				return sc;
			}

			@Override
			public List<String> getSearchPathPackages ()
			{
				return pkgs;
			}
		};

		// read sinks
		JsonVisitor.forEachElement ( programJson.optJSONObject ( "sinks" ), new ObjectVisitor<JSONObject,ConfigReadException> ()
		{
			@Override
			public boolean visit ( String sinkName, JSONObject sink ) throws ConfigReadException
			{
				try
				{
					p.addSink (
						sinkName, 
						Builder.withBaseClass ( Sink.class )
							.withClassNameInData ()
							.searchingPath ( NullSink.class.getPackage ().getName () )
							.searchingPaths ( pkgs )
							.providingContext ( clc )
							.usingData ( clc.getServiceContainer ().getExprEval ().evaluateJsonObject ( sink ) )
							.build ()
					);
					log.info ( "\twith sink {}...", sinkName );
				}
				catch ( BuildFailure | JSONException e )
				{
					throw new ConfigReadException ( e );
				}

				return true;
			}
		} );

		// read pipelines
		JsonVisitor.forEachElement ( programJson.optJSONObject ( "pipelines" ), new ObjectVisitor<JSONArray,ConfigReadException> ()
		{
			@Override
			public boolean visit ( String pipelineName, JSONArray rules ) throws ConfigReadException
			{
				final Pipeline pl = readPipeline ( rules, pkgs, clc );
				p.addPipeline ( pipelineName, pl );
				log.info ( "\twith pipeline {}...", pipelineName );

				return true;
			}
		} );

		// read sources
		JsonVisitor.forEachElement ( programJson.optJSONObject ( "sources" ), new ObjectVisitor<JSONObject,ConfigReadException> ()
		{
			@Override
			public boolean visit ( String srcName, JSONObject source ) throws ConfigReadException
			{
				try
				{
					source.put ( "name", srcName );
					readSource ( clc, p, pkgs, srcName, source );
				}
				catch ( BuildFailure | JSONException e )
				{
					throw new ConfigReadException ( e );
				}
				return true;
			}
		} );
	}

	public static Filter readFilter ( JSONObject fromJson, ConfigLoadContext clc, List<String> pkgs ) throws BuildFailure
	{
		return Builder.withBaseClass ( Filter.class )
			.withClassNameInData ()
			.searchingPath ( Any.class.getPackage ().getName () )
			.searchingPaths ( pkgs )
			.providingContext ( clc )	// FIXME: clc has search path for pkgs... why both?
			.usingData ( clc.getServiceContainer ().getExprEval ().evaluateJsonObject ( fromJson ) )
			.build ()
		;
	}

	public static Pipeline readPipeline ( JSONArray rules, List<String> pkgs, ConfigLoadContext clc ) throws ConfigReadException
	{
		try
		{
			final Pipeline pl = new Pipeline ();

			JsonVisitor.forEachElement ( rules, new ArrayVisitor<JSONObject,ConfigReadException> ()
			{
				@Override
				public boolean visit ( JSONObject rule ) throws ConfigReadException
				{
					try
					{
						boolean isAlways = false;

						// get the filter
						final Filter f;
						final JSONObject ifBlock = rule.optJSONObject ( "if" );
						if ( ifBlock != null )
						{
							f = readFilter ( rule.getJSONObject ( "if" ), clc, pkgs );
						}
						else
						{
							isAlways = true;
							f = new Any ();
						}

						// get THEN and ELSE processing
						final List<Processor> thenSteps = readProcessorArray ( clc, pkgs, rule.optJSONArray ( isAlways ? "always" : "then" ) );
						final List<Processor> elseSteps = isAlways ? new ArrayList<> () : readProcessorArray ( clc, pkgs, rule.optJSONArray ( "else" ) );

						// build the rule and add it to the pipeline
						pl.addRule (
							new Rule.Builder ()
								.checkIf ( f )
								.thenDo ( thenSteps )
								.elseDo ( elseSteps )
								.build ()
						);
					}
					catch ( JSONException | BuildFailure e )
					{
						throw new ConfigReadException ( e );
					}
					return true;
				}
			} );
			return pl;
		}
		catch ( JSONException e )
		{
			throw new ConfigReadException ( e );
		}
	}

	private Source readSource ( ConfigLoadContext clc, Program p, List<String> pkgs, String srcName, JSONObject source ) throws BuildFailure
	{
		final Source src = Builder.withBaseClass ( Source.class )
			.withClassNameInData ()
			.searchingPath ( NullSource.class.getPackage ().getName () )
			.searchingPaths ( clc.getSearchPathPackages () )
			.providingContext ( clc )
			.usingData ( source )
			.build ()
		;

		p.addSource ( srcName, src );
		log.info ( "\twith source {}...", srcName );

		JsonVisitor.forEachElement ( source.optJSONObject ( "services" ), new ObjectVisitor<JSONObject,BuildFailure> ()
		{
			@Override
			public boolean visit ( String serviceName, JSONObject svcBlock ) throws JSONException, BuildFailure
			{
				final ProcessingService ps = Builder.withBaseClass ( ProcessingService.class )
					.withClassNameInData ()
					.searchingPaths ( pkgs )
					.providingContext ( clc )
					.usingData ( clc.getServiceContainer ().getExprEval ().evaluateJsonObject ( svcBlock ) )
					.build ()
				;
				p.addServiceToSource ( srcName, serviceName, ps );
				log.info ( "\t\twith service {}...", serviceName );
				return true;
			}
		} );
		
		return src;
	}
	
	private static List<Processor> readProcessorArray ( ConfigLoadContext clc, List<String> pkgs, JSONArray block ) throws ConfigReadException
	{
		final ArrayList<Processor> result = new ArrayList<> ();
		JsonVisitor.forEachElement ( block, new ArrayVisitor<JSONObject,ConfigReadException> ()
		{
			@Override
			public boolean visit ( JSONObject processorBlock ) throws ConfigReadException
			{
				try
				{
					result.add (
						Builder.withBaseClass ( Processor.class )
							.withClassNameInData ()
							.searchingPath ( Set.class.getPackage ().getName () )
							.searchingPaths ( pkgs )
							.providingContext ( clc )
							.usingData ( clc.getServiceContainer ().getExprEval ().evaluateJsonObject ( processorBlock ) )
							.build ()
					);
				}
				catch ( JSONException | BuildFailure e )
				{
					throw new ConfigReadException ( e );
				}
				return true;
			}
		} );
		return result;
	}

	private static final Logger log = LoggerFactory.getLogger ( JsonConfigReader.class );
}
