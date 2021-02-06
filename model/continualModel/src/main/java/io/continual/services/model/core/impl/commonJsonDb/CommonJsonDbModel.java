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

import java.util.Collection;
import java.util.List;

import org.json.JSONObject;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelObjectUpdater;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelRelation;
import io.continual.util.naming.Path;

public abstract class CommonJsonDbModel extends SimpleService implements Model
{
	public CommonJsonDbModel ( ServiceContainer sc, JSONObject config )
	{
		this ( config.getString ( "acctId" ), config.getString ( "modelId" ) );
	}

	public CommonJsonDbModel ( String acctId, String modelId )
	{
		fAcctId = acctId;
		fModelId = modelId;
	}

	@Override
	public String getAcctId ()
	{
		return fAcctId;
	}

	@Override
	public String getId ()
	{
		return fModelId;
	}

	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final ModelObjectPath mop = pathToFullPath ( objectPath );
		if ( context.knownToNotExist ( mop ) ) return false;

		final boolean result = objectExists ( context, mop );
		if ( !result )
		{
			context.doesNotExist ( mop );
		}
		return result;
	}

	@Override
	public ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceIoException, ModelServiceRequestException
	{
		final ModelObjectPath mop = pathToFullPath ( objectPath );
		if ( context.knownToNotExist ( mop ) )
		{
			throw new ModelItemDoesNotExistException ( mop );
		}
		return loadObject ( context, mop );
	}

	@Override
	public void store ( ModelRequestContext context, Path objectPath, String jsonData ) throws ModelServiceRequestException, ModelServiceIoException
	{
		store ( context, objectPath, new SimpleDataObject ( objectPath.getId (), jsonData ) );
	}

	@Override
	public void store ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException
	{
		internalStore ( context, pathToFullPath ( objectPath ), o );
	}

	@Override
	public void update ( ModelRequestContext context, Path objectPath, ModelObjectUpdater o )
		throws ModelServiceRequestException, ModelServiceIoException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void relate ( ModelRequestContext context, ModelRelation reln )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void relate ( ModelRequestContext context, Collection<ModelRelation> relns )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final String fAcctId;
	private final String fModelId;

	protected ModelObjectPath pathToFullPath ( Path id )
	{
		return new ModelObjectPath (
			fAcctId,
			fModelId,
			id
		);
	}

	protected abstract boolean objectExists ( ModelRequestContext context, ModelObjectPath objectPath ) throws ModelServiceIoException, ModelServiceRequestException;
	protected abstract ModelObject loadObject ( ModelRequestContext context, ModelObjectPath objectPath ) throws ModelItemDoesNotExistException, ModelServiceIoException, ModelServiceRequestException;
	protected abstract void internalStore ( ModelRequestContext context, ModelObjectPath objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException;

	public static final String kMetadataTag = "Ⓜ";
	public static final String kUserDataTag = "Ⓤ";
}
