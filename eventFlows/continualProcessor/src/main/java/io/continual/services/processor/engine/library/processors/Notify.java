/*
 *	Copyright 2023, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.processor.engine.library.processors;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.notify.ContinualNotifier;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.JsonDataSource;
import io.continual.util.data.json.JsonUtil;

// FIXME: should this be a sink instead of a processor?
public class Notify implements Processor
{
	public Notify () throws BuildFailure
	{
		fPreConfig = new JSONObject ();
	}

	public Notify ( JSONObject config ) throws BuildFailure
	{
		fPreConfig = JsonUtil.clone ( config );
	}

	public Notify ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		final ExpressionEvaluator ee = sc.getServiceContainer ().getExprEval ();

		fPreConfig = new JSONObject ()
			.put ( kTopicSetting, ee.evaluateSymbol ( kTopicSetting ) )
			.put ( kStreamSetting, ee.evaluateSymbol ( kStreamSetting ) )
			.put ( kMsgSetting, ee.evaluateSymbol ( kMsgSetting ) )
			.put ( kUserSetting, ee.evaluateSymbol ( kUserSetting ) )
			.put ( kPasswordSetting, ee.evaluateSymbol ( kPasswordSetting ) )
		;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String msgEval = readSetting ( context, kMsgSetting );
		final String line = msgEval == null ?
			context.getMessage ().toLine () :
			context.evalExpression ( msgEval )
		;

		new ContinualNotifier ()
			.asUser ( readSetting ( context, kUserSetting ), readSetting ( context, kPasswordSetting ) )
			.toTopic ( readSetting ( context, kTopicSetting ) )
			.onStream ( readSetting ( context, kStreamSetting ) )
			.withDetails ( line )
			.send ()
		;
	}

	private final JSONObject fPreConfig;

	private String readSetting ( MessageProcessingContext context, String key )
	{
		final String result = context.evalExpression ( "${" + key + "}", new JsonDataSource ( fPreConfig ) );
		return result == null || result.length () == 0 ? null : result;
	}
	
	private static final String kTopicSetting = "CONTINUAL_TOPIC";
	private static final String kStreamSetting = "CONTINUAL_STREAM";
	private static final String kMsgSetting = "CONTINUAL_MESSAGE";
	private static final String kUserSetting = "CONTINUAL_USER";
	private static final String kPasswordSetting = "CONTINUAL_PASSWORD";
}
