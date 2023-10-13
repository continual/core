/*
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

package io.continual.util.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CVersionInfo
{
	public static String getVersionString ()
	{
		return "Continual.io " + getVersion ();
	}

	public static String getVersion ()
	{
		return skVersion;
	}

	public static int getBuildYear ()
	{
		return skBuildYear;
	}

	private static final String skVersion;
	private static final int skBuildYear;

	static
	{
		String use = null;
		int build = 0;
		try
		{
			final InputStream is = CVersionInfo.class.getResourceAsStream ( "/continualVersion.properties" );
			if ( is != null )
			{
				final Properties props = new Properties();
				props.load ( is );
				use = props.getProperty ( "continualVersion", null );

				// format: 2023-10-06 00:11
				final String ts = props.getProperty ( "continualBuildTimestamp", null );
				final int hyphen = ts.indexOf ( '-' );
				if ( hyphen > -1 )
				{
					final String yrStr = ts.substring ( 0, hyphen );
					build = Integer.parseInt ( yrStr );
				}
			}
		}
		catch ( IOException e )
		{
		}
		skVersion = use;
		skBuildYear = build;
	}
}
