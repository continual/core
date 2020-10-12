package io.continual.monitor.testing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.client.ClientBuilders.EventClientBuilder;
import io.continual.client.events.EventClient;
import io.continual.client.events.EventClient.EventServiceException;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.monitor.ContinualMonitor;
import io.continual.monitor.ContinualMonitor.MonitorContext;
import io.continual.monitor.daemon.ContinualMonitorWrapper;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

/**
 * A monitor that runs and exits. Generally you'd run this on an OS
 * schedule.
 */
public class SimpleMonitorRunner extends ConfiguredConsole
{
	protected ContinualMessageSink buildSink ( NvReadable p, CmdLinePrefs cmdLine, Logger monitorLog ) throws BuildFailure
	{
		try
		{
			// instantiate the event api client
			final EventClient client = new EventClientBuilder ()
				.asUser ( p.getString ( "CIO_USER", "" ), p.getString ( "CIO_PASSWORD", "" ) )
				.usingApiKey ( p.getString ( "CIO_APIKEY", "" ), p.getString ( "CIO_APISECRET", "" ) )
				.build ();

			if ( cmdLine.getFileArguments ().size () < 1 )
			{
				throw new BuildFailure ( "No job info provided on command line." );
			}

			// wrap in event sink interface
			final String onTopic = "??";	// FIXME
			final ContinualMessageSink ces = new ContinualMessageSink ()
			{
				@Override
				public void send ( ContinualMessageStream stream, Collection<ContinualMessage> events )
				{
					try
					{
						for ( ContinualMessage e : events )
						{
							client.send ( e.toJson (), onTopic, stream.getName () );
						}
					}
					catch ( EventServiceException | IOException e )
					{
						monitorLog.error ( e.getMessage () );
					}
				}
			};
			return ces;
		}
		catch ( MalformedURLException e )
		{
			throw new BuildFailure ( e );
		}
	}
	
	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs cmdLine ) throws StartupFailureException
	{
		final Logger monitorLog = LoggerFactory.getLogger ( "io.continual.monitor" );
		try
		{
			preRun ( p, cmdLine );

			final ContinualMessageSink ces = buildSink ( p, cmdLine, monitorLog );

			// instantiate the monitor object
			final String jobInfo = cmdLine.getFileArguments ().firstElement ();
			final JSONObject data = JsonUtil.readJsonObject ( jobInfo );
			final ContinualMonitor mon = Builder
				.withBaseClass ( ContinualMonitor.class )
				.withClassNameInData ()
				.usingData ( data )
				.build ()
			;

			final JSONObject config = ContinualMonitorWrapper.makeConfig ( p, data );

			// run the monitor
			final MonitorContext ctx = new MonitorContext () {

				@Override
				public ContinualMessageSink getEventSink () { return ces; }

				@Override
				public Logger getLog () { return monitorLog; }

				@Override
				public JSONObject getSettings () { return config; }

				@Override
				public JSONObject getData () { return data; }
			};
			mon.run ( ctx );
			postRun ( mon, ctx );

			return null;
		}
		catch ( BuildFailure e )
		{
			monitorLog.error ( "Didn't start: " + e.getMessage (), e );
			throw new StartupFailureException ( e );
		}
	}

	protected void preRun ( NvReadable p, CmdLinePrefs cmdLine ) throws BuildFailure
	{
	}

	protected void postRun ( ContinualMonitor mon, MonitorContext ctx )
	{
	}

	@Override
	protected void cleanup ()
	{
	}

	public static void main ( String[] args ) throws UsageException, LoadException, MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		new SimpleMonitorRunner().runFromMain ( args );
	}
}
