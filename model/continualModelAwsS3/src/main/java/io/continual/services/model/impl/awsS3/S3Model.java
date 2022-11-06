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

package io.continual.services.model.impl.awsS3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.services.model.impl.json.CommonJsonDbObject;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class S3Model extends CommonJsonDbModel
{
	public S3Model ( String acctId, String modelId, String accessKey, String secretKey, String bucketId, String prefix ) throws BuildFailure
	{
		super ( acctId, modelId );

		fS3 = AmazonS3ClientBuilder
			.standard ()
			.withRegion ( Regions.DEFAULT_REGION )
			.withCredentials ( new S3Creds ( accessKey, secretKey ) )
			.build ()
		;
		fBucketId = bucketId;
		fPrefix = prefix == null ? "" : prefix;
	}

	public S3Model ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		try
		{
			final ExpressionEvaluator evaluator = sc.getExprEval ( config );
			final JSONObject evaledConfig = evaluator.evaluateJsonObject ( config );

			fS3 = AmazonS3ClientBuilder
				.standard ()
				.withRegion ( Regions.fromName ( evaledConfig.optString ( "region", Regions.US_WEST_2.getName () ) ) )
				.withCredentials ( new S3Creds (
					evaledConfig.getString ( "accessKey" ),
					evaledConfig.getString ( "secretKey" )
				) )
				.build ()
			;
			fBucketId = evaledConfig.getString ( "bucket" );
			fPrefix = evaledConfig.optString ( "prefix", "" );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public long getMaxSerializedObjectLength ()
	{
		// S3 has various limits; for now we'll use the allowed in a PUT, 5 GB
		return 5L * 1024L * 1024L * 1024L;
	}

	@Override
	public long getMaxPathLength ()
	{
		return 1024L;
	}

	@Override
	public long getMaxRelnNameLength ()
	{
		// this is more arbitrary depending on how we store it
		return 1024L;
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ( );
	}

	@Override
	public ModelPathList listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
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

        return new ModelPathList ()
        {
			@Override
			public Iterator<Path> iterator ()
			{
				return objects.iterator ();
			}
		};
	}

	private class S3ModelQuery extends SimpleModelQuery
	{
		@Override
		public ModelObjectList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObject> result = new LinkedList<> ();

			final ModelPathList objectPaths = listObjectsStartingWith ( context, getPathPrefix () );
			for ( Path objectPath : objectPaths )
			{
				final ModelObject mo = load ( context, objectPath );
				boolean match = true;
				for ( Filter f : getFilters() )
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
			ModelObjectComparator orderBy = getOrdering ();
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

			return new ModelObjectList ()
			{
				@Override
				public Iterator<ModelObject> iterator ()
				{
					return result.iterator ();
				}
			};
		}
	}
	
	@Override
	public ModelQuery startQuery ()
	{
		return new S3ModelQuery ();
	}

	private Path s3KeyToPath ( String s3Key )
	{
		if ( !s3Key.startsWith ( fPrefix  ) )
		{
			throw new IllegalArgumentException ( "The key [ " + s3Key + "] is not from this bucket." );
		}

		final String withoutPrefix = s3Key.substring ( fPrefix.length () );

		Path asPath = Path.fromString ( withoutPrefix );
		asPath = asPath.makePathWithinParent ( Path.fromString ( "/" + getAcctId() ) );
		asPath = asPath.makePathWithinParent ( Path.fromString ( "/" + getId() ) );
		return asPath;
	}

	private String pathToS3Path ( Path path )
	{
		Path p = Path.getRootPath ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			p = p.makeChildItem ( Name.fromString ( fPrefix ) );
		}
		p = p
			.makeChildItem ( Name.fromString ( getAcctId () ) )
			.makeChildItem ( Name.fromString ( getId () ) )
			.makeChildPath ( path )
		;
		return p.toString ().substring ( 1 );
	}
	
	@Override
	protected boolean objectExists ( ModelRequestContext context, Path objectPath ) throws ModelRequestException
	{
		try
		{
			return fS3.doesObjectExist ( fBucketId, pathToS3Path ( objectPath ) );
		}
		catch ( SdkClientException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	@Override
	protected ModelObject loadObject ( ModelRequestContext context, final Path objectPath ) throws ModelServiceException, ModelRequestException
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
				throw new ModelRequestException ( "The object data is corrupt." );
			}
			catch ( IOException x )
			{
				throw new ModelServiceException ( x );
			}

			return new CommonJsonDbObject ( objectPath.toString (), rawData );
		}
		catch ( AmazonS3Exception x ) 
		{
			if ( x.getErrorCode ().equals ( "NoSuchKey" ) )
			{
				throw new ModelItemDoesNotExistException ( objectPath );
			}
			throw new ModelRequestException ( x );
		}
		catch ( SdkClientException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	@Override
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelRequestException, ModelServiceException
	{
		try ( final ByteArrayInputStream bais = new ByteArrayInputStream ( o.toJson ().toString ( 4 ).getBytes ( kUtf8 ) ) )
		{
			fS3.putObject ( fBucketId, pathToS3Path ( objectPath ), bais, new ObjectMetadata () );
		}
		catch ( JSONException x )
		{
			throw new ModelRequestException ( "The object data is corrupt." );
		}
		catch ( IOException x )
		{
			throw new ModelServiceException ( x );
		}
	}

	@Override
	protected boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		final boolean existed = exists ( context, objectPath );
		fS3.deleteObject ( fBucketId, pathToS3Path ( objectPath ) );
		return existed;
	}

	private final AmazonS3 fS3;
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

	@Override
	public void relate ( ModelRequestContext context, ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln )
		throws ModelServiceException,
			ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}

	@Override
	public List<ModelRelation> getInboundRelations (
		ModelRequestContext context, Path forObject )
		throws ModelServiceException,
			ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}

	@Override
	public List<ModelRelation> getOutboundRelations (
		ModelRequestContext context, Path forObject )
		throws ModelServiceException,
			ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed (
		ModelRequestContext context, Path forObject, String named )
		throws ModelServiceException,
			ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}

	@Override
	public List<ModelRelation> getOutboundRelationsNamed (
		ModelRequestContext context, Path forObject, String named )
		throws ModelServiceException,
			ModelRequestException
	{
		throw new ModelServiceException ( "not implemented" );
	}
}
