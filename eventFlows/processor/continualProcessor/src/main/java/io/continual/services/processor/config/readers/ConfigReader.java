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

package io.continual.services.processor.config.readers;

import java.io.InputStream;

import io.continual.services.processor.engine.model.Program;

/**
 * A configuration reader.
 */
public interface ConfigReader
{
	/**
	 * Read a program from a named resource.
	 * @param resName
	 * @return a program
	 * @throws ConfigReadException
	 */
	default public Program read ( String resName ) throws ConfigReadException
	{
		return read ( new String[] { resName } );
	}

	/**
	 * Read a program from a set of named sources
	 * @param resNames
	 * @return a program
	 * @throws ConfigReadException
	 */
	public Program read ( String[] resNames ) throws ConfigReadException;

	/**
	 * Read a program from an input stream.
	 * @param stream
	 * @return a program
	 * @throws ConfigReadException
	 */
	public Program read ( InputStream stream ) throws ConfigReadException;
}
