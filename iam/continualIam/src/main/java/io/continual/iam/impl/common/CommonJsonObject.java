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
package io.continual.iam.impl.common;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.UserDataHolder;

public abstract class CommonJsonObject implements UserDataHolder
{
	public abstract void reload () throws IamSvcException;

	protected void setValue ( String name, String val ) throws IamSvcException
	{
		JSONObject o = getDataRecord().optJSONObject ( "aux" );
		if ( o == null )
		{
			o = new JSONObject ();
			getDataRecord().put ( "aux", o );
		}
		o.put ( name, val );
		store ();
	}

	protected String getValue ( String name, String defval )
	{
		final JSONObject o = getDataRecord().optJSONObject ( "aux" );
		if ( o == null ) return defval;
		return o.optString ( name, defval );
	}

	protected boolean getValue ( String name, boolean defval )
	{
		final JSONObject o = getDataRecord().optJSONObject ( "aux" );
		if ( o == null ) return defval;
		return o.optBoolean ( name, defval );
	}

	protected void removeValue ( String name ) throws IamSvcException
	{
		final JSONObject o = getDataRecord().optJSONObject ( "aux" );
		if ( o != null )
		{
			o.remove ( name );
		}
		store ();
	}
	
	@Override
	public String getUserData ( String key ) throws IamSvcException
	{
		return getValue ( key, null );
	}

	@Override
	public void putUserData ( String key, String val ) throws IamSvcException
	{
		setValue ( key, val );
	}

	@Override
	public void removeUserData ( String key ) throws IamSvcException
	{
		removeValue ( key );
	}

	@Override
	public Map<String,String> getAllUserData () throws IamSvcException
	{
		final HashMap<String,String> result = new HashMap<String,String> ();
		final JSONObject o = getDataRecord().optJSONObject ( "aux" );
		if ( o != null )
		{
			for ( Object key : o.keySet () )
			{
				final Object val = o.get ( key.toString() );
				if ( val != null )
				{
					result.put ( key.toString (), val.toString() );
				}
			}
		}
		return result;
	}

	protected abstract JSONObject getDataRecord ();
	protected abstract void store () throws IamSvcException;
}
