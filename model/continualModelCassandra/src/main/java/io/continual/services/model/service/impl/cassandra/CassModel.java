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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class CassModel extends CassObjectContainer implements Model
{
	public static String getKeyspaceNameFor ( String acctId, String modelName )
	{
		return "model_" + acctId + "_" + modelName;
	}

	public static String getObjectTableName ( String acctId, String modelName )
	{
		return getKeyspaceNameFor ( acctId, modelName ) + ".objects";
	}

	public static String getRelationTableName ( String acctId, String modelName )
	{
		return getKeyspaceNameFor ( acctId, modelName ) + ".relns";
	}

	public static CassModel fromJson ( JSONObject object, CassModelLoaderContext mlc ) throws ModelServiceException
	{
		return new CassModel ( mlc, object );
	}

	CassModel ( CassModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );
	}

	@Override
	public String getId ()
	{
		return Path.getRootPath ()
			.makeChildItem ( Name.fromString ( getAcctId() ) )
			.makeChildItem ( Name.fromString ( getModelName () ) )
			.toString ()
		;
	}

	@Override
	public String getResourceDescription ()
	{
		return "Model " + getId ();
	}


	@Override
	public boolean exists ( ModelRequestContext context, Path id ) throws ModelServiceException, ModelRequestException
	{
		final ModelObjectPath mop = pathToFullPath ( id );
		if ( context.knownToNotExist ( mop ) ) return false;

		final String keyspace = getKeyspaceNameFor ( mop.getAcctId (), mop.getModelName () );

		final ResultSet rs = getBaseContext().getModelService ().runQuery ( "SELECT data FROM " + keyspace + ".objects WHERE path=?", id.toString () );
		final List<Row> rows = rs.all ();
		final boolean exists = !rows.isEmpty ();
		if ( !exists )
		{
			context.knownToNotExist ( mop );
		}
		return exists;
	}

	@Override
	public boolean exists ( ModelRequestContext context, Name itemName ) throws ModelServiceException, ModelRequestException
	{
		return exists ( context, Path.getRootPath ().makeChildItem ( itemName ) );
	}

	@Override
	public CassElementList getElementsBelow ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
	{
		return getElementsBelow ( context, Path.getRootPath () );
	}

	@Override
	public ModelObjectContainer load ( ModelRequestContext context, Path id ) throws ModelItemDoesNotExistException, ModelRequestException, ModelServiceException
	{
		// requesting the root node? return this model.
		if ( id == null || id.getParentPath () == null )
		{
			return this;
		}

		// otherwise, load the parent node, then load the item from there
		return load ( context, id.getParentPath () )
			.load ( context, id.getItemName () )
		;
	}

	@Override
	public void store ( ModelRequestContext context, Path id, ModelObject o )
		throws ModelRequestException, ModelServiceException
	{
		if ( id == null || id.getParentPath () == null )
		{
			throw new ModelRequestException ( "You cannot store to the model node." );
		}

		final Path parentPath = id.getParentPath ();
		final ModelObjectContainer parent = load ( context, parentPath );
		parent.store ( context, id.getItemName (), o );
	}

	@Override
	public void store ( ModelRequestContext context, Path id, String json )
		throws ModelRequestException, ModelServiceException
	{
		try
		{
			new JSONObject ( new CommentedJsonTokener ( json ) );
		}
		catch ( JSONException e )
		{
			throw new ModelRequestException ( e );
		}

		final JSONObject objData = new JSONObject ( new CommentedJsonTokener (
			CassModelObject.createBasicObjectJson ( context.getOperator ().getId () )
		) );
		JsonUtil.copyInto ( new JSONObject ( new CommentedJsonTokener ( json ) ), objData );

		store ( context, id, new ModelObject () {

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
			public String asJson () { return objData.toString (); }

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
		throws ModelRequestException, ModelServiceException
	{
		final ModelObject toUpdate;
		if ( exists ( context, id ) )
		{
			toUpdate = load ( context, id );
		}
		else
		{
			final String json = CassModelObject.createBasicObjectJson ( context.getOperator ().toString () );

			toUpdate = new ModelObject ()
			{
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
				public String asJson ()
				{
					return json;
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
		throws ModelServiceException, ModelRequestException
	{
		// remove this item by asking its parent to remove it
		return load ( context, id.getParentPath () )
			.remove ( context, id.getItemName () )
		;
	}

	@Override
	public void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> relns = new LinkedList<> ();
		relns.add ( reln );
		relate ( context, relns );
	}

	@Override
	public void relate ( ModelRequestContext context, Collection<ModelRelation> relns ) throws ModelServiceException, ModelRequestException
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
				throw new ModelRequestException ( "A relation may not span models." );
			}

			final ModelObjectContainer from = load ( context, reln.getFrom ().getObjectPath () );
			final ModelObjectContainer to = load ( context, reln.getTo ().getObjectPath () );

			from.checkUser ( context.getOperator (), ModelOperation.UPDATE );
			to.checkUser ( context.getOperator (), ModelOperation.UPDATE );
		}

		// now run the relation creation
		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();
		for ( ModelRelation reln : relns )
		{
			final String rev = buildReverseEntry ( reln.getTo ().getObjectPath (), reln.getName () );
			svc.runQuery (
				"INSERT INTO " + keyspace + ".relations ( fromPath, relation, toPath, reversed ) VALUES ( ?, ?, ?, ? )",
				reln.getFrom ().getObjectPath ().toString (),
				reln.getName (),
				reln.getTo ().getObjectPath ().toString (),
				rev
			);
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();

		final ResultSet rs = svc.runQuery (
			"DELETE FROM " + keyspace + ".relations WHERE fromPath=?, relation=?, toPath=?",
			reln.getFrom ().getObjectPath ().toString (),
			reln.getName (),
			reln.getTo ().getObjectPath ().toString ()
		);

		return rs.wasApplied ();
	}

	@Override
	public List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		// this has to be run as two queries because Cassandra has no "OR" operator.
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		result.addAll ( getInboundRelations ( context, forObject ) );
		result.addAll ( getOutboundRelations ( context, forObject ) );
		return result;
	}

	@Override
	public List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		if ( !exists ( context, forObject ) )
		{
			throw new ModelItemDoesNotExistException ( pathToFullPath ( forObject ) );
		}
		
		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();

		final ResultSet rs = svc.runQuery (
			"SELECT * FROM " + keyspace + ".relations WHERE toPath=?",
			forObject.toString ()
		);
		return rowSetToRelationList ( rs );
	}

	@Override
	public List<ModelRelation> getOutboundRelations ( ModelRequestContext context, final Path forObject ) throws ModelServiceException, ModelRequestException
	{
		if ( !exists ( context, forObject ) )
		{
			throw new ModelItemDoesNotExistException ( pathToFullPath ( forObject ) );
		}

		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();

		final ResultSet rs = svc.runQuery (
			"SELECT * FROM " + keyspace + ".relations WHERE fromPath=?",
			forObject.toString ()
		);
		return rowSetToRelationList ( rs );
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( !exists ( context, forObject ) )
		{
			throw new ModelItemDoesNotExistException ( pathToFullPath ( forObject ) );
		}

		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();

		final ResultSet rs = svc.runQuery (
			"SELECT * FROM " + keyspace + ".relations WHERE reversed=?",
			buildReverseEntry ( forObject, named )
		);
		return rowSetToRelationList ( rs );
	}

	@Override
	public List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( !exists ( context, forObject ) )
		{
			throw new ModelItemDoesNotExistException ( pathToFullPath ( forObject ) );
		}

		final String keyspace = getKeyspaceNameFor ( getAcctId (), getModelName () );
		final CassandraModelService svc = getBaseContext().getModelService ();

		final ResultSet rs = svc.runQuery (
			"SELECT * FROM " + keyspace + ".relations WHERE fromPath=? AND relation=?",
			forObject.toString (),
			named
		);
		return rowSetToRelationList ( rs );
	}

	@Override
	JSONObject getLocalData ()
	{
		return new JSONObject ();
	}

	@Override
	public String asJson ()
	{
		return toJson().toString ();
	}

	private static String buildReverseEntry ( Path objectPath, String name )
	{
		return new StringBuilder ()
			.append ( objectPath.toString () )
			.append ( "::" )
			.append ( name )
			.toString ()
		;
	}

	private List<ModelRelation> rowSetToRelationList ( ResultSet rs )
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		for ( Row row : rs )
		{
			final ModelObjectPath from = new ModelObjectPath ( getAcctId(), getModelName(), Path.fromString ( row.getString ( "fromPath" ) ) );
			final ModelObjectPath to = new ModelObjectPath ( getAcctId(), getModelName(), Path.fromString ( row.getString ( "toPath" ) ) );
			final String reln = row.getString ( "relation" );
			
			result.add ( new ModelRelation ()
			{
				@Override
				public ModelObjectPath getFrom () { return from; }

				@Override
				public ModelObjectPath getTo () { return to; }

				@Override
				public String getName () { return reln; }
			} );
		}
		return result;
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
