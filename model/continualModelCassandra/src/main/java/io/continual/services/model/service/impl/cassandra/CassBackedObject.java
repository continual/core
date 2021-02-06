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

import java.io.InputStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessException;
import io.continual.iam.access.AclChecker;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;

abstract class CassBackedObject implements JsonSerialized
{
	public static final String kMetadataTag = "᚜ ⓜⅇτα ᚛";
	public static final String kAclTag = "acl";

	@Override
	public String toString ()
	{
		return fBaseContext.getPath ().toString ();
	}

	/**
	 * Typical object build. Loads the input stream as JSON, constructs the object, and checks for READ rights.
	 * @param clazz
	 * @param ctx
	 * @param user
	 * @param is
	 * @return an instance of clazz
	 * @throws BuildFailure
	 * @throws ModelServiceAccessException
	 * @throws ModelServiceIoException
	 */
	static <T extends CassBackedObject> T build ( Class<T> clazz, CassModelLoaderContext ctx, Identity user, InputStream is ) throws BuildFailure, ModelServiceAccessException, ModelServiceIoException
	{
		// get the object data
		final T moc = Builder.withBaseClass ( clazz )
			.usingClassName ( clazz.getName () )
			.readingJsonData ( is )
			.providingContext ( ctx )
			.build ()
		;
		moc.checkUser ( user, ModelOperation.READ );
		return moc;
	}
	
	/**
	 * Typical object build. Loads the input stream as JSON, constructs the object, and checks for READ rights.
	 * @param clazz
	 * @param ctx
	 * @param user
	 * @param json
	 * @return an instance of clazz
	 * @throws BuildFailure
	 * @throws ModelServiceAccessException
	 * @throws ModelServiceIoException
	 */
	static <T extends CassBackedObject> T build ( Class<T> clazz, String targetClass, CassModelLoaderContext ctx, Identity user, JSONObject json ) throws BuildFailure, ModelServiceAccessException, ModelServiceIoException
	{
		// get the object data
		final T moc = Builder.withBaseClass ( clazz )
			.usingClassName ( targetClass )
			.usingData ( json )
			.providingContext ( ctx )
			.build ()
		;
		moc.checkUser ( user, ModelOperation.READ );
		return moc;
	}

	CassBackedObject ( CassModelLoaderContext bc, JSONObject data )
	{
		fBaseContext = bc;

		final JSONObject metadataBlock = data.optJSONObject ( kMetadataTag );

		final JSONObject aclBlock = metadataBlock == null ? null : metadataBlock.optJSONObject ( kAclTag );
		fAcl = AccessControlList.deserialize ( aclBlock,
			new AclUpdateListener ()
			{
				@Override
				public void onAclUpdate ( AccessControlList accessControlList )
				{
					log.warn ( "acl updated unexpected; onAclUpdate caled in S3BackedObject" );
				}
			}
		);
	}

	public boolean canUser ( Identity operator, ModelOperation op )
	{
		try
		{
			checkUser ( operator, op );
			return true;
		}
		catch ( ModelServiceAccessException | ModelServiceIoException e )
		{
			return false;
		}
	}

	public void checkUser ( Identity operator, ModelOperation op ) throws ModelServiceAccessException, ModelServiceIoException
	{
		try
		{
			new AclChecker ()
				.forUser ( operator )
				.performing ( op.toString () )
				.onResource ( getResourceDescription () )
				.controlledByAcl ( fAcl )
				.check ()
			;
		}
		catch ( IamSvcException e )
		{
			throw new ModelServiceIoException ( e );
		}
		catch ( AccessException e )
		{
			throw new ModelServiceAccessException ( e );
		}
	}

	public abstract String getResourceDescription ();
	abstract JSONObject getLocalData ();

	CassModelLoaderContext getBaseContext ()
	{
		return fBaseContext;
	}
	
	public AccessControlList getAccessControlList ()
	{
		return fAcl;
	}

	@Override
	public JSONObject toJson ()
	{
		return toJson ( getLocalData (), fAcl, true );
	}

	static JSONObject toJson ( JSONObject data, AccessControlList acl, boolean withMetadata )
	{
		final JSONObject o = JsonUtil.clone ( data );
		if ( withMetadata )
		{
			o.put ( kMetadataTag,
				new JSONObject ()
					.put ( kAclTag, acl.asJson () )
				);
		}
		return o;
	}

	private final CassModelLoaderContext fBaseContext;
	private final AccessControlList fAcl;

	private static final Logger log = LoggerFactory.getLogger ( CassandraModelService.class );
}
