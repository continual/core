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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelObjectUpdater;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.Model;
import io.continual.services.model.service.ModelObjectContainer;
import io.continual.services.model.service.ModelRelation;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class S3Model extends S3ObjectContainer implements Model
{
	public static S3Model fromJson ( JSONObject object, S3ModelLoaderContext mlc ) throws ModelServiceIoException
	{
		return new S3Model ( mlc, object );
	}

	S3Model ( S3ModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );
	}

	@Override
	public String getId ()
	{
		return S3Account.makeS3ModelId ( getAcctId(), getModelName() );
	}

	@Override
	public String getResourceDescription ()
	{
		return "Model " + getModelName();
	}


	@Override
	public boolean exists ( ModelRequestContext context, Path id ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final ModelObjectPath mop = pathToFullPath ( id );
		if ( context.knownToNotExist ( mop ) ) return false;

		final String s3Path = S3Account.makeS3ObjectPath ( mop, false );
		return getS3().exists ( s3Path );
	}

	@Override
	public boolean exists ( ModelRequestContext context, Name itemName ) throws ModelServiceIoException, ModelServiceRequestException
	{
		return exists ( context, Path.getRootPath ().makeChildItem ( itemName ) );
	}

	@Override
	public S3ElementList getElementsBelow ( ModelRequestContext context ) throws ModelServiceRequestException, ModelServiceIoException
	{
		return new S3ElementList ( Path.getRootPath (),
			getS3().getChildrenOf ( S3Account.makeS3ModelPath ( getAcctId(), getModelName(), true ) )
		);
	}

	@Override
	public ModelObjectContainer load ( ModelRequestContext context, Path id ) throws ModelItemDoesNotExistException, ModelServiceRequestException, ModelServiceIoException
	{
		// requesting the root node?
		if ( id == null || id.getParentPath () == null )
		{
			// get the model, which is an object container
			final String baseModelPath = S3Account.makeS3ModelPath ( getAcctId(), getModelName(), false );
			try ( final InputStream is = getS3().getObject ( baseModelPath ) )
			{
				return S3BackedObject.build ( S3Model.class, getBaseContext (), context.getOperator (), is );
			}
			catch ( IOException | BuildFailure x )	// FIXME: handle not found
			{
				throw new ModelServiceIoException ( x );
			}
			catch ( AmazonS3Exception e )
			{
				if ( e.getErrorCode ().equals ( "NoSuchKey" ) )	// FIXME: anything better than string compare?
				{
					final ModelObjectPath itemPath = new ModelObjectPath ( getAcctId(), getModelName (), Path.getRootPath () );
					throw new ModelItemDoesNotExistException ( itemPath );
				}
				else
				{
					throw new ModelServiceIoException ( e );
				}
			}
		}

		// load the parent node, then load the item from it
		return load ( context, id.getParentPath () )
			.load ( context, id.getItemName () )
		;
	}

	@Override
	public void store ( ModelRequestContext context, Path id, ModelObject o )
		throws ModelServiceRequestException, ModelServiceIoException
	{
		if ( id == null || id.getParentPath () == null )
		{
			throw new ModelServiceRequestException ( "You cannot store to the model node." );
		}

		final Path parentPath = id.getParentPath ();
		final ModelObjectContainer parent = load ( context, parentPath );
		parent.store ( context, id.getItemName (), o );
	}

	@Override
	public void store ( ModelRequestContext context, Path id, String json )
		throws ModelServiceRequestException, ModelServiceIoException
	{
		try
		{
			new JSONObject ( new CommentedJsonTokener ( json ) );
		}
		catch ( JSONException e )
		{
			throw new ModelServiceRequestException ( e );
		}

		final JSONObject objData = new JSONObject ( new CommentedJsonTokener (
			S3ModelObject.createBasicObjectJson ( context.getOperator ().getId () )
		) );
		JsonUtil.copyInto ( new JSONObject ( new CommentedJsonTokener ( json ) ), objData );

		store ( context, id, new ModelObject () {

			@Override
			public String asJson () { return objData.toString (); }

			@Override
			public AccessControlList getAccessControlList ()
			{
				// TODO Auto-generated method stub
				// FIXME
				return null;
			}

			@Override
			public String getId ()
			{
				// TODO Auto-generated method stub
				// FIXME
				return null;
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
		} );
	}

	@Override
	public void update ( ModelRequestContext context, Path id, ModelObjectUpdater updater )
		throws ModelServiceRequestException, ModelServiceIoException
	{
		final ModelObject toUpdate;
		if ( exists ( context, id ) )
		{
			toUpdate = load ( context, id );
		}
		else
		{
			final String json = S3ModelObject.createBasicObjectJson ( context.getOperator ().toString () );

			toUpdate = new ModelObject ()
			{
				@Override
				public String asJson ()
				{
					return json;
				}

				@Override
				public AccessControlList getAccessControlList ()
				{
					// TODO Auto-generated method stub
					// FIXME
					return null;
				}

				@Override
				public String getId ()
				{
					// TODO Auto-generated method stub
					// FIXME
					return null;
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
			};
		}
		final ModelObject resultingObject = updater.update ( toUpdate );
		store ( context, id, resultingObject );
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path id )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		return load ( context, id.getParentPath () )
			.remove ( context, id.getItemName () )
		;
	}

	@Override
	public boolean remove ( ModelRequestContext context, Name itemName )
		throws ModelServiceIoException,
			ModelServiceAccessException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> relns = new LinkedList<> ();
		relns.add ( reln );
		relate ( context, relns );
	}

	@Override
	public void relate ( ModelRequestContext context, Collection<ModelRelation> relns ) throws ModelServiceIoException, ModelServiceRequestException
	{
		// check validity of inbound relations
		for ( ModelRelation reln : relns )
		{
			if (
				!reln.getFrom ().getAcctId ().equals ( getAcctId () ) ||
				!reln.getTo ().getAcctId ().equals ( getAcctId () ) ||
				!reln.getFrom ().getModelName ().equals ( getModelName () ) ||
				!reln.getTo ().getModelName ().equals ( getModelName () )
			)
			{
				throw new ModelServiceRequestException ( "A relation may not span models." );
			}

			final ModelObjectContainer from = load ( context, reln.getFrom ().getObjectPath () );
			final ModelObjectContainer to = load ( context, reln.getTo ().getObjectPath () );

			from.checkUser ( context.getOperator (), ModelOperation.UPDATE );
			to.checkUser ( context.getOperator (), ModelOperation.UPDATE );
		}

		// now run the relation creation
//		for ( ModelRelation reln : relns )
//		{
//			final ModelObjectContainer from = load ( context, reln.getFrom ().getObjectPath () );
//			final ModelObjectContainer to = load ( context, reln.getTo ().getObjectPath () );
//
//			// FIXME how to implement this in S3?
//		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException
	{
		return false;
	}

	@Override
	public List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		return result;
	}

	@Override
	public List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		return result;
	}

	@Override
	public List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		return result;
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		return result;
	}

	@Override
	public List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		return result;
	}

	@Override
	JSONObject getLocalData ()
	{
		return new JSONObject ();
	}

	private ModelObjectPath pathToFullPath ( Path id )
	{
		return new ModelObjectPath (
			getAcctId (),
			getModelName (),
			id
		);
	}

	@Override
	public String asJson ()
	{
		return toJson().toString ();
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
