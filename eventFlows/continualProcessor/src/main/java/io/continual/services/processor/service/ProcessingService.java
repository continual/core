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

package io.continual.services.processor.service;

/**
 * A processing service is instantiated within the scope of a source and provides a
 * mechanism for running background processing. 
 */
public interface ProcessingService
{
	/**
	 * Start background processing (if any) for this service.
	 */
	void startBackgroundProcessing ();

	/**
	 * Determine if this service is running.
	 * @return true if still running
	 */
	boolean isRunning ();

	/**
	 * Stop background processing (if any) for this service. This method should return
	 * immediately. The caller can check isRunning() to determine that the service has
	 * stopped.
	 */
	void stopBackgroundProcessing ();

	/**
	 * Called when the source associated with this service instance reaches EOF
	 */
	default void onSourceEof () {}
}
