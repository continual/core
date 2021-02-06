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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface Source extends Closeable
{
	/**
	 * Is the source at EOF?  Not all sources are bounded, so they may
	 * always return false.
	 * @return true if the source is at EOF
	 * @throws IOException
	 */
	boolean isEof () throws IOException;

	/**
	 * Get the next message from this source.
	 * @return the next message with routing, or null if none are available
	 * @throws IOException
	 */
	MessageAndRouting getNextMessage ( StreamProcessingContext spc, long waitAtMost, TimeUnit waitAtMostTimeUnits ) throws IOException, InterruptedException;

	/**
	 * Requeue a message for delivery into the given pipeline
	 * @param msgAndRoute
	 */
	void requeue ( MessageAndRouting msgAndRoute );

	/**
	 * Called by the processing engine when processing for a given message is complete.
	 * @param spc
	 * @param mr
	 */
	void markComplete ( StreamProcessingContext spc, MessageAndRouting mr );
}
