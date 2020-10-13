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

package io.continual.services.processor.engine.model;

import io.continual.util.data.json.JsonSerialized;

public interface Filter extends JsonSerialized
{
	/**
	 * Return true if the Filter passes the message in the given context.
	 * @param ctx the message processing context
	 * @return true if the filter passes the message
	 */
	boolean passes ( MessageProcessingContext ctx );
}
