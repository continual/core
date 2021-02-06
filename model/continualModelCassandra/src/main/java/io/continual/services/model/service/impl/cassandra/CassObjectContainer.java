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

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONObject;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelObjectContainer;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public abstract class CassObjectContainer extends CassBackedObject implements ModelObjectContainer
{
	CassObjectContainer ( CassModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );
	}

	/**
	 * Get the elements below the given parent path
	 * @param context
	 * @param parentPath
	 * @return
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	public CassElementList getElementsBelow ( ModelRequestContext context, Path parentPath ) throws ModelServiceRequestException, ModelServiceIoException
	{
		final ModelObjectPath mop = pathToFullPath ( parentPath );
		final String keyspace = CassModel.getKeyspaceNameFor ( mop.getAcctId (), mop.getModelName () );

		final TreeSet<String> names = new TreeSet<> ();
		final ResultSet rs = getBaseContext().getModelService ().runQuery ( "SELECT path FROM " + keyspace + ".objects WHERE parentPath=?",
			parentPath.toString () );
		for ( Row row : rs )
		{
			final Path child = Path.fromString ( row.getString ( "path" ) );
			names.add ( child.getItemName ().toString () );
		}

		return new CassElementList ( parentPath, names );
	}

	@Override
	public ModelObjectContainer load ( ModelRequestContext context, Name itemName )
		throws ModelItemDoesNotExistException, ModelServiceRequestException, ModelServiceIoException
	{
		final ModelObjectPath itemPath = getBaseContext().getPath ().makeChildItem ( itemName );
		if ( context.knownToNotExist ( itemPath ) ) throw new ModelItemDoesNotExistException ( itemPath );

		final String keyspace = CassModel.getKeyspaceNameFor ( itemPath.getAcctId (), itemPath.getModelName () );

		final ResultSet rs = getBaseContext().getModelService ().runQuery ( "SELECT data FROM " + keyspace + ".objects WHERE path=?",
			itemPath.getObjectPath ().toString () );
		final List<Row> rows = rs.all ();
		if ( rows.size () == 0 )
		{
			context.knownToNotExist ( itemPath );
			throw new ModelItemDoesNotExistException ( itemPath );
		}

		if ( rows.size () > 1 )
		{
			throw new ModelServiceIoException ( "Query for " + itemPath.toString () + " returned multiple objects" );
		}

		try
		{
			final String targetClass = CassModelObject.class.getName ();	// FIXME: load from row eventually
			final JSONObject data = JsonUtil.readJsonObject ( rows.get ( 0 ).getString ( "data" ) );
			final ModelObjectContainer result = CassBackedObject.build ( CassModelObject.class,
				targetClass,
				getBaseContext ().withPath ( itemPath ),
				context.getOperator (),
				data
			);
			result.checkUser ( context.getOperator (), ModelOperation.READ );
			return result;
		}
		catch ( IOException | BuildFailure x )
		{
			throw new ModelServiceIoException ( x );
		}
	}

	@Override
	public void store ( ModelRequestContext context, Name itemName, ModelObject o )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		checkUser ( context.getOperator (), exists ( context, itemName ) ? ModelOperation.UPDATE : ModelOperation.CREATE );

		final ModelObjectPath itemPath = getBaseContext ().getPath ().makeChildItem ( itemName );

		final String keyspace = CassModel.getKeyspaceNameFor ( itemPath.getAcctId (), itemPath.getModelName () );
		final String path = itemPath.getObjectPath ().getId ();
		final String data = o.asJson ();
		final String parent = itemPath.getObjectPath ().getParentPath ().toString ();

		getBaseContext().getModelService ().runQuery (
			"INSERT INTO " + keyspace + ".objects ( path, data, parentPath ) VALUES ( ?, ?, ? )",
			path, data, parent );
	}

	@Override
	public boolean remove ( ModelRequestContext context, Name itemName )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		if ( !exists ( context, itemName ) ) return false;

		// in keeping with a POSIX-like structure, we check write access to the container directory
		// but don't care about permissions on the item itself

		checkUser ( context.getOperator (), ModelOperation.UPDATE );

		boolean changed = false;

		// get the child...
		final ModelObjectContainer child = load ( context, itemName );

		// ask the child to remove everything below itself...
		for ( Path grandChild : child.getElementsBelow ( context ).getElements () )
		{
			changed |= child.remove ( context, grandChild.getItemName () );
		}

		// now remove this object...
		final ModelObjectPath itemPath = getBaseContext ().getPath ().makeChildItem ( itemName );
		final String keyspace = CassModel.getKeyspaceNameFor ( itemPath.getAcctId (), itemPath.getModelName () );
		final String path = itemPath.getObjectPath ().getId ();
		ResultSet rs = getBaseContext().getModelService ().runQuery ( "DELETE FROM " + keyspace + ".objects WHERE path=?", path );
		changed |= rs.wasApplied ();

		// and its relations...
		// for toPath, we have to query for primary key in order to delete
		rs = getBaseContext().getModelService ().runQuery ( "SELECT fromPath, relation, toPath FROM " + keyspace + ".relations WHERE toPath=?", path );
		for ( Row row : rs.all () )
		{
			final ResultSet inner = getBaseContext().getModelService ().runQuery ( "DELETE FROM " + keyspace + ".relations WHERE fromPath=? AND relation=? AND toPath=?",
				row.getString ( "fromPath" ), row.getString ( "relation" ), row.getString ( "toPath" ) );
			changed |= inner.wasApplied ();
		}
		rs = getBaseContext().getModelService ().runQuery ( "DELETE FROM " + keyspace + ".relations WHERE fromPath=?", path );
		changed |= rs.wasApplied ();

		return changed;
	}

	String getAcctId () { return super.getBaseContext ().getPath ().getAcctId (); }
	String getModelName () { return super.getBaseContext ().getPath ().getModelName (); }

	ModelObjectPath pathToFullPath ( Path id )
	{
		return new ModelObjectPath (
			getAcctId (),
			getModelName (),
			id
		);
	}
}
