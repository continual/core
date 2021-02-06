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

package io.continual.resources.sources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import io.continual.util.data.StreamTools;

import io.continual.resources.ResourceLoader;
import io.continual.resources.ResourceSource;

public class AwsS3BucketAndKeyLoader implements ResourceSource
{
	public AwsS3BucketAndKeyLoader ( String bucket )
	{
		fClient = null;
		fBucket = bucket;

		fCreds = new AWSCredentials ()
		{
			@Override
			public String getAWSAccessKeyId () { return System.getenv ( "AWS_ACCESS_KEY_ID" ); }

			@Override
			public String getAWSSecretKey () { return System.getenv ( "AWS_SECRET_ACCESS_KEY" ); }
		};
	}

	@Override
	public boolean qualifies ( String resourceId )
	{
		return true;
	}

	@Override
	public InputStream loadResource ( String resourceId ) throws IOException
	{
		try
		{
			if ( fClient == null )
			{
				fClient = AmazonS3ClientBuilder
					.standard ()
					.withCredentials ( new AWSStaticCredentialsProvider ( fCreds ) )
					.build ()
				;
			}

			final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			S3Object object = null;
			try
			{
				object = fClient.getObject ( new GetObjectRequest ( fBucket, resourceId ) );
				final InputStream is = object.getObjectContent ();
	
				// s3 objects must be closed or will leak an HTTP connection
				StreamTools.copyStream ( is, baos );
			}
			catch ( AmazonS3Exception x )
			{
				throw new IOException ( x ); 
			}
			finally
			{
				if ( object != null )
				{
					object.close ();
				}
			}
			return new ByteArrayInputStream ( baos.toByteArray () );
		}
		catch ( java.lang.NoClassDefFoundError x )
		{
			log.warn ( "URL [" + resourceId + "] requires the AWS S3 libraries but they're not available. " + x.getMessage (), x );
			throw new IOException ( x );
		}
	}

	private final AWSCredentials fCreds;
	private final String fBucket;
	private AmazonS3 fClient;

	private static final Logger log = LoggerFactory.getLogger ( ResourceLoader.class );
}
