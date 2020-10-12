package io.continual.monitor.daemon;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.client.ClientBuilders;
import io.continual.client.events.EventClient;
import io.continual.client.events.EventClient.EventServiceException;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.messaging.MessagePublishException;
import io.continual.monitor.ContinualMonitor;
import io.continual.monitor.ContinualMonitor.MonitorContext;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.nv.NvReadable;

// note: must be public for quartz to access
public class ContinualMonitorWrapper implements Job, InterruptableJob
{
	@Override
	@SuppressWarnings("unchecked")
	public synchronized void execute ( JobExecutionContext context ) throws JobExecutionException
	{
		final ContinualMonitor theMonitor;

		final JSONObject json = JsonUtil.readJsonObject ( context.getJobDetail ().getJobDataMap ().getString ( "jobJson" ) );
		try
		{
			theMonitor = Builder
				.withBaseClass ( ContinualMonitor.class )
				.usingData ( json )
				.withClassNameInData ()
				.searchingPath ( "by.neb.monitors" )
				.build ()
			;
		}
		catch ( BuildFailure e )
		{
			// remove this job because we can't build it
			log.error ( "Failed to build job: " + e.getMessage () );
			cancelJob ( context, e );
			return;
		}

		final HashMap<String,JSONObject> sharedData;
		try
		{
			sharedData = (HashMap<String,JSONObject>) context.getScheduler ().getContext ().get ( "sharedData" );
		}
		catch ( SchedulerException e )
		{
			// remove this job because we can't build it
			log.error ( "Failed to get system shared data: " + e.getMessage () );
			cancelJob ( context, e );
			return;
		}

		final JSONObject monitorConfig;
		try
		{
			final Object settings = context.getScheduler ().getContext ().get ( "systemSettings" );
			if ( settings instanceof NvReadable )
			{
				final NvReadable so = ((NvReadable) settings);
				monitorConfig = makeConfig ( so, json );
			}
			else
			{
				// remove this job because we can't build it
				log.error ( "System settings are not available." );
				cancelJob ( context, new ClassCastException () );
				return;
			}
		}
		catch ( SchedulerException e )
		{
			// remove this job because we can't build it
			log.error ( "Failed to get system settings: " + e.getMessage () );
			cancelJob ( context, e );
			return;
		}

		final EventClient fClient;
		try
		{
			// instantiate the nebby api client
			fClient = new ClientBuilders.EventClientBuilder ()
//				.usingHost ( fSettings.getString ( "CIO_EVENT_API", null ) )
				.asUser ( monitorConfig.optString ( "CIO_USER", "" ), monitorConfig.optString ( "CIO_PASSWORD", "" ) )
				.usingApiKey ( monitorConfig.optString ( "CIO_APIKEY", "" ), monitorConfig.optString ( "CIO_APISECRET", "" ) )
				.build ()
			;
		}
		catch ( MalformedURLException e )
		{
			// remove this job because we can't build it
			log.error ( "Failed to build job's client: " + e.getMessage () );
			cancelJob ( context, e );
			return;
		}

		final ContinualMessageSink ces = new ContinualMessageSink ()
		{
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> events ) throws MessagePublishException
			{
				for ( ContinualMessage event : events )
				{
					try
					{
						fClient.send ( event.toJson (), "events", stream.getName () );
					}
					catch ( EventServiceException e )
					{
						throw new MessagePublishException ( e );
					}
					catch ( IOException e )
					{
						throw new MessagePublishException ( e );
					}
				}
			}
		};
		
		final Logger monitorLog = LoggerFactory.getLogger ( "by.neb.monitor" );

		// run the monitor
		final String name = json.optString ( "name", "(anonymous)" );
		monitorLog.info ( "starting [" + name + "]" );
		theMonitor.run ( new MonitorContext () {

			@Override
			public ContinualMessageSink getEventSink () { return ces; }

			@Override
			public Logger getLog () { return monitorLog; }

			@Override
			public JSONObject getSettings () { return monitorConfig; }

			@Override
			public JSONObject getData ()
			{
				final String name = json.getString ( "name" );
				JSONObject data = sharedData.get ( name );
				if ( data == null )
				{
					data = new JSONObject ();
					sharedData.put ( name, data );
				}
				return data;
			}
		} );
		monitorLog.info ( "completed [" + name + "]" );
	}

	@Override
	public void interrupt () throws UnableToInterruptJobException
	{
	}

	public static JSONObject makeConfig ( NvReadable systemSettings, JSONObject monConfig )
	{
		final JSONObject monitorConfig = new JSONObject ();
		for ( Map.Entry<String,String> e : systemSettings.getCopyAsMap ().entrySet () )
		{
			monitorConfig.put ( e.getKey (), e.getValue () );
		}
		JsonUtil.copyInto ( monConfig, monitorConfig );
		return monitorConfig;
	}
	
	private void cancelJob ( JobExecutionContext context, Throwable e ) throws JobExecutionException
	{
		try
		{
			context.getScheduler ().deleteJob ( context.getJobDetail ().getKey () );
		}
		catch ( SchedulerException e1 )
		{
			log.error ( "Couldn't remove job from scheduler after build failure: " + e1.getMessage () );
		}
		throw new JobExecutionException ( e );
	}

	private static final Logger log = LoggerFactory.getLogger ( ContinualMonitorWrapper.class );
}
