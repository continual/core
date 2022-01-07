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

import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelStdUserGroups;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.service.Model;
import io.continual.services.model.service.ModelAccount;
import io.continual.util.data.json.JsonUtil;

public class CassAccount extends CassBackedObject implements ModelAccount
{
	public static CassAccount fromJson ( JSONObject object, CassModelLoaderContext mlc ) throws ModelServiceException
	{
		return new CassAccount ( mlc, object );
	}

	CassAccount ( CassModelLoaderContext bc, JSONObject data )
	{
		super ( bc, data );

		fAcctId = bc.getPath ().getAcctId ();
	}

	@Override
	public String getResourceDescription ()
	{
		return "Model Account " + fAcctId;
	}

	@Override
	JSONObject getLocalData ()
	{
		return new JSONObject ();
	}

	@Override
	public String getId ()
	{
		return fAcctId;
	}

	@Override
	public boolean doesModelExist ( ModelRequestContext mrc, String modelName ) throws ModelServiceException, ModelRequestException
	{
		final ResultSet rs = getBaseContext().getModelService ().runQuery ( "SELECT data FROM continual.models WHERE acctId=? AND modelName=?", fAcctId, modelName );
		final List<Row> rows = rs.all ();
		return !rows.isEmpty ();
	}

	@Override
	public List<String> getModelsInAccount ( ModelRequestContext mrc ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<String> result = new LinkedList<> ();

		final ResultSet rs = getBaseContext().getModelService ().runQuery ( "SELECT modelName FROM continual.models WHERE acctId=?", fAcctId );
		for ( Row row : rs )
		{
			result.add ( row.getString ( "modelName" ) );
		}
		return result;
	}

	@Override
	public Model initModel ( ModelRequestContext mrc, String modelName ) throws ModelServiceException, ModelRequestException
	{
		if ( doesModelExist ( mrc, modelName ) )
		{
			throw new ModelRequestException ( "Model " + modelName + " already exists. It must be explicitly deleted." );
		}

		// get the new keyspace name
		final String keyspace = makeKeyspaceNameFor ( modelName );
		if ( keyspace.length () > 48 )
		{
			throw new ModelRequestException ( "The model name is too long." );
		}

		log.info ( "Initializing model " + fAcctId + " / " + modelName );

		try
		{
			// create the new keyspace and tables
			final CassandraModelService svc = getBaseContext().getModelService ();
			svc.runQuery ( "CREATE KEYSPACE " + keyspace + " WITH REPLICATION = { 'class':'SimpleStrategy', 'replication_factor':1 }" );
			svc.runQuery ( "CREATE TABLE " + keyspace + ".meta ( key TEXT, data TEXT, PRIMARY KEY ( key ) )" );
			svc.runQuery ( "CREATE TABLE " + keyspace + ".objects ( path TEXT, parentPath TEXT, data TEXT, PRIMARY KEY ( path ) )" );
			svc.runQuery ( "CREATE INDEX parentPaths ON " + keyspace + ".objects ( parentPath )" );
			svc.runQuery ( "CREATE TABLE " + keyspace + ".relations ( fromPath TEXT, relation TEXT, toPath TEXT, reversed TEXT, PRIMARY KEY ( fromPath, relation, toPath ) )" );
			svc.runQuery ( "CREATE INDEX revRelns ON " + keyspace + ".relations ( reversed )" );
			svc.runQuery ( "CREATE INDEX toPaths ON " + keyspace + ".relations ( toPath )" );

			final String modelData = getModelSetupData ( mrc.getOperator ().getId () );
			svc.runQuery ( "INSERT INTO continual.models ( acctId, modelName, data ) VALUES ( ?, ?, ? )", fAcctId, modelName, modelData );
		}
		catch ( ModelServiceException e )
		{
			log.error ( "Problem with model creation: " + e.getMessage (), e );
			throw e;
		}
		
		return getModel ( mrc, modelName );
	}

	@Override
	public Model getModel ( ModelRequestContext mrc, String modelName ) throws ModelServiceException, ModelRequestException
	{
		final ResultSet rs = getBaseContext().getModelService ().runQuery (
			"SELECT data FROM continual.models WHERE acctId=? AND modelName=?",
			fAcctId,
			modelName
		);
		final List<Row> rows = rs.all ();
		if ( rows.size () < 1 )
		{
			throw new ModelRequestException ( "Model " + modelName + " was not found." );
		}

		if ( rows.size() > 1 )
		{
			throw new ModelServiceException ( "Multiple model records found." );
		}

		try
		{
			final String targetClass = CassModel.class.getName ();	// FIXME: load from row eventually
			final JSONObject data = JsonUtil.readJsonObject ( rows.get ( 0 ).getString ( "data" ) );
			return CassBackedObject.build ( CassModel.class,
				targetClass,
				getBaseContext().withPath ( new ModelObjectPath ( fAcctId, modelName, null ) ),
				mrc.getOperator (),
				data
			);
		}
		catch ( BuildFailure e )
		{
			throw new ModelRequestException ( e );
		}
	}

	private final String fAcctId;
	private static final Logger log = LoggerFactory.getLogger ( CassandraModelService.class );

	private String makeKeyspaceNameFor ( String modelName )
	{
		return CassModel.getKeyspaceNameFor ( fAcctId, modelName );
	}

//	private void storeAccount () throws ModelServiceIoException
//	{
//		getBaseContext().getModelService ().runQuery ( "UPDATE continual.accounts SET data=? WHERE acctId=?", toJson().toString(), fAcctId );
//	}

	private String getModelSetupData ( String ownerId )
	{
		return
			CassModelObject.createBasicObjectJson (
				new AccessControlList ( null )
					.setOwner ( "root" )
					.addAclEntry (
						new AccessControlEntry ( ownerId, Access.PERMIT, new String[] {
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
