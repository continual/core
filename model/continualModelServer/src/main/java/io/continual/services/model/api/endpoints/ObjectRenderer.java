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

package io.continual.services.model.api.endpoints;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.util.naming.Path;

public class ObjectRenderer
{
	public ObjectRenderer atPath ( Path p ) { fPath = p; return this; }
	public ObjectRenderer withData ( ModelObject o ) { fMoc = o; return this; }

	public ObjectRenderer withRelations ( List<ModelRelationInstance> relns ) { fRelns = relns; return this; }
	public ObjectRenderer withInboundRelnsOnly () { fDir = "in"; return this; }
	public ObjectRenderer withOutboundRelnsOnly () { fDir = "out"; return this; }
	public ObjectRenderer withRelnName ( String name ) { fRelnName = name; return this; }

	public String renderText () { return render().toString (); }

	public JSONObject render ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "path", fPath.toString () )
		;

		// write the object data if available
		if ( fMoc != null )
		{
			result
				.put ( "data", fMoc.getData () )
				.put ( "meta", fMoc.getMetadata ().toJson () )
			;
		}

		// write relations if available
		if ( fRelns != null )
		{
			// top-level structures
			final JSONObject out = new JSONObject ();
			final JSONObject in = new JSONObject ();
			final JSONObject filter = new JSONObject ();

			final JSONObject relns = new JSONObject ()
				.put ( "outbound", out )
				.put ( "inbound", in )
				.put ( "filter", filter )
			;
			result.put ( "relations", relns );

			// organize each relation into the rendering structure
			for ( ModelRelationInstance r : fRelns )
			{
				final boolean isOut = r.getFrom ().equals ( fPath );
				final JSONObject targetList = isOut ? out : in;

				// get/create relation set by name
				JSONObject entry = targetList.optJSONObject ( r.getName () );
				if ( entry == null )
				{
					entry = new JSONObject ()
						.put ( "type", "set" )
					;
					targetList.put ( r.getName (), entry );
				}

				JSONArray list = entry.optJSONArray ( "entries" );
				if ( list == null )
				{
					list = new JSONArray ();
					entry.put ( "entries", list );
				}

				list.put (
					new JSONObject ()
						.put ( "id", r.getId () )
						.put ( "remote", isOut ? r.getTo () : r.getFrom () )
				);
			}

			// note any filters
			if ( fDir != null )
			{
				filter.put ( "direction", fDir );
			}
			if ( fRelnName != null )
			{
				filter.put ( "name", fRelnName );
			}
		}

		return result;
	}

	private Path fPath = null;
	private ModelObject fMoc = null;
	private List<ModelRelationInstance> fRelns = null;
	private String fDir = null;
	private String fRelnName = null;
}
