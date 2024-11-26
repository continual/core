package io.continual.messaging.impl;

import java.util.Collection;

import org.json.JSONObject;

import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.services.ServiceContainer;

public class DevNullPublisher implements ContinualMessagePublisher
{
	public DevNullPublisher ()
	{
	}

	public DevNullPublisher ( JSONObject settings )
	{
	}

	public DevNullPublisher ( ServiceContainer sc, JSONObject settings )
	{
	}

	@Override
	public ContinualMessageSink getTopic ( String topic ) throws TopicUnavailableException
	{
		return new ContinualMessageSink ()
		{
			@Override
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs, AckType acks )
			{
				// no-op
			}
		};
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void close ()
	{
	}
}
