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

package io.continual.resources.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.resources.ResourceLoader;
import io.continual.resources.ResourceSource;

public class FileLoader implements ResourceSource
{
	public FileLoader ()
	{
		this ( null );
	}

	public FileLoader ( File base )
	{
		fBase = base;
	}

	@Override
	public boolean qualifies ( String resourceId )
	{
		// pretty much anything *might* be a filename
		return true;
	}

	@Override
	public InputStream loadResource ( String resourceId ) throws IOException
	{
		// try as a file
		final File f = fBase == null ?
			new File ( resourceId ) :
			new File ( fBase, resourceId )
		;
		if ( f.exists () )
		{
			try
			{
				return new FileInputStream ( resourceId );
			}
			catch ( FileNotFoundException x )
			{
				log.warn ( "File not found after exists() returned true.", x );
			}
		}
		return null;
	}

	private final File fBase;

	private static final Logger log = LoggerFactory.getLogger ( ResourceLoader.class );
}
