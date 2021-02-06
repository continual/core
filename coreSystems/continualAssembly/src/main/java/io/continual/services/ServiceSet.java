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

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class ServiceSet
{
	public static ServiceSet readConfig ( Reader r )
	{
		final JSONObject top = new JSONObject ( new CommentedJsonTokener ( r ) );

		final ServiceSet ss = new ServiceSet ()
			.useConfiguration ( ConfigObject.read ( top.optJSONObject ( "config" ) ) )
		;

		JsonVisitor.forEachElement ( top.optJSONObject ( "profiles" ), new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String key, JSONObject sc ) throws JSONException
			{
				final ProfileConfig pc = ProfileConfig.read ( sc );
				ss.profiles.put ( key, pc );
				return true;
			}
		} );

		JsonVisitor.forEachElement ( top.optJSONArray ( "services" ), new ArrayVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( JSONObject sc ) throws JSONException
			{
				ss.hostingService ( ServiceConfig.read ( sc ) );
				return true;
			}
		} );
		
		return ss;
	}

	public void applyProfile ( String profile )
	{
		init ();

		final ProfileConfig pc = profiles.get ( profile );
		if ( pc != null )
		{
			for ( ServiceConfig sc : services )
			{
				sc.overwrite ( pc );
			}
		}
		else
		{
			log.warn ( "Profile [" + profile + "] is not in the configuration." );
		}
	}

	public ServiceConfig getService ( String named )
	{
		init ();
		return servicesByName.get ( named );
	}

	public Collection<ServiceConfig> getServices ()
	{
		init ();
		return new LinkedList<ServiceConfig> ( services );
	}

	private synchronized void init ()
	{
		if ( !fInited )
		{
			fInited = true;

			// make sure each service knows its name and has the base
			// config
			for ( ServiceConfig sc : services )
			{
				sc.setBaseConfig ( config );
				servicesByName.put ( sc.getName (), sc );
			}
		}
	}

	public ServiceSet useConfiguration ( ConfigObject co )
	{
		config = co;
		return this;
	}

	public ServiceSet hostingService ( ServiceConfig sc )
	{
		services.add ( sc );
		return this;
	}

	private boolean fInited = false;
	private ConfigObject config = new ConfigObject ();
	private LinkedList<ServiceConfig> services = new LinkedList<ServiceConfig> ();
	private HashMap<String,ProfileConfig> profiles = new HashMap<String,ProfileConfig> ();
	private HashMap<String,ServiceConfig> servicesByName = new HashMap<String,ServiceConfig> ();

	private static final Logger log = LoggerFactory.getLogger ( ServiceSet.class );
}
