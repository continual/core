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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.DriverException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelRequestContext.CacheControl;
import io.continual.services.model.core.ModelStdUserGroups;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelLimitsAndCaps;
import io.continual.services.model.service.ModelService;
import io.continual.util.nv.NvReadable;

public class CassandraModelService extends SimpleService implements ModelService
{
	public static final String kSvcName = "CassandraModelService";

	public static final String kSetting_CassandraContactPoint = "cassandra.contactPoint";
	public static final String kDefault_CassandraContactPoint = "127.0.0.1";


	public CassandraModelService ( ServiceContainer sc, NvReadable settings ) throws NvReadable.MissingReqdSettingException
	{
		fServiceContainer = sc;
		fSettings = settings;

		final String cassandraClusterCp = settings.getString ( kSetting_CassandraContactPoint, kDefault_CassandraContactPoint );

		final Cluster cluster = Cluster
			.builder ()
			.addContactPoint ( cassandraClusterCp )
			.build ()
		;
		fCassandra = cluster.newSession ();

		fBaseContext = new CassModelLoaderContext ( fServiceContainer, this, fSettings );
	}

	@Override
	public ModelLimitsAndCaps getLimitsAndCaps ()
	{
		return new ModelLimitsAndCaps ()
		{
			@Override
			public long getMaxSerializedObjectLength ()
			{
				// see https://docs.datastax.com/en/cql/3.3/cql/cql_reference/refLimits.html
				return 1024 * 1000 * 1000 * 1;	// 1GB (could be 2, but then I'd have to make sure I calculated it equivalently)
			}
		};
	}

	/**
	 * Create a request context builder
	 */
	@Override
	public RequestContextBuilder buildRequestContext ()
	{
		return new RequestContextBuilder ()
		{
			@Override
			public RequestContextBuilder forUser ( Identity forWhom )
			{
				fIdentity = forWhom;
				return this;
			}

			@Override
			public RequestContextBuilder usingCache ( CacheControl caching )
			{
				fCache = caching;
				return this;
			}

			@Override
			public ModelRequestContext build ()
			{
				return new CassModelRequestContext ( fIdentity, fCache );
			}

			private Identity fIdentity;
			private CacheControl fCache;
		};
	}

	@Override
	public CassAccount createAccount ( ModelRequestContext mrc, String acctId, String ownerId ) throws ModelServiceIoException, ModelServiceRequestException
	{
		// FIXME-SECURITY: check admin access

		// FIXME: cassandra keyspaces are lowercased, so make sure there's not a case-related collision possibility here
		
		try
		{
			final ResultSet rs = runQuery ( "INSERT INTO continual.accounts ( acctId, data ) VALUES ( ?, ? ) IF NOT EXISTS",
				acctId, getAccountSetupData ( ownerId ) );
			if ( !rs.wasApplied () )
			{
				throw new ModelServiceRequestException ( "Account [" + acctId + "] exists. Use PATCH or explicitly DELETE it." );
			}
		}
		catch ( DriverException x )
		{
			throw new ModelServiceIoException ( x );
		}

		return getAccount ( mrc, acctId );
	}

	@Override
	public List<String> getAccounts ( ModelRequestContext mrc ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<String> result = new LinkedList <> ();
		try
		{
			final ResultSet rs = runQuery ( "SELECT acctId FROM continual.accounts" );
			for ( Row row : rs )
			{
				result.add ( row.getString ( "acctId" ) );
			}
		}
		catch ( DriverException x )
		{
			throw new ModelServiceIoException ( x );
		}
		return result;
	}

	@Override
	public CassAccount getAccount ( ModelRequestContext mrc, String acctId ) throws ModelServiceIoException, ModelServiceRequestException
	{
		try
		{
			final ResultSet rs = runQuery ( "SELECT acctId, data FROM continual.accounts WHERE acctId=?", acctId );
			final List<Row> row = rs.all ();

			if ( row.isEmpty () ) return null;
			if ( row.size () > 1 )
			{
				throw new ModelServiceIoException ( "Unexpected results for account load: Query for " + acctId + " produced " + row.size () + " records." );
			}

			final Row data = row.get ( 0 );
			return CassBackedObject.build (
				CassAccount.class,
				fBaseContext.withPath ( new ModelObjectPath ( acctId, null, null ) ),
				mrc.getOperator (),
				new ByteArrayInputStream ( data.getString ( "data" ).getBytes ( "UTF-8" ) )
			);
		}
		catch ( DriverException | UnsupportedEncodingException | BuildFailure x )
		{
			throw new ModelServiceIoException ( x );
		}
	}

	private final ServiceContainer fServiceContainer;
	private final NvReadable fSettings;
	private final CassModelLoaderContext fBaseContext;
	private Session fCassandra;

	ResultSet runQuery ( String query, Object... args ) throws ModelServiceIoException
	{
		try
		{
			final SimpleStatement statement = new SimpleStatement ( query, args );
			return fCassandra.execute ( statement );
		}
		catch ( DriverException x )
		{
			throw new ModelServiceIoException ( x );
		}
	}
	
	private String getAccountSetupData ( String ownerId )
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
