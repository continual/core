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
import io.continual.services.model.core.ModelRelation;

public class ObjectRenderer
{
	public ObjectRenderer withData ( ModelObject o ) { fMoc = o; return this; }

	public ObjectRenderer withRelations ( List<ModelRelation> relns ) { fRelns = relns; return this; }
	public ObjectRenderer withInboundRelnsOnly () { fDir = "in"; return this; }
	public ObjectRenderer withOutboundRelnsOnly () { fDir = "out"; return this; }
	public ObjectRenderer withRelnName ( String name ) { fRelnName = name; return this; }

	public String renderText () { return render().toString (); }

	public JSONObject render ()
	{
		final JSONObject result = new JSONObject ();

		if ( fMoc != null )
		{
			result
				.put ( "data", fMoc.getData () )
				.put ( "meta", fMoc.getMetadata ().toJson () )
			;
		}

		if ( fRelns != null )
		{
			final JSONArray relnArr = new JSONArray ();
			for ( ModelRelation r : fRelns )
			{
				final JSONObject relnO = new JSONObject ()
					.put ( "from", r.getFrom () )
					.put ( "to", r.getTo () )
					.put ( "name", r.getName () )
				;
				relnArr.put ( relnO );
			}

			final JSONObject relns = new JSONObject ()
				.put ( "set", relnArr )
			;

			if ( fDir != null )
			{
				final JSONObject filter = new JSONObject ()
					.put ( "set", fDir )
				;
				if ( fRelnName != null )
				{
					filter.put ( "name", fRelnName );
				}
				relns.put ( "filter", filter );
			}
				
			result.put ( "relations", relns );
		}

		return result;
	}
	
	private ModelObject fMoc = null;
	private List<ModelRelation> fRelns = null;
	private String fDir = null;
	private String fRelnName = null;
}
