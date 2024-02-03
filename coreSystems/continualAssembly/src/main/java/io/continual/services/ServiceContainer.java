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

package io.continual.services;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.util.collections.MultiMap;
import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.JsonDataSource;
import io.continual.util.data.exprEval.SpecialFnsDataSource;
import io.continual.util.nv.NvReadable;

public class ServiceContainer
{
	public static final String kServices = "services";
	public static final String kServicesChar = "s";

	public static final String kProfile = "profile";
	public static final String kProfileChar = "p";

	public static ServiceContainer build ( NvReadable p, boolean withStart ) throws StartupFailureException
	{
		return build ( p, withStart, new Server.StdFactory () );
	}

	public static <T extends ServiceContainer> T build ( NvReadable p, boolean withStart, ServiceContainerFactory<T> scf ) throws StartupFailureException
	{
		final String services = p.getString ( kServices, "services.json" );
		if ( services == null )
		{
			throw new StartupFailureException ( "No services configuration name provided." );
		}

		final InputStream serviceStream = ServiceContainer.class.getClassLoader ().getResourceAsStream ( services );
		if ( serviceStream == null )
		{
			throw new StartupFailureException ( "No service stream available." );
		}

		return build ( serviceStream, p.getStrings ( kProfile, new String[]{"default"} ), withStart, scf );
	}

	public static ServiceContainer build ( InputStream serviceStream, String[] profiles, boolean withStart ) throws StartupFailureException
	{
		return build ( serviceStream, profiles, withStart, new Server.StdFactory () );
	}

	public static <T extends ServiceContainer> T build ( InputStream serviceStream, String[] profiles, boolean withStart, ServiceContainerFactory<T> scf ) throws StartupFailureException
	{
		final T svcContainer = scf.create ();

		if ( serviceStream == null )
		{
			throw new StartupFailureException ( "No stream provided" );
		}

		// read the config
		final ServiceSet tlc = ServiceSet.readConfig ( new InputStreamReader ( serviceStream ) );

		// apply any profiles
		for ( String profile : profiles )
		{
			log.info ( "Profile [" + profile + "] is active." );
			tlc.applyProfile ( profile );
		}

		// load the services
		for ( ServiceConfig sc : tlc.getServices () )
		{
			if ( sc.enabled () )
			{
				log.info ( "Service [" + sc.getName() + "] is enabled..." );
				try
				{
					final Service s = Builder.withBaseClass ( Service.class )
						.usingClassName ( sc.getClassname () )
						.usingData ( new BuilderJsonDataSource ( sc.toJson() ) )
						.providingContext ( svcContainer )
						.build ();
					svcContainer.add ( sc.getName (), s );
				}
				catch ( BuildFailure e )
				{
					throw new StartupFailureException ( e );
				}
			}
			else
			{
				log.info ( "Service [" + sc.getName() + "] is disabled." );
			}
		}

		try
		{
			if ( withStart )
			{
				// startup the services
				log.info ( "Starting services..." );
				svcContainer.startAll ();
				log.info ( "Server is ready." );
			}
		}
		catch ( Service.FailedToStart e )
		{
			throw new StartupFailureException ( e );
		}

		return svcContainer;
	}
	
	public ServiceContainer ()
	{
		fServices = new LinkedList<Service> ();
		fServiceByName = new MultiMap<String,Service> ();
	}

	public synchronized ServiceContainer add ( String name, Service s )
	{
		fServices.add ( s );
		if ( name != null )
		{
			final int count = fServiceByName.size ( name );
			if ( count > 0 )
			{
				log.warn ( "While adding service [{}], {} instances are already present.", name, count );
			}
			fServiceByName.put ( name, s );
		}
		return this;
	}

	public synchronized List<String> getServiceNames ()
	{
		return new LinkedList<String> ( fServiceByName.getKeys () );
	}

	public synchronized List<Service> getServices ()
	{
		return new LinkedList<Service> ( fServices );
	}

	@SuppressWarnings("unchecked")
	public synchronized <T> T get ( String name, Class<T> asClass )
	{
		final List<Service> svcs = fServiceByName.get ( name );
		if ( svcs != null )
		{
			for ( Service svc : svcs )
			{
				if ( asClass.isInstance ( svc ) )
				{
					return (T) svc;
				}
			}
		}
		return null;
	}

	/**
	 * Get a named service, throwing BuildFailure if it's not found.
	 * @param <T> the target class
	 * @param name the service name
	 * @param asClass the target class
	 * @return an instance of the target class
	 * @throws BuildFailure
	 */
	public synchronized <T> T getReqd ( String name, Class<T> asClass ) throws BuildFailure
	{
		T result = get ( name, asClass );
		if ( result == null )
		{
			throw new BuildFailure ( "Couldn't location a " + asClass.getName () + " named " + name );
		}
		return result;
	}

	public void startAll () throws Service.FailedToStart
	{
		try
		{
			// startup the services
			for ( Entry<String, List<Service>> svcEntry : fServiceByName.getValues().entrySet() )
			{
				log.info ( "Starting service [{}]...", svcEntry.getKey () );
				for ( Service svc : svcEntry.getValue() )
				{
					svc.start ();
				}
			}
		}
		catch ( Service.FailedToStart e )
		{
			stopAll ();
			throw e;
		}
	}

	public void stopAll ()
	{
		// stop the services
		for ( Service svc : getServices () )
		{
			svc.requestFinish ();
		}
	}

	public void awaitTermination () throws InterruptedException
	{
		int runCount = Integer.MAX_VALUE;
		while ( runCount > 0 )
		{
			// service implementation is wide-open, so there's not a good way
			// to enforce a signal to wait on. Here, we just poll.
			Thread.sleep ( 250 );

			runCount = 0;
			for ( Service s : getServices () )
			{
				if ( s.isRunning () ) runCount++;
			}
		}
	}

	public ExpressionEvaluator getExprEval ()
	{
		return getExprEval ( null );
	}
	
	public ExpressionEvaluator getExprEval ( JSONObject data )
	{
		final ExprDataSourceStack stack = new ExprDataSourceStack (
			new JsonDataSource ( data ),
			new EnvDataSource (),
			new SpecialFnsDataSource ()
		);
		return new ExpressionEvaluator ( stack );
	}

	private final LinkedList<Service> fServices;
	private final MultiMap<String,Service> fServiceByName;

	private static final Logger log = LoggerFactory.getLogger ( ServiceContainer.class );
}
