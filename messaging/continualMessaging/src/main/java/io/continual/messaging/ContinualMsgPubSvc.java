package io.continual.messaging;

import java.io.IOException;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * A service to wrap a non-service publisher
 */
public class ContinualMsgPubSvc extends SimpleService implements ContinualMessagePublisher
{
	public ContinualMsgPubSvc ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final JSONObject innerConfig = config.getJSONObject ( "inner" ) ;
		final JSONObject updatedInnerConfig = sc.getExprEval ( config ).evaluateJsonObject ( innerConfig );
		fInner = Builder.withBaseClass ( ContinualMessagePublisher.class )
			.withClassNameInData ()
			.usingData ( new BuilderJsonDataSource ( updatedInnerConfig ) )
			.providingContext ( sc )
			.build ();
	}

	@Override
	public void close () throws IOException
	{
		fInner.close ();
	}

	@Override
	public ContinualMessageSink getTopic ( String topic ) throws TopicUnavailableException
	{
		return fInner.getTopic ( topic );
	}

	@Override
	public void flush () throws MessagePublishException
	{
		fInner.flush ();
	}

	private final ContinualMessagePublisher fInner; 
}
