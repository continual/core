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

package io.continual.services.model.core.impl.commonJsonDb;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObject;
import io.continual.util.data.json.JsonUtil;

public class SimpleDataObject implements ModelObject
{
	public SimpleDataObject ( String id, String data )
	{
		fId = id;
		fData = JsonUtil.readJsonObject ( data );
	}
	
	@Override
	public AccessControlList getAccessControlList () { return null; }

	@Override
	public String getId () { return fId; }

	@Override
	public String asJson ( ) { return fData.toString (); }

	@Override
	public Set<String> getTypes () { return new TreeSet<> (); }

	@Override
	public JSONObject getData ()
	{
		return JsonUtil.clone ( fData );
	}

	private final String fId;
	private final JSONObject fData;
}
