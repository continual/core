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

package io.continual.services.model.service.impl.s3;

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
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelElementList;
import io.continual.services.model.service.ModelObjectContainer;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Name;

public class S3ModelObject extends S3ObjectContainer implements ModelObjectContainer
{
	public static S3ModelObject fromJson ( JSONObject object, S3ModelLoaderContext mlc ) throws ModelServiceIoException
	{
		return new S3ModelObject ( mlc, object );
	}

	S3ModelObject ( S3ModelLoaderContext mlc, JSONObject serialized )
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
	public boolean exists ( ModelRequestContext context, Name itemName ) throws ModelServiceIoException
	{
		try
		{
			return getBaseContext ()
				.getS3ModelService ()
				.getAccount ( context, fPathAsLoaded.getAcctId () )
				.getModel ( context, fPathAsLoaded.getModelName () )
				.exists ( context, fPathAsLoaded.getObjectPath ().makeChildItem ( itemName ) )
			;
		}
		catch ( ModelServiceRequestException e )
		{
			throw new ModelServiceIoException ( e );
		}
	}

	@Override
	public boolean remove ( ModelRequestContext context, Name itemName ) throws ModelServiceIoException, ModelServiceAccessException
	{
		final ModelObjectPath id = fPathAsLoaded.makeChildItem ( itemName );

		// check permissions
		if ( exists ( context, itemName ) )
		{
			checkUser ( context.getOperator (), ModelOperation.DELETE );
			getS3().deleteObject ( id );
			return true;
		}
		return false;
	}

	@Override
	public ModelElementList getElementsBelow ( ModelRequestContext context ) throws ModelServiceRequestException, ModelServiceIoException
	{
		return new S3ElementList ( getS3().getChildrenOf ( fPathAsLoaded ) );
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
		return toJson ().toString ();
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
