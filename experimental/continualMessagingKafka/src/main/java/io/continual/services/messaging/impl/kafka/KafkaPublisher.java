package io.continual.services.messaging.impl.kafka;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageStream;
import io.continual.messaging.MessagePublishException;
import io.continual.services.ServiceContainer;

/**
 * Kafka publisher
 */
public class KafkaPublisher implements ContinualMessagePublisher
{
	public KafkaPublisher ( ServiceContainer sc, JSONObject config ) throws IOException
	{
		final Properties props = new Properties ();
		transfer ( config, props, "bootstrap.servers" );
		transfer ( config, props, "acks", "all" );
		transfer ( config, props, "retries", 0 );
		transfer ( config, props, "batch.size", 16384);
		transfer ( config, props, "linger.ms", 1);
		transfer ( config, props, "buffer.memory", 33554432);
		transfer ( config, props, "key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		transfer ( config, props, "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		fProducer = new KafkaProducer<> ( props );
	}

	@Override
	public ContinualMessageSink getTopic ( final String topic ) throws TopicUnavailableException
	{
		return new ContinualMessageSink ()
		{
			@Override
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs ) throws MessagePublishException
			{
				for ( ContinualMessage msg : msgs )
				{
					final String partition = stream.getName ();
					final String payload = msg.getMessagePayload ().toString ();
					log.debug  ( "To Kafka ("+ topic + " / " + partition + "): " + payload );
					fProducer.send ( new ProducerRecord<String,String> ( topic.toString (), partition, payload ) );
				}
			}
		};
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void close () throws IOException
	{
		fProducer.close ();
	}

	private final KafkaProducer<String,String> fProducer;

	private static void transfer ( JSONObject from, Properties to, String toKey )
	{
		transfer ( from, to, toKey, null );
	}

	private static void transfer ( JSONObject from, Properties to, String toKey, String def )
	{
		transfer ( from, "kafka." + toKey, to, toKey, def );
	}

	private static void transfer ( JSONObject from, Properties to, String toKey, int def )
	{
		transfer ( from, "kafka." + toKey, to, toKey, Integer.toString ( def ) );
	}

	private static void transfer ( JSONObject from, String fromKey, Properties to, String toKey, String def )
	{
		final String val = from.optString ( fromKey, def );
		if ( val != null )
		{
			log.info ( "kafka: " + toKey + "=" + val );
			to.put ( toKey, val );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( KafkaPublisher.class );
}
