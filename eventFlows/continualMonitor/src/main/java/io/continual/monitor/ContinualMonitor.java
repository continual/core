package io.continual.monitor;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.continual.messaging.ContinualMessageSink;
import io.continual.services.model.core.Model;

/**
 * A very general monitor that examines its target and reports status
 * to the Continual event receiver.
 * 
 * @author peter
 */
public interface ContinualMonitor
{
	/**
	 * Context for the monitor
	 */
	public interface MonitorContext
	{
		/**
		 * Get the event sink to push events into
		 * @return an event sink
		 */
		ContinualMessageSink getEventSink ();

		/**
		 * Get a log to write into
		 * @return a log
		 */
		Logger getLog ();

		/**
		 * Get the configuration for this monitor.
		 * @return a configuration object
		 */
		JSONObject getSettings ();

		/**
		 * Get a model mount for this monitor, which provides a place to read and write data
		 * that's persistent across runs.
		 * @return a data object
		 */
		Model getModel ();
	};
	
	/**
	 * Run the monitoring task and report results to the message sink.
	 * @param context
	 */
	void run ( MonitorContext context );
}
