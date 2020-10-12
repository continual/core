package io.continual.messaging.impl;

import java.util.Collection;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.services.ServiceContainer;

public class LoggerPublisher implements ContinualMessagePublisher
{
	public LoggerPublisher ()
	{
	}

	public LoggerPublisher ( JSONObject config )
	{
	}

	public LoggerPublisher ( ServiceContainer sc, JSONObject config )
	{
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void close ()
	{
	}

	@Override
	public ContinualMessageSink getTopic ( String topic ) throws TopicUnavailableException
	{
		return new ContinualMessageSink ()
		{
			@Override
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs )
			{
				for ( ContinualMessage msg : msgs )
				{
					log.info ( topic + " (" + stream.getName () + "): " + msg.getMessagePayload ().toString () );
				}
			}
		};
	}

	private static final Logger log = LoggerFactory.getLogger ( LoggerPublisher.class );
}
