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

package io.continual.http.service.framework;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CHttpVersionInfo
{
	public static String getVersionString ()
	{
		return "Continual HTTP " + getVersion ();
	}

	public static String getVersion ()
	{
		return version;
	}

	public static String[] getTitleAndCopyright ()
	{
		return new String[]
		{
			".  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .",
			"Continual HTTP, version " + getVersion(),
			"Licensed under the Apache License, Version 2.0",
			".  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .  .",
		};
	}

	private static final Properties props = new Properties();
	private static final String version;

	static
	{
		String use = null;
		try
		{
			final InputStream is = CHttpVersionInfo.class.getResourceAsStream ( "/continualHttp.properties" );
			if ( is != null )
			{
				props.load ( is );
				use = props.getProperty ( "continualHttpVersion", null );
			}
		}
		catch ( IOException e )
		{
		}
		version = use;
	}
}
