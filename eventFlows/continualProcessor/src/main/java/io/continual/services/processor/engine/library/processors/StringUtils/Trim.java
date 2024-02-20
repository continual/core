/*
 *	Copyright 2019, Continual.io
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

package io.continual.services.processor.engine.library.processors.StringUtils;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class Trim implements Processor
{
	public Trim ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fFrom = config.getString ( "from" );
		fTo = config.optString ( "to", fFrom );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		context.getMessage ().putValue ( fTo, context.getMessage().getString ( fFrom ).trim () );
	}

	private final String fFrom;
	private final String fTo;
}
