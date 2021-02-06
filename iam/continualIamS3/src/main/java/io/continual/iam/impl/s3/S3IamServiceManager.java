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
package io.continual.iam.impl.s3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.AccessManager;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.iam.impl.common.jwt.JwtValidator;
import io.continual.iam.tags.TagManager;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class S3IamServiceManager extends SimpleService implements IamServiceManager<CommonJsonIdentity,CommonJsonGroup>, Service
{
	public S3IamServiceManager ( ServiceContainer sc, JSONObject settings ) throws IamSvcException, BuildFailure
	{
		final String sysAdminGroup = settings.optString ( "sysAdminGroup", "sysadmin" );

		final JSONObject jwt = settings.optJSONObject ( "jwt" );

		JwtProducer p = null;
		if ( jwt != null )
		{
			final String jwtIssuer = jwt.optString ( "issuer", null );
			final String jwtSecret = jwt.optString ( "sha256Key", null );
			if ( jwtIssuer != null && jwtSecret != null )
			{
				p = new JwtProducer.Builder ()
					.withIssuerName ( jwtIssuer )
					.usingSigningKey ( jwtSecret )
					.build ()
				;
			}
		}

		final ExpressionEvaluator evaluator = sc.getExprEval ( settings );

		// get the AWS settings
		final JSONObject aws = settings.getJSONObject ( "aws" );

		fDb = new S3IamDb.Builder ()
			.withAccessKey ( evaluator.evaluateText ( aws.getString ( "accessKey" ) ) )
			.withSecretKey ( evaluator.evaluateText ( aws.getString ( "secretKey" ) ) )
			.withBucket ( evaluator.evaluateText ( settings.getString ( "bucketId" ) ) )
			.withPathPrefix ( evaluator.evaluateText ( settings.optString ( "pathPrefix", "" ) ) )
			.usingAclFactory ( new AclFactory ()
			{
				@Override
				public AccessControlList createDefaultAcl ( AclUpdateListener acll )
				{
					final AccessControlList acl = new AccessControlList ( acll );
					acl
						.permit ( sysAdminGroup, AccessControlList.READ )
						.permit ( sysAdminGroup, AccessControlList.UPDATE )
						.permit ( sysAdminGroup, AccessControlList.CREATE )
						.permit ( sysAdminGroup, AccessControlList.DELETE )
					;
					return acl;
				}
			} )
			.withJwtProducer ( p )
			.build ()
		;

		// optionally add 3rd party JWT validators to the db
		if ( jwt != null )
		{
			final JSONArray auths = jwt.optJSONArray ( "thirdPartyAuth" );
			JsonVisitor.forEachElement ( auths, new ArrayVisitor<JSONObject,BuildFailure> ()
			{
				@Override
				public boolean visit ( JSONObject authEntry ) throws JSONException,BuildFailure
				{
					final String keys = authEntry.optString ( "keys" );
					
					final JwtValidator v = new JwtValidator.Builder ()
						.named ( authEntry.optString ( "name", "(anonymous)" ) )
						.forIssuer ( authEntry.getString ( "issuer" ) )
						.forAudience ( authEntry.getString ( "audience" ) )
						.getPublicKeysFrom ( keys )
						.build ()
					;
					fDb.addJwtValidator ( v );
					return true;
				}
			} );
		}
	}

	@Override
	public IdentityDb<CommonJsonIdentity> getIdentityDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessDb<CommonJsonGroup> getAccessDb () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public IdentityManager<CommonJsonIdentity> getIdentityManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public AccessManager<CommonJsonGroup> getAccessManager () throws IamSvcException
	{
		return fDb;
	}

	@Override
	public TagManager getTagManager () throws IamSvcException
	{
		return fDb;
	}

	private final S3IamDb fDb;
}
