package io.continual.monitor;

import org.json.JSONObject;
import org.slf4j.Logger;

import io.continual.messaging.ContinualMessageSink;

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
		 * Get a writeable data space for this monitor instance, which is
		 * persistent across runs.
		 * @return a data object
		 */
		JSONObject getData ();
	};
	
	/**
	 * Run the monitoring task and report results to the client provided.
	 * @param monctx
	 */
	void run ( MonitorContext monctx );

}
