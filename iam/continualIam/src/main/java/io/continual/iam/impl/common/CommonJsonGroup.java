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

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.util.data.json.JsonVisitor;

public class CommonJsonGroup extends CommonJsonObject implements Group
{
	public static JSONObject initializeGroup ( String name )
	{
		return new JSONObject ()
			.put ( "name", name )
		;
	}

	/**
	 * Construct a group
	 * @param db
	 * @param id
	 * @param groupData
	 * @deprecated Use the other form, which is more similar to the identity constructor
	 */
	@Deprecated
	public CommonJsonGroup ( CommonJsonDb<?,?> db, String id, JSONObject groupData )
	{
		this ( id, groupData, db );
	}

	public CommonJsonGroup ( String id, JSONObject groupData, CommonJsonDb<?,?> db )
	{
		fDb = db;
		fId = id;
		fObj = groupData;
		fMembers = new TreeSet<String> ();

		parse ();
	}

	@Override
	public String getId ()
	{
		return fId;
	}

	@Override
	public String getName ()
	{
		return fObj.optString ( "name" );
	}

	@Override
	public boolean isMember ( String userId ) throws IamSvcException
	{
		return fMembers.contains ( userId );
	}

	@Override
	public Set<String> getMembers () throws IamSvcException
	{
		return new TreeSet<String> ( fMembers );
	}

	public void addUser ( String userId ) throws IamSvcException
	{
		if ( fMembers.add ( userId ) )
		{
			pack ();
			store ();
		}
	}

	public void removeUser ( String userId ) throws IamSvcException
	{
		if ( fMembers.remove ( userId ) )
		{
			pack ();
			store ();
		}
	}

	private final String fId;
	private final CommonJsonDb<?,?> fDb;
	private JSONObject fObj;
	private final TreeSet<String> fMembers;

	@Override
	public void reload () throws IamSvcException
	{
		fObj = fDb.loadGroupObject ( getId() );
		parse ();
	}

	@Override
	protected JSONObject getDataRecord ()
	{
		return fObj;
	}

	@Override
	protected void store () throws IamSvcException
	{
		fDb.storeGroupObject ( getId(), getDataRecord() );
	}

	private void parse ()
	{
		fMembers.clear ();
		fMembers.addAll ( JsonVisitor.arrayToList ( fObj.optJSONArray ( "members" ) ) );
	}

	private void pack ()
	{
		fObj.put (
			"members",
			JsonVisitor.collectionToArray ( fMembers )
		);
	}
}
