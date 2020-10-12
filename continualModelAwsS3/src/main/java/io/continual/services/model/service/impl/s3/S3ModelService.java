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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

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
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelLimitsAndCaps;
import io.continual.services.model.service.ModelService;
import io.continual.util.nv.NvReadable;

/**
 * An S3 model service.
 */
public class S3ModelService extends SimpleService implements ModelService
{
	public static final String kSvcName = "S3ModelService";

	public static final String kSetting_AwsApiKey = "s3model.aws.accessKey";
	public static final String kSetting_AwsApiSecret = "s3model.aws.secretKey";

	public static final String kSetting_Bucket = "s3model.aws.s3.bucket";
	public static final String kSetting_Prefix = "s3model.aws.s3.pathPrefix";

	@SuppressWarnings("deprecation")
	public S3ModelService ( ServiceContainer sc, NvReadable settings ) throws NvReadable.MissingReqdSettingException
	{
		fServiceContainer = sc;
		fSettings = settings;

		final String apiKey = settings.getString ( kSetting_AwsApiKey );
		final String privateKey = settings.getString ( kSetting_AwsApiSecret );

		fS3 = new S3Interface (
			new AmazonS3Client ( new S3Creds ( apiKey, privateKey ) ),
			settings.getString ( kSetting_Bucket ),
			settings.getString ( kSetting_Prefix, "" )
		);

		fBaseContext = new S3ModelLoaderContext ( fServiceContainer, this, fS3, fSettings );
	}

	@Override
	public ModelLimitsAndCaps getLimitsAndCaps ()
	{
		return new ModelLimitsAndCaps ()
		{
			@Override
			public long getMaxSerializedObjectLength ()
			{
				// see https://aws.amazon.com/s3/faqs/
				return 1024 * 1000 * 1000 * 4;	// 4GB (could be 5, but then I'd have to make sure I calculated it equivalently)
			}
		};
	}

	@Override
	public S3Account createAccount ( ModelRequestContext mrc, String acctId, String ownerId ) throws ModelServiceIoException, ModelServiceRequestException
	{
		// FIXME-SECURITY: check admin access

		final String modelPath = makeS3AcctPath ( acctId, true );

		// if the account exists, that's an error
		if ( fS3.exists ( modelPath ) )
		{
			return null;
		}

		fS3.putObject ( modelPath, getAccountSetupData ( ownerId ) );

		return getAccount ( mrc, acctId );
	}

	@Override
	public List<String> getAccounts ( ModelRequestContext mrc ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<String> result = new LinkedList<> ();
		for ( String child : fS3.getTopLevelItems () )	// FIXME: truncation
		{
			result.add ( removeTrailingSlash ( child ) );
		}
		return result;
	}

	@Override
	public S3Account getAccount ( ModelRequestContext mrc, String acctId ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final String modelPath = makeS3AcctPath ( acctId, true );
		try
		{
			try ( final InputStream is = fS3.getObject ( modelPath ) )
			{
				return S3BackedObject.build ( S3Account.class, fBaseContext.withPath ( new ModelObjectPath ( acctId, null, null ) ), mrc.getOperator (), is );					
			}
			catch ( ModelServiceAccessException msae )
			{
				throw msae;
			}
			catch ( IOException | BuildFailure x )	// FIXME: handle not found
			{
				throw new ModelServiceIoException ( x );
			}
		}
		catch ( AmazonClientException x )
		{
			return null;
		}
	}

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
				return new S3ModelRequestContext ( fIdentity, fCache );
			}

			private Identity fIdentity;
			private CacheControl fCache;
		};
	}

	@Override
	protected synchronized void onStopRequested ()
	{
	}

	private final ServiceContainer fServiceContainer;
	private final NvReadable fSettings;
	private final S3Interface fS3;
	private final S3ModelLoaderContext fBaseContext;

//	private static final Logger log = LoggerFactory.getLogger ( S3ModelService.class );

	private static class S3Creds implements AWSCredentials
	{
		public S3Creds ( String key, String secret )
		{
			fAccessKey = key;
			fPrivateKey = secret;
		}

		@Override
		public String getAWSAccessKeyId ()
		{
			return fAccessKey;
		}

		@Override
		public String getAWSSecretKey ()
		{
			return fPrivateKey;
		}

		private final String fAccessKey;
		private final String fPrivateKey;
	}

	private String getAccountSetupData ( String ownerId )
	{
		return
			S3ModelObject.createBasicObjectJson (
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

	public static String makeS3ModelPath ( String acctId, String model, boolean asFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( makeS3AcctPath ( acctId, true ) );
		sb.append ( model );
		if ( asFolder )
		{
			sb.append ( "/" );
		}
		return sb.toString ();
	}

	public static String removeTrailingSlash ( String in )
	{
		if ( in.endsWith ( "/" ) ) return in.substring ( 0, in.length () - 1 );
		return in;
	}
}
