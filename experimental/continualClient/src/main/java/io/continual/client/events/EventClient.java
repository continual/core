package io.continual.client.events;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

public interface EventClient
{
	static class EventServiceException extends Exception
	{
		public EventServiceException ( String msg ) { super(msg); }
		public EventServiceException ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Send data to the event receiver
	 * @param data
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONObject data ) throws EventServiceException, IOException;

	/**
	 * Send data to the event receiver
	 * @param data
	 * @param onTopic
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONObject data, String onTopic ) throws EventServiceException, IOException;

	/**
	 * Send data to the event receiver
	 * @param data
	 * @param onTopic
	 * @param onPartition
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONObject data, String onTopic, String onPartition ) throws EventServiceException, IOException;

	/**
	 * Send data to the event receiver
	 * @param data
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONArray data ) throws EventServiceException, IOException;

	/**
	 * Send data to the event receiver
	 * @param data
	 * @param onTopic
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONArray data, String onTopic ) throws EventServiceException, IOException;

	/**
	 * Send data to the event receiver
	 * @param data
	 * @param onTopic
	 * @param onPartition
	 * @throws EventServiceException
	 * @throws IOException
	 */
	void send ( JSONArray data, String onTopic, String onPartition ) throws EventServiceException, IOException;
}
