package io.continual.services.messaging.impl.kafka;

import java.io.IOException;
import java.util.Collection;
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
		
		// setup props with some defaults
		final Properties props = new Properties ();
		props.put ( "acks", "all" );
		props.put ( "retries", 0 );
		props.put ( "batch.size", 16384);
		props.put ( "linger.ms", 1);
		props.put ( "buffer.memory", 33554432);
		props.put ( "key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put ( "value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		// transfer into props
		JsonVisitor.forEachElement ( config.optJSONObject ( "kafka" ), new ObjectVisitor<Object,BuildFailure> ()
		{
			@Override
			public boolean visit ( String key, Object t ) throws JSONException, BuildFailure
			{
				props.put ( key, t.toString () );
				return true;
			}
		} );

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
					final String payload = msg.toJson ().toString ();
					log.info  ( "To Kafka ("+ topic + " / " + partition + "): " + payload );
					fProducer.send ( new ProducerRecord<String,String> ( topic.toString (), partition, payload ) );
				}
				fProducer.flush ();
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
	private static final Logger log = LoggerFactory.getLogger ( KafkaPublisher.class );
}
