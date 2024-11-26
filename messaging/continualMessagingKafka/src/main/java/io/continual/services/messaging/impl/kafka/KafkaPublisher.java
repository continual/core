package io.continual.services.messaging.impl.kafka;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.messaging.ContinualMessage;
import io.continual.messaging.ContinualMessagePublisher;
import io.continual.messaging.ContinualMessageSink;
import io.continual.messaging.ContinualMessageSink.AckType;
import io.continual.messaging.ContinualMessageStream;
import io.continual.messaging.MessagePublishException;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

/**
 * Kafka publisher
 */
public class KafkaPublisher extends SimpleService implements ContinualMessagePublisher
{
	public KafkaPublisher ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		final JSONObject config = sc.getExprEval ().evaluateJsonObject ( rawConfig );

		// read properties
		final Properties props = new Properties ( skDefaultProps );
		JsonVisitor.forEachElement ( config.optJSONObject ( "kafka" ), new ObjectVisitor<Object,BuildFailure> ()
		{
			@Override
			public boolean visit ( String key, Object t ) throws JSONException, BuildFailure
			{
				props.put ( key, t.toString () );
				return true;
			}
		} );

		fProducers = new HashMap<> ();

		{
			final Properties localProps = new Properties ( props );
			localProps.put ( "acks", -1 );
			fProducers.put ( AckType.ALL, new KafkaProducer<> ( localProps ) );
		}

		{
			final Properties localProps = new Properties ( props );
			localProps.put ( "acks", 1 );
			fProducers.put ( AckType.MINIMAL, new KafkaProducer<> ( localProps ) );
		}

		{
			final Properties localProps = new Properties ( props );
			localProps.put ( "acks", 1 );
			fProducers.put ( AckType.NONE, new KafkaProducer<> ( localProps ) );
		}
	}

	@Override
	public ContinualMessageSink getTopic ( final String topic ) throws TopicUnavailableException
	{
		return new ContinualMessageSink ()
		{
			@Override
			public void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs, AckType acks ) throws MessagePublishException
			{
				final KafkaProducer<String, String> producer = fProducers.get ( acks );
				for ( ContinualMessage msg : msgs )
				{
					final String partition = stream.getName ();
					final String payload = msg.toJson ().toString ();
					log.info  ( "To Kafka [{}/{}] with {} acks: {}", topic, partition, acks, payload );

					producer.send ( new ProducerRecord<String,String> ( topic.toString (), partition, payload ) );
				}
				producer.flush ();
			}
		};
	}

	@Override
	public void flush ()
	{
		for ( KafkaProducer<String, String> producer : fProducers.values () )
		{
			producer.flush ();
		}
	}

	@Override
	public void close () throws IOException
	{
		for ( KafkaProducer<String, String> producer : fProducers.values () )
		{
			producer.close ();
		}
	}

	private final HashMap<AckType,KafkaProducer<String,String>> fProducers;

	private static final Logger log = LoggerFactory.getLogger ( KafkaPublisher.class );

	private static final Properties skDefaultProps = new Properties ();
	static
	{
		skDefaultProps.put ( "acks", "all" );
		skDefaultProps.put ( "batch.size", 16384 );
		skDefaultProps.put ( "linger.ms", 1 );
		skDefaultProps.put ( "buffer.memory", 33554432 );
		skDefaultProps.put ( "key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		skDefaultProps.put ( "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	}
}
