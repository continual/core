package io.continual.services.processor.library.telegram.sinks;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.jsonHttpClient.impl.ok.OkHttp;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;

public class TelegramSink implements Sink
{
	public static final String kSetting_PayloadTag = "payload";
	public static final String kDefault_PayloadTag = "toTelegram";
	
	public TelegramSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fHttpClient = new OkHttp ();
		fMsgPayload = config.optString ( kSetting_PayloadTag, kDefault_PayloadTag );
		fBotApiKey = config.getString ( "botApiKey" );
		fTargetId = config.getString ( "targetId" );
	}

	@Override
	public void init ()
	{
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void close ()
	{
		fHttpClient.close ();
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final Object obj = context.getMessage ().getRawValue ( fMsgPayload );
		if ( obj instanceof String )
		{
			final JSONObject payload = new JSONObject ()
				.put ( "chat_id", context.evalExpression ( fTargetId ) )
				.put ( "text", (String) obj )
			;

			log.info ( "POST to " + kTelegramApiPath + ": " + payload.toString () );

			try ( HttpResponse resp = fHttpClient.newRequest ()
				.onPath ( kTelegramApiPath + context.evalExpression ( fBotApiKey ) + "/sendMessage" )
				.withHeader ( "Content-Type", "application/json" )
				.post ( payload )
			)
		    {
				if ( resp.isSuccess () )
				{
					log.info ( "> " + resp.getCode () + " " + resp.getMessage () );
				}
				else
				{
					context.warn ( "Telegram API call failed: " + resp.getCode () + " " + resp.getMessage () );
				}
		    }
			catch ( HttpServiceException e )
			{
				context.warn ( "Telegram API call failed: " + e.getMessage () );
			}
		}
		else
		{
			context.warn ( "No Telegram payload found at " + fMsgPayload );
		}
	}

	private final String fBotApiKey;
	private final String fTargetId;
    private final String fMsgPayload;
    private final OkHttp fHttpClient;

    private static final String kTelegramApiPath = "https://api.telegram.org/bot";
    private static final Logger log = LoggerFactory.getLogger ( TelegramSink.class );
}
