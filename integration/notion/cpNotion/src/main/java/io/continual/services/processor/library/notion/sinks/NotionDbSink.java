package io.continual.services.processor.library.notion.sinks;

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

public class NotionDbSink implements Sink
{
	public static final String kSetting_PayloadTag = "payload";
	public static final String kDefault_PayloadTag = "toNotion";
	
	public NotionDbSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fHttpClient = new OkHttp ();
		fNotionPayload = config.optString ( kSetting_PayloadTag, kDefault_PayloadTag );
		fApiToken = config.getString ( "apiToken" );
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
		final Object obj = context.getMessage ().getRawValue ( fNotionPayload );
		if ( obj instanceof JSONObject )
		{
			log.info ( "POST to " + kNotionApiPath );

			try ( HttpResponse resp = fHttpClient.newRequest ()
				.onPath ( kNotionApiPath )
				.withHeader ( "Authorization", context.evalExpression ( fApiToken ) )
				.withHeader ( "Notion-Version", "2022-06-28" )
				.withHeader ( "Content-Type", "application/json" )
				.post ( (JSONObject) obj ) )
		    {
				if ( resp.isSuccess () )
				{
					log.info ( "> " + resp.getCode () + " " + resp.getMessage () );
				}
				else
				{
					context.warn ( "Notion API call failed: " + resp.getCode () + " " + resp.getMessage () );
				}
		    }
			catch ( HttpServiceException e )
			{
				context.warn ( "Notion API call failed: " + e.getMessage () );
			}
		}
		else
		{
			context.warn ( "No Notion payload found at " + fNotionPayload );
		}
	}

	private final String fApiToken;
    private final String fNotionPayload;
    private final OkHttp fHttpClient;

    private static final String kNotionApiPath = "https://api.notion.com/v1/pages";
    private static final Logger log = LoggerFactory.getLogger ( NotionDbSink.class );
}
