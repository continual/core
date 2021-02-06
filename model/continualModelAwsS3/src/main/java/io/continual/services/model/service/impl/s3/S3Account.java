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
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelStdUserGroups;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelAccount;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class S3Account extends S3BackedObject implements ModelAccount
{
	public final String kAccountId = "acctId";

	public static S3Account fromJson ( JSONObject object, S3ModelLoaderContext mlc ) throws ModelServiceIoException
	{
		return new S3Account ( mlc, object );
	}

	S3Account ( S3ModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );
		fAcctId = bc.getPath ().getAcctId ();
	}

	@Override
	public JSONObject getLocalData ()
	{
		return new JSONObject ();
	}

	@Override
	public String getId ()
	{
		return fAcctId;
	}

	@Override
	public String getResourceDescription ()
	{
		return "model account " + fAcctId;
	}

	@Override
	public boolean doesModelExist ( ModelRequestContext mrc, String modelName )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		checkUser ( mrc.getOperator (), ModelOperation.READ );
		return getS3().exists ( makeS3ModelPath ( fAcctId, modelName, false ) );
	}

	@Override
	public List<String> getModelsInAccount ( ModelRequestContext mrc )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		checkUser ( mrc.getOperator (), ModelOperation.READ );

		final String acctPath = makeS3AcctPath ( fAcctId, true );

		final LinkedList<String> result = new LinkedList<> ();
		for ( String modelPath : getS3().getChildrenOf ( acctPath ) )
		{
			final Path p = Path.fromString ( "/" + modelPath );
			final Name modelName = p.getItemName ();
			result.add ( modelName.toString () );
		}
		return result;
	}

	@Override
	public S3Model initModel ( ModelRequestContext mrc, String modelName )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		final String modelPath = makeS3ModelPath ( fAcctId, modelName, false );
		if ( !getS3().exists ( modelPath ) )
		{
			checkUser ( mrc.getOperator (), ModelOperation.CREATE );

			log.info ( "Creating model [" + fAcctId + "/" + modelName + "]..." );
			getS3().putObject ( modelPath, getModelSetupData ( mrc.getOperator () ) );
		}
		return getModel ( mrc, modelName );
	}

	@Override
	public S3Model getModel ( ModelRequestContext mrc, String modelName )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		final String modelPath = makeS3ModelPath ( fAcctId, modelName, false );

		try ( final InputStream is = getS3().getObject ( modelPath ) )
		{
			return S3BackedObject.build ( S3Model.class,
				getBaseContext().withPath ( new ModelObjectPath ( fAcctId, modelName, null ) ),
				mrc.getOperator (), is );
		}
		catch ( AmazonS3Exception x )
		{
			return null;
		}
		catch ( IOException | BuildFailure x )	// FIXME: handle not found
		{
			throw new ModelServiceIoException ( x );
		}
	}

	private final String fAcctId;

	private static final Logger log = LoggerFactory.getLogger ( S3ModelService.class );

	private String getModelSetupData ( Identity identity )
	{
		return
			S3ModelObject.createBasicObjectJson (
				new AccessControlList ( null )
				.setOwner ( "root" )
				.addAclEntry (
					new AccessControlEntry ( identity.getId (), Access.PERMIT, new String[] {
						ModelOperation.READ.toString (),
						ModelOperation.CREATE.toString () 
					} ) )
					.addAclEntry (
						new AccessControlEntry ( ModelStdUserGroups.kSysAdminGroup, Access.PERMIT, new String[] {
							ModelOperation.READ.toString (),
							ModelOperation.CREATE.toString (),
							ModelOperation.UPDATE.toString (),
							ModelOperation.DELETE.toString ()
						} ) )
			);
	}
}
