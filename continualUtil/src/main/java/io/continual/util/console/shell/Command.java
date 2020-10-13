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
package io.continual.util.console.shell;

import java.io.PrintStream;
import java.util.HashMap;

import io.continual.util.console.ConsoleProgram.UsageException;

import io.continual.util.nv.NvReadable;

public interface Command
{
	String getCommand ();

	/**
	 * check the arguments provided
	 * @param p the preferences structure
	 * @param args the user's arguments
	 * @throws UsageException if the call does not conform to correct usage
	 */
	void checkArgs ( NvReadable p, String[] args ) throws UsageException;

	/**
	 * @return a string used for the help command to show simple usage
	 */
	String getUsage ();

	/**
	 * @return a string used for the help command to show detail info
	 */
	String getHelp ();

	/**
	 * @param workspace the data workspace for this program
	 * @param outTo the output stream for commands
	 * @return an input result
	 * @throws UsageException if the call does not conform to correct usage
	 */
	ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, PrintStream outTo ) throws UsageException;
}
