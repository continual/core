/*
 *	Copyright 2019-2020, Continual.io
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

package io.continual.services.model.core.impl.zk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectFilter;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.core.impl.commonJsonDb.CommonJsonDbModel;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class ZookeeperModel extends CommonJsonDbModel
{
	public ZookeeperModel ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		try
		{
			final ExpressionEvaluator evaluator = sc.getExprEval ( config );
			final JSONObject aws = config.getJSONObject ( "aws" );

			final String zkHost = 

			fZk = new ZooKeeper ( zkHost, 2000, new Watcher () {} );
			
			fS3 = AmazonS3ClientBuilder
				.standard ()
				.withRegion ( Regions.US_WEST_2 )
				.withCredentials ( new S3Creds (
					evaluator.evaluateText ( aws.getString ( "accessKey" ) ),
					evaluator.evaluateText ( aws.getString ( "secretKey" ) )
				) )
				.build ()
			;
			fBucketId = config.getString ( "bucket" );
			fPrefix = config.optString ( "prefix", "" );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new ModelRequestContextBuilder ()
		{
			@Override
			public ModelRequestContextBuilder forUser ( Identity user )
			{
				fUser = user;
				return this;
			}

			@Override
			public ModelRequestContext build ()
			{
				return new S3ModelRequestContext ( fUser );
			}

			private Identity fUser = null;
		};
	}

	@Override
	public List<Path> listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<Path> objects = new LinkedList<> ();

		final ListObjectsV2Request req = new ListObjectsV2Request()
			.withBucketName ( fBucketId )
			.withPrefix ( pathToS3Path ( prefix ) )
		;

		ListObjectsV2Result result;
        do
        {
			result = fS3.listObjectsV2 ( req );

			for ( S3ObjectSummary objectSummary : result.getObjectSummaries () )
			{
				final String key = objectSummary.getKey ();
				objects.add ( s3KeyToPath ( key ) );
			}

			final String token = result.getNextContinuationToken ();
			req.setContinuationToken ( token );
        }
		while ( result.isTruncated () );

        return objects;
	}

	@Override
	public List<ModelObject> queryModelForObjects ( ModelRequestContext context, Path prefix, ModelObjectComparator orderBy, ModelObjectFilter ... filters )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelObject> result = new LinkedList<> ();

		final List<Path> objectPaths = listObjectsStartingWith ( context, prefix );
		for ( Path objectPath : objectPaths )
		{
			final ModelObject mo = load ( context, objectPath );
			boolean match = true;
			for ( ModelObjectFilter f : filters )
			{
				match = f.matches ( mo );
				if ( !match )
				{
					break;
				}
			}
			if ( match )
			{
				result.add ( mo );
			}
		}

		// now sort our list
		if ( orderBy != null )
		{
			Collections.sort ( result, new java.util.Comparator<ModelObject> ()
			{
				@Override
				public int compare ( ModelObject o1, ModelObject o2 )
				{
					return orderBy.compare ( o1, o2 );
				}
			} );
		}

		return result;
	}

	private Path s3KeyToPath ( String s3Key )
	{
		if ( !s3Key.startsWith ( fPrefix  ) )
		{
			throw new IllegalArgumentException ( "The key [ " + s3Key + "] is not from this bucket." );
		}
		return Path.fromString ( s3Key.substring ( fPrefix.length () ) );
	}

	private String pathToS3Path ( Path path )
	{
		return pathToS3Path ( pathToFullPath ( path ) );
	}

	private String pathToS3Path ( ModelObjectPath path )
	{
		Path p = Path.getRootPath ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			p = p.makeChildItem ( Name.fromString ( fPrefix ) );
		}
		p = p
			.makeChildItem ( Name.fromString ( path.getAcctId () ) )
			.makeChildItem ( Name.fromString ( path.getModelName () ) )
			.makeChildPath ( path.getObjectPath () )
		;
		return p.toString ().substring ( 1 );
	}
	
	@Override
	protected boolean objectExists ( ModelRequestContext context, ModelObjectPath objectPath ) throws ModelServiceRequestException
	{
		try
		{
			return fS3.doesObjectExist ( fBucketId, pathToS3Path ( objectPath ) );
		}
		catch ( SdkClientException x )
		{
			throw new ModelServiceRequestException ( x );
		}
	}

	@Override
	protected ModelObject loadObject ( ModelRequestContext context, final ModelObjectPath objectPath ) throws ModelServiceIoException, ModelServiceRequestException
	{
		try
		{
			final S3Object o = fS3.getObject ( fBucketId, pathToS3Path ( objectPath ) );

			final JSONObject rawData;
			try ( InputStream is = o.getObjectContent () )
			{
				 rawData = new JSONObject ( new CommentedJsonTokener ( is ) );
			}
			catch ( JSONException x )
			{
				throw new ModelServiceRequestException ( "The object data is corrupt." );
			}
			catch ( IOException x )
			{
				throw new ModelServiceIoException ( x );
			}

			return new ZkModelObject ( objectPath.toString (), rawData );
		}
		catch ( AmazonS3Exception x ) 
		{
			if ( x.getErrorCode ().equals ( "NoSuchKey" ) )
			{
				throw new ModelItemDoesNotExistException ( objectPath );
			}
			throw new ModelServiceRequestException ( x );
		}
		catch ( SdkClientException x )
		{
			throw new ModelServiceRequestException ( x );
		}
	}

	@Override
	protected void internalStore ( ModelRequestContext context, ModelObjectPath objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException
	{
		final JSONObject toWrite = new JSONObject ()
			.put ( kUserDataTag, new JSONObject ( new CommentedJsonTokener ( o.asJson () ) ) )
		;

		try ( final ByteArrayInputStream bais = new ByteArrayInputStream ( toWrite.toString ( 4 ).getBytes ( kUtf8 ) ) )
		{
			fS3.putObject ( fBucketId, pathToS3Path ( objectPath ), bais, new ObjectMetadata () );
		}
		catch ( JSONException x )
		{
			throw new ModelServiceRequestException ( "The object data is corrupt." );
		}
		catch ( IOException x )
		{
			throw new ModelServiceIoException ( x );
		}
	}

	private final String fBucketId;
	private final String fPrefix;

	private static final Charset kUtf8 = Charset.forName ( "UTF8" );

	private static class S3Creds implements AWSCredentialsProvider
	{
		public S3Creds ( String key, String secret )
		{
			fAccessKey = key;
			fPrivateKey = secret;
		}

		@Override
		public AWSCredentials getCredentials ()
		{
			return new AWSCredentials ()
			{
				@Override
				public String getAWSAccessKeyId () { return fAccessKey; }

				@Override
				public String getAWSSecretKey () { return fPrivateKey; }
			};
		}

		@Override
		public void refresh ()
		{
			// ignore
		}

		private final String fAccessKey;
		private final String fPrivateKey;
	}
}
