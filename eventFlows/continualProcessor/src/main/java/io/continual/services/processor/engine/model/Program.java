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

package io.continual.services.processor.engine.model;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import io.continual.services.processor.service.ProcessingService;

/**
 * A program contains sources, pipelines, and sinks.
 */
public class Program
{
	public static JSONObject config () { return new JSONObject(); }

	/**
	 * Construct an empty program.
	 */
	public Program ()
	{
		fSources = new HashMap<> ();
		fSinks = new HashMap<> ();
		fPipelines = new HashMap<> ();
	}

	/**
	 * Add a message source to this program.  
	 * @param name 
	 * @param src
	 * @return this program
	 */
	public Program addSource ( String name, Source src )
	{
		fSources.put ( name, new SourceInfo ( src ) );
		return this;
	}

	/**
	 * Add service to a given source.
	 * @param srcName
	 * @param service
	 * @return this program
	 */
	public Program addServiceToSource ( String srcName, String svcName, ProcessingService service )
	{
		final SourceInfo si = fSources.get ( srcName );
		if ( si == null )
		{
			throw new IllegalStateException ( "There's no source named " + srcName + " in this program." );
		}

		si.addService ( svcName, service );

		return this;
	}

	/**
	 * Get the labeled sources in this program
	 * @return a map of sources
	 */
	public Map<String,Source> getSources ()
	{
		final HashMap<String,Source> map = new HashMap<> ();
		for ( Map.Entry<String,SourceInfo> entry : fSources.entrySet () )
		{
			map.put ( entry.getKey (), entry.getValue ().getSource () );
		}
		return map;
	}

	/**
	 * Get the services for a given source
	 * @param srcName
	 * @return a list of 0 or more services
	 */
	public Map<String,ProcessingService> getServicesFor ( String srcName )
	{
		final SourceInfo si = fSources.get ( srcName );
		if ( si == null )
		{
			return new HashMap<> ();
		}
		return si.getServices ();
	}

	/**
	 * Add a sink to this program. 
	 * @param name
	 * @param sink
	 * @return this program
	 */
	public Program addSink ( String name, Sink sink )
	{
		fSinks.put ( name, sink );
		return this;
	}

	/**
	 * Get the labeled sinks in this program
	 * @return a map of sinks
	 */
	public Map<String,Sink> getSinks ()
	{
		final HashMap<String,Sink> map = new HashMap<> ();
		map.putAll ( fSinks );
		return map;
	}

	/**
	 * Add a pipeline to this program. 
	 * @param name
	 * @param pipeline
	 * @return this program
	 */
	public Program addPipeline ( String name, Pipeline pipeline )
	{
		fPipelines.put ( name, pipeline );
		return this;
	}

	/**
	 * Get a pipeline by name.
	 * @param plName
	 * @return a pipeline or null
	 */
	public Pipeline getPipeline ( String plName )
	{
		return fPipelines.get ( plName );
	}

	private static class SourceInfo
	{
		public SourceInfo ( Source src )
		{
			fSrc = src;
			fServices = new HashMap<> ();
		}

		public void addService ( String svcName, ProcessingService service )
		{
			fServices.put ( svcName, service );
		}

		public Source getSource ( ) { return fSrc; }
		public Map<String,ProcessingService> getServices () { return fServices; }

		private final Source fSrc;
		private final HashMap<String,ProcessingService> fServices;
	}

	private final HashMap<String,SourceInfo> fSources;
	private final HashMap<String,Sink> fSinks;
	private final HashMap<String,Pipeline> fPipelines;
}
