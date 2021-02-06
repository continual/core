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

package io.continual.services.model.core.impl.file;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.access.AccessControlList;
import io.continual.util.data.json.JsonUtil;

import io.continual.services.model.core.ModelObject;

class FileModelObject implements ModelObject
{
	public FileModelObject ( String id, JSONObject rawData )
	{
		fId = id;
		fAcl = AccessControlList.deserialize ( rawData.optJSONObject ( "acl" ), null );
		fRaw = rawData;
	}

	@Override
	public String getId ()
	{
		return fId;
	}

	@Override
	public AccessControlList getAccessControlList ()
	{
		return fAcl;
	}

	@Override
	public String asJson ()
	{
		return getData().toString ();
	}

	@Override
	public Set<String> getTypes ()
	{
		return new TreeSet<> ();
	}

	@Override
	public JSONObject getData ()
	{
		final JSONObject data = fRaw.optJSONObject ( FileBasedModel.kUserDataTag );
		return ( data == null ? new JSONObject () : JsonUtil.clone ( data ) );
	}

	private final String fId;
	private final AccessControlList fAcl;
	private final JSONObject fRaw;
};
