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

import io.continual.util.console.shell.Command;

/**
 * A service that implements this interface can return command line command objects
 * when given a command string.
 */
public interface CliControlledService
{
	/**
	 * Return a command handler if the command string is known to the service. 
	 * @param cmd
	 * @return a command handler or null
	 */
	Command getCommandFor ( String cmd );
}
