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

package io.continual.services.model.service.impl.cassandra;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelStdUserGroups;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.service.ModelObjectContainer;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Name;

public class CassModelObject extends CassObjectContainer implements ModelObjectContainer
{
	public static CassModelObject fromJson ( JSONObject object, CassModelLoaderContext mlc ) throws ModelServiceException
	{
		return new CassModelObject ( mlc, object );
	}

	CassModelObject ( CassModelLoaderContext mlc, JSONObject serialized )
	{
		super ( mlc, serialized );

		fPathAsLoaded = mlc.getPath ();

		fObjectData = JsonUtil.clone ( serialized );
		fObjectData.remove ( kMetadataTag );
	}

	@Override
	public String getResourceDescription ()
	{
		return fPathAsLoaded == null ? "/" : fPathAsLoaded.toString ();
	}

	@Override
	public boolean exists ( ModelRequestContext context, Name itemName ) throws ModelServiceException
	{
		// ask the top-level model service if the full path exists
		try
		{
			return getBaseContext ()
				.getModelService ()
				.getAccount ( context, fPathAsLoaded.getAcctId () )
				.getModel ( context, fPathAsLoaded.getModelName () )
				.exists ( context, fPathAsLoaded.getObjectPath ().makeChildItem ( itemName ) )
			;
		}
		catch ( ModelRequestException e )
		{
			throw new ModelServiceException ( e );
		}
	}

	@Override
	public CassElementList getElementsBelow ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
	{
		return getElementsBelow ( context, fPathAsLoaded.getObjectPath () );
	}

	public static String createBasicObjectJson ( String acctId )
	{
		return toJson ( new JSONObject (), 
				new AccessControlList ( null )
					.setOwner ( acctId )
					.addAclEntry ( new AccessControlEntry ( AccessControlEntry.kOwner, Access.PERMIT, new String[] {
						ModelOperation.READ.toString (),
						ModelOperation.CREATE.toString (),
						ModelOperation.UPDATE.toString (),
						ModelOperation.DELETE.toString ()
					} ) )
					.addAclEntry ( new AccessControlEntry ( ModelStdUserGroups.kSysAdminGroup, Access.PERMIT, new String[] {
						ModelOperation.READ.toString (),
						ModelOperation.CREATE.toString (),
						ModelOperation.UPDATE.toString (),
						ModelOperation.DELETE.toString ()
					} ) ),
				true
			)
			.toString ();
	}

	public static String createBasicObjectJson ( AccessControlList acl )
	{
		return toJson ( new JSONObject (), acl, true ).toString ();
	}

	@Override
	public String getId ()
	{
		return fPathAsLoaded.getId ();
	}

	private final ModelObjectPath fPathAsLoaded;
	private final JSONObject fObjectData;

	@Override
	public String asJson ()
	{
		return toJson ( getLocalData (), getAccessControlList (), false ).toString ();
	}

	@Override
	JSONObject getLocalData ()
	{
		return fObjectData;
	}

	@Override
	public Set<String> getTypes ()
	{
		return new TreeSet<> ();
	}

	@Override
	public JSONObject getData ()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
