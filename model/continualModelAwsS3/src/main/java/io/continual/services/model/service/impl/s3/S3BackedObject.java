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

import java.io.InputStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclChecker;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;

abstract class S3BackedObject implements JsonSerialized
{
	public static final String kMetadataTag = "᚜ ⓜⅇτα ᚛";
	public static final String kAclTag = "acl";

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
	static <T extends S3BackedObject> T build ( Class<T> clazz, S3ModelLoaderContext ctx, Identity user, InputStream is ) throws BuildFailure, ModelServiceAccessException, ModelServiceIoException
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
	
	S3BackedObject ( S3ModelLoaderContext bc, JSONObject data )
	{
		fS3 = bc.getS3Interface ();
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
		catch ( io.continual.iam.access.AccessException e )
		{
			throw new ModelServiceAccessException ( e );
		}
	}

	public abstract String getResourceDescription ();
	abstract JSONObject getLocalData ();

	S3Interface getS3 ()
	{
		return fS3;
	}

	S3ModelLoaderContext getBaseContext ()
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

	public static String makeS3AcctPath ( String acctId, boolean asFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( acctId );
		if ( asFolder )
		{
			sb.append ( "/" );
		}
		return sb.toString ();
	}

	public static String makeS3ModelId ( String acctId, String model )
	{
		return makeS3AcctPath ( acctId, true ) + model;
	}

	public static String makeS3ModelPath ( String acctId, String model, boolean asFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( makeS3ModelId ( acctId, model ) );
		if ( asFolder )
		{
			sb.append ( "/" );
		}
		return sb.toString ();
	}

	public static String makeS3ObjectPath ( ModelObjectPath path, boolean asFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( makeS3ModelPath ( path.getAcctId (), path.getModelName (), false ) );
		sb.append ( path.getObjectPath ().toString () );
		if ( asFolder )
		{
			sb.append ( "/" );
		}
		return sb.toString ();
	}

	private final S3Interface fS3;
	private final S3ModelLoaderContext fBaseContext;
	private final AccessControlList fAcl;

	private static final Logger log = LoggerFactory.getLogger ( S3ModelService.class );
}
