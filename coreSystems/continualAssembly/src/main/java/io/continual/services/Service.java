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

package io.continual.services;

public interface Service
{
	public class FailedToStart extends Exception
	{
		public FailedToStart ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * Start this service, post construction.
	 */
	void start () throws FailedToStart;

	/**
	 * Request this service to shutdown.
	 */
	void requestFinish ();

	/**
	 * Return true if the service is running.
	 * @return true if the service is running
	 */
	boolean isRunning ();
}
