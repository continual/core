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

import io.continual.metrics.MetricsCatalog;

/**
 * A message processing context is provided to processors for access to the current message, its
 * containing stream, and control state.
 */
public interface MessageProcessingContext
{
	/**
	 * Get the stream processing context associated with the current message.
	 * @return a stream processing context
	 */
	StreamProcessingContext getStreamProcessingContext ();

	/**
	 * Get a unique serial number for the current message
	 * @return a serial number
	 */
	String getId ();

	/**
	 * Get the message being processed
	 * @return the message
	 */
	Message getMessage ();

	/**
	 * Should the engine continue to process this message?
	 * @return normally true, false if stopProcessing or stream context's fail has been called
	 */
	boolean shouldContinue ();

	/**
	 * Ask the engine to stop processing this message
	 */
	void stopProcessing ();

	/**
	 * Ask the engine to stop processing this message and log the warning.
	 * @param warningText
	 */
	default void stopProcessing ( String warningText )
	{
		stopProcessing ();
		warn ( warningText );
	}

	/**
	 * report a warning about processing
	 * @param warningText
	 */
	void warn ( String warningText );

	/**
	 * Get a named source from the program
	 * @param srcName the name of a source
	 * @return a source, or null if none by that name
	 */
	Source getSource ( String srcName );

	/**
	 * Get a named sink from the program
	 * @param sinkName
	 * @return a sink, or null if none by that name
	 */
	Sink getSink ( String sinkName );

	/**
	 * Evaluate a ${} expression in the message context, which includes
	 * the message as primary source, then the stream processing context.
	 * @param expression
	 * @return a value, which may be an empty string
	 */
	default String evalExpression ( String expression )
	{
		return evalExpression ( expression, String.class );
	}

	/**
	 * Evaluate a ${} expression in the message context, which includes
	 * the message as primary source, then the stream processing context.
	 * @param expression
	 * @return a value, which may be an empty string
	 */
	<T> T evalExpression ( String expression, Class<T> targetType );

	/**
	 * Get a metrics catalog appropriate for the scope of this message's processing
	 * @return
	 */
	MetricsCatalog getMetrics ();
}
