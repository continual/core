package io.continual.services.processor.library.kafka.sources;

import io.continual.notify.ContinualAlertAgent;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

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
		fProps.put ( "enable.auto.commit", false );
		fProps.put ( ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class );
		fProps.put ( ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class );

		fConsumer = new KafkaConsumer<> ( fProps );
		fConsumer.subscribe ( Collections.singletonList ( topic ) );

		fPendingMsgs = new LinkedList<> ();
		fCommitOnDraw = cc.optBoolean ( "commitOnDraw", false );

		fAlerts = new ContinualAlertAgent ();
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
		if ( !fPendingMsgs.isEmpty () )
		{
			return drawNext ();
		}

		final ConsumerRecords<String,String> records = fConsumer.poll ( Duration.ofMillis ( 100L ) );
		for ( ConsumerRecord<String,String> cr : records )
		{
			try
			{
				fPendingMsgs.add ( new LocalMsgAndRouting ( cr ) );
			}
			catch ( JSONException x )
			{
				spc.warn ( "Couldn't parse inbound text as a JSON object: " + cr.value () );
			}
		}

		return drawNext ();
	}

	@Override
	public synchronized void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		if ( !fCommitOnDraw && mr instanceof LocalMsgAndRouting )
		{
			((LocalMsgAndRouting) mr).commit ();
		}
	}

	private final Properties fProps;
	private final KafkaConsumer<String, String> fConsumer;
	private final LinkedList<LocalMsgAndRouting> fPendingMsgs;
	private final boolean fCommitOnDraw;
	private final ContinualAlertAgent fAlerts;

	private LocalMsgAndRouting drawNext ()
	{
		if ( !fPendingMsgs.isEmpty () )
		{
			final LocalMsgAndRouting msg = fPendingMsgs.remove ();
			if ( fCommitOnDraw )
			{
				msg.commit ();
			}
			return msg;
		}
		return null;
	}

	private class LocalMsgAndRouting implements MessageAndRouting
	{
		public LocalMsgAndRouting ( ConsumerRecord<String,String> cr )
		{
			fRecord = cr;
			fMsg = Message.adoptJsonAsMessage ( new JSONObject ( new CommentedJsonTokener ( cr.value () ) ) );
			fCommitted = false;
		}

		@Override
		public Message getMessage ()
		{
			return fMsg;
		}

		@Override
		public String getPipelineName ()
		{
			return getDefaultPipelineName ();
		}

		public void commit ()
		{
			try
			{
				log.info ( "Committing offset for {}: {}", fRecord.topic (), fRecord.offset () );

				final Map<TopicPartition, OffsetAndMetadata> commitMap = Collections.singletonMap (
					new TopicPartition ( fRecord.topic (), fRecord.partition () ),
					new OffsetAndMetadata ( fRecord.offset () + 1 ) // +1 to mark the *next* message
				);
				fConsumer.commitSync ( commitMap );
				fCommitted = true;
			}
			catch ( Exception x )
			{
				log.error ( "Error committing offset for {}: {}", fRecord.topic (), x.getMessage () );
			}
		}

		private final ConsumerRecord<String,String> fRecord;
		private final Message fMsg;
		private boolean fCommitted;
	}

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

	private static final Logger log = LoggerFactory.getLogger ( KafkaSource.class );
}
