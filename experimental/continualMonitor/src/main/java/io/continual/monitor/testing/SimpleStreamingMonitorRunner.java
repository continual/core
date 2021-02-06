package io.continual.monitor.testing;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.continual.builder.Builder.BuildFailure;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

/**
 * A monitor that runs and exits. Generally you'd run this on an OS
 * schedule.
 */
public class SimpleStreamingMonitorRunner extends SimpleMonitorRunner
{
	public SimpleStreamingMonitorRunner ()
	{
		fEvents = new LinkedList<> ();
	}

	public List<JSONObject> getEvents ()
	{
		return fEvents;
	}

	@Override
	protected ContinualMessageSink buildSink ( NvReadable p, CmdLinePrefs cmdLine, Logger monitorLog ) throws BuildFailure
	{
		return new ContinualMessageSink ()
		{
			@Override
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> events )
			{
				for ( ContinualMessage e : events )
				{
					fEvents.add ( e.toJson () );
				}
			}
		};
	}

	private final LinkedList<JSONObject> fEvents;
	
	public static void main ( String[] args ) throws UsageException, LoadException, MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		new SimpleStreamingMonitorRunner().runFromMain ( args );
	}
}
