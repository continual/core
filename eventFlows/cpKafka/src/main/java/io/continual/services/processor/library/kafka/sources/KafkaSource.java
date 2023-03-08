package io.continual.services.processor.library.kafka.sources;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class KafkaSource extends BasicSource
{
	public KafkaSource ( final ConfigLoadContext sc, JSONObject config )
	{
		super ( config );

		final ExpressionEvaluator ee = sc.getServiceContainer ().getExprEval ();
		final JSONObject cc = ee.evaluateJsonObject ( config );

		final String topic = cc.getString ( "topic" );
		final String group = cc.getString ( "group" );
		
		fProps = new Properties ();
		readConfigInto ( config.optJSONObject ( "kafka" ), fProps, ee );
		fProps.put ( "group.id", topic + "::" + group );
		fProps.put ( "client.id", UUID.randomUUID ().toString () );
		fProps.put ( "enable.auto.commit", cc.optBoolean ( "autoCommit", true ) );
		fProps.put ( ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class );
		fProps.put ( ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class );

		fConsumer = new KafkaConsumer<> ( fProps );

		fConsumer.subscribe ( Arrays.asList ( topic ) );

		fPendingMsgs = new LinkedList<> ();
	}

	@Override
	public synchronized void close () throws IOException
	{
		noteEndOfStream ();

		fConsumer.close ();
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		if ( fPendingMsgs.size () > 0 )
		{
			return fPendingMsgs.remove ();
		}
		
		final ConsumerRecords<String,String> records = fConsumer.poll ( 100L );
		for ( ConsumerRecord<String,String> cr : records )
		{
			final String msgStr = cr.value ();
			try
			{
				final JSONObject msgData = new JSONObject ( new CommentedJsonTokener ( msgStr ) );
				final Message msg = Message.adoptJsonAsMessage ( msgData );
				fPendingMsgs.add ( makeDefRoutingMessage ( msg ) );
			}
			catch ( JSONException x )
			{
				spc.warn ( "Couldn't parse inbound text as JSON: " + msgStr );
			}
		}

		if ( fPendingMsgs.size () > 0 )
		{
			return fPendingMsgs.remove ();
		}
		return null;
	}

	private final Properties fProps;
	private final KafkaConsumer<String, String> fConsumer;
	private final LinkedList<MessageAndRouting> fPendingMsgs;

	private void readConfigInto ( JSONObject config, Properties props, ExpressionEvaluator ee )
	{
		if ( config == null ) return;

		JsonVisitor.forEachElement ( config, new ObjectVisitor<Object,JSONException>()
		{
			@Override
			public boolean visit ( String key, Object value ) throws JSONException
			{
				// ignore sub-objects and arrays; they're type specific overrides
				if ( value == null || value instanceof JSONObject || value instanceof JSONArray )
				{
					return true;
				}

				props.put ( key, ee.evaluateText ( value.toString () ) );

				return true;
			}
		} );

	}
}
