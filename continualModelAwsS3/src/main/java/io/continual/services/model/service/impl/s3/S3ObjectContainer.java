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

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelObjectContainer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public abstract class S3ObjectContainer extends S3BackedObject implements ModelObjectContainer
{
	S3ObjectContainer ( S3ModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );
	}

	@Override
	public ModelObjectContainer load ( ModelRequestContext context, Name itemName )
		throws ModelItemDoesNotExistException, ModelServiceRequestException, ModelServiceIoException
	{
		final ModelObjectPath itemPath = new ModelObjectPath ( getAcctId(), getModelName (), Path.getRootPath ().makeChildItem ( itemName ) );
		try
		{
			final String childPath = S3Account.makeS3ObjectPath ( itemPath, false );
			try ( final InputStream is = getS3().getObject ( childPath ) )
			{
				return S3BackedObject.build ( S3ModelObject.class, getBaseContext ().withPath ( itemPath ), context.getOperator (), is );
			}
			catch ( IOException | BuildFailure x )
			{
				throw new ModelServiceIoException ( x );
			}
		}
		catch ( AmazonS3Exception e )
		{
			if ( e.getErrorCode ().equals ( "NoSuchKey" ) )	// FIXME: anything better than string compare?
			{
				throw new ModelItemDoesNotExistException ( itemPath );
			}
			else
			{
				throw new ModelServiceIoException ( e );
			}
		}
	}

	@Override
	public void store ( ModelRequestContext context, Name itemName, ModelObject o )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		final ModelObjectPath itemPath = new ModelObjectPath ( getAcctId(), getModelName (), Path.getRootPath ().makeChildItem ( itemName ) );
		checkUser ( context.getOperator (), exists ( context, itemName ) ? ModelOperation.UPDATE : ModelOperation.CREATE );
		getS3().putObject ( S3Account.makeS3ObjectPath ( itemPath, false ), o.asJson () );
	}

	String getAcctId () { return super.getBaseContext ().getPath ().getAcctId (); }
	String getModelName () { return super.getBaseContext ().getPath ().getModelName (); }
}
