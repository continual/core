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

import org.json.JSONObject;

public class ServiceConfig
{
	public static ServiceConfig read ( JSONObject sc )
	{
		final ServiceConfig oc = new ServiceConfig ();
		oc.fLocal = ConfigObject.read ( sc );
		return oc;
	}

	public JSONObject toJson ()
	{
		return fLocal.toJson ();
	}
	
	@Override
	public String toString ()
	{
		return toJson().toString ( 4 );
	}

	public void setBaseConfig ( ConfigObject c )
	{
		fLocal.setBaseConfig ( c );
	}

	public void overwrite ( ProfileConfig pc )
	{
		fLocal = pc.getConfigOverridesFor ( getName () )
			.setBaseConfig ( fLocal )
		;
	}

	public String getClassname ()
	{
		// the service code always used "classname" but the builder code used
		// "class."  For compatibility, we'll allow both but prefer "classname".
		
		String cn = fLocal.get ( "classname" );
		if ( cn == null )
		{
			cn = fLocal.get ( "class" );
		}
		return cn;
	}

	public String getName ()
	{
		return fLocal.get ( "name", "?" );
	}

	public boolean enabled ()
	{
		return fLocal.getBoolean ( "enabled", true );
	}

	private ConfigObject fLocal;
}
