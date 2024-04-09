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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsService;
import io.continual.metrics.MetricsSupplier;
import io.continual.metrics.impl.noop.NoopMeter;
import io.continual.metrics.impl.noop.NoopTimer;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonDataTransfer;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.util.collections.ShardedExpiringCache;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class S3Model extends CommonJsonDbModel implements MetricsSupplier
{
	public static void initModel ( String acctId, String modelId, String accessKey, String secretKey, Regions region, String bucketId, String prefix ) throws BuildFailure
	{
		final AmazonS3 fS3 = AmazonS3ClientBuilder
			.standard ()
			.withRegion ( region )
			.withCredentials ( new S3Creds ( accessKey, secretKey ) )
			.build ()
		;
		final String fBucketId = bucketId;
		final String fPrefix = prefix == null ? "" : prefix;

		// setup metadata using our Path tools
		Path metadataPath = Path.getRootPath ();
		if ( fPrefix.length () > 0 )
		{
			metadataPath = metadataPath.makeChildItem ( Name.fromString ( fPrefix ) );
		}
		metadataPath = metadataPath
			.makeChildItem ( Name.fromString ( acctId ) )
			.makeChildItem ( Name.fromString ( modelId  ) )
			.makeChildItem ( Name.fromString ( "metadata.json" ) )
		;
		final String metdataStr = metadataPath.toString ().substring ( 1 );

		// if the object exists, something's not right
		if ( fS3.doesObjectExist ( fBucketId, metdataStr ) )
		{
			throw new BuildFailure ( "This model is already initialized." );
		}

		// write the metadata object
		final JSONObject metadata = new JSONObject ()
			.put ( "version", Version.V2.toString () )
		;

		try (
			final ByteArrayInputStream bais = new ByteArrayInputStream ( metadata.toString ( 4 ).getBytes ( kUtf8 ) )
		)
		{
			fS3.putObject ( fBucketId, metdataStr, bais, new ObjectMetadata () );
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}
	}

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

		fCache = new ShardedExpiringCache.Builder<String,ModelDataTransfer> ()
			.named ( "object cache" )
			.build ()
		;
		fNotFoundCache = new ShardedExpiringCache.Builder<String,Boolean> ()
			.named ( "not found cache" )
			.build ()
		;

		fVersion = determineVersion ( );
		fRelnMgr = new S3SysRelnMgr ( fS3, fBucketId, getRelationsPath () );

		fFoldersAsObjects = false;
	}

	public S3Model ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		try
		{
			final ExpressionEvaluator evaluator = sc.getExprEval ( config );
			final JSONObject evaledConfig = evaluator.evaluateJsonObject ( config );

			final String accessKey = evaledConfig.getString ( "accessKey" );
			final String secretKey = evaledConfig.getString ( "secretKey" );
			final Regions region = Regions.fromName ( evaledConfig.optString ( "region", Regions.US_WEST_2.getName () ) );

			fS3 = AmazonS3ClientBuilder
				.standard ()
				.withRegion ( region )
				.withCredentials ( new S3Creds ( accessKey, secretKey ) )
				.build ()
			;
			fBucketId = evaledConfig.getString ( "bucket" );
			fPrefix = evaledConfig.optString ( "prefix", "" );

			fCache = new ShardedExpiringCache.Builder<String,ModelDataTransfer> ()
				.named ( "object cache" )
				.build ()
			;
			fNotFoundCache = new ShardedExpiringCache.Builder<String,Boolean> ()
				.named ( "not found cache" )
				.build ()
			;

			Version vv = determineVersion ();
			if ( config.optBoolean ( "initOk", false ) && vv == Version.V1_IMPLIED )
			{
				initModel ( super.getAcctId (), super.getId (), accessKey, secretKey, region, fBucketId, fPrefix );
				vv = Version.V2;
			}
			fVersion = vv;

			fRelnMgr = new S3SysRelnMgr ( fS3, fBucketId, getRelationsPath () );

			fFoldersAsObjects = evaledConfig.optBoolean ( "foldersAsObjects", false );

			// optionally report metrics
			final MetricsService ms = sc.get ( "metrics", MetricsService.class );
			if ( ms != null )
			{
				populateMetrics ( ms.getCatalog ( "S3Model " + config.optString ( "name", "anonymous" ) ) );
			}
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void populateMetrics ( MetricsCatalog metrics )
	{
		fCacheHitCounter = metrics.meter ( "cacheHits" );
		fCacheMissCounter = metrics.meter ( "cacheMisses" );
		fReadTimer = metrics.timer ( "readTimer" );
		fS3ReadTimer = metrics.timer ( "s3ReadTimer" );
		fWriteTimer = metrics.timer ( "writeTimer" );
		fRemoveTimer = metrics.timer ( "removeTimer" );
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
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<Path> pending = new LinkedList<> ();
		final TreeSet<Path> seen = new TreeSet<> ();

		final ListObjectsV2Request req = new ListObjectsV2Request ()
			.withBucketName ( fBucketId )
			.withPrefix ( pathToS3Path ( prefix ) + "/" )
			.withDelimiter ( Path.getPathSeparatorString () )
		;

		final AtomicBoolean lastResultTruncated = new AtomicBoolean ( true );

		final Iterator<Path> iter = new Iterator<Path> ()
		{
			@Override
			public boolean hasNext ()
			{
				if ( pending.size () > 0 ) return true;

				// if we're out, maybe load the next set
				if ( lastResultTruncated.get () )
				{
					final ListObjectsV2Result result = fS3.listObjectsV2 ( req );
					for ( S3ObjectSummary objectSummary : result.getObjectSummaries () )
					{
						final String key = objectSummary.getKey ();
						final Path asPath = s3KeyToPath ( key );
						if ( !asPath.equals ( prefix ) && !seen.contains ( asPath ) )
						{
							pending.add ( asPath );
							seen.add ( asPath );
						}
					}
					for ( String commonPrefix : result.getCommonPrefixes () )
					{
						final Path asPath = s3KeyToPath ( commonPrefix );
						if ( !asPath.equals ( prefix ) && !seen.contains ( asPath ) )
						{
							pending.add ( asPath );
							seen.add ( asPath );
						}
					}
					final String token = result.getNextContinuationToken ();
					req.setContinuationToken ( token );
	
					lastResultTruncated.set ( result.isTruncated () );
				}
				return pending.size () > 0;
			}

			@Override
			public Path next ()
			{
				return pending.removeFirst ();
			}
		};

		return new ModelPathList ()
		{
			@Override
			public Iterator<Path> iterator () { return iter; }
		};
	}

	private class S3ModelQuery extends SimpleModelQuery
	{
		@Override
		public <T,K> ModelObjectList<T> execute ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException
		{
			Comparator<ModelObject> orderBy = getOrdering ();
			if ( orderBy != null )
			{
				return fullLoad ( context, factory, accessor, userContext );
			}
			else
			{
				return streamLoad ( context, factory, accessor, userContext );
			}
		}

		private <T,K> ModelObjectList<T> fullLoad ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObjectAndPath<T>> result = new LinkedList<> ();

			final ModelPathList objectPaths = listChildrenOfPath ( context, getPathPrefix () );
			for ( Path objectPath : objectPaths )
			{
				final T mo = load ( context, objectPath, factory, userContext );
				boolean match = true;
				for ( Filter f : getFilters() )
				{
					match = f.matches ( accessor.getDataFrom ( mo ) );
					if ( !match )
					{
						break;
					}
				}
				if ( match )
				{
					result.add ( ModelObjectAndPath.from ( objectPath, mo ) );
				}
			}

			// now sort our list
			Comparator<ModelObject> orderBy = getOrdering ();
			if ( orderBy != null )
			{
				Collections.sort ( result, new java.util.Comparator<ModelObjectAndPath<T>> ()
				{
					@Override
					public int compare ( ModelObjectAndPath<T> o1, ModelObjectAndPath<T> o2 )
					{
						return orderBy.compare (
							accessor.getDataFrom ( o1.getObject () ),
							accessor.getDataFrom ( o2.getObject () )
						);
					}
				} );
			}

			return new ModelObjectList<T> ()
			{
				@Override
				public Iterator<ModelObjectAndPath<T>> iterator ()
				{
					return result.iterator ();
				}
			};
		}

		private <T,K> ModelObjectList<T> streamLoad ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException
		{
			final ModelPathList objectPaths = listChildrenOfPath ( context, getPathPrefix () );
			final Iterator<Path> paths = objectPaths.iterator ();

			final LinkedList<ModelObjectAndPath<T>> pending = new LinkedList<> ();

			return new ModelObjectList<T> ()
			{
				@Override
				public Iterator<ModelObjectAndPath<T>> iterator ()
				{
					return new Iterator<ModelObjectAndPath<T>> ()
					{
						@Override
						public boolean hasNext ()
						{
							if ( pending.size () > 0 ) return true;

							// otherwise, we're empty... are there more available from the path list?
							if ( !paths.hasNext () ) return false;

							// paths has more
							while ( pending.size () == 0 )
							{
								final Path p = paths.next ();
								try
								{
									final T mo = load ( context, p, factory, userContext );
									boolean match = true;
									for ( Filter f : getFilters() )
									{
										match = f.matches ( accessor.getDataFrom ( mo ) );
										if ( !match )
										{
											break;
										}
									}
									if ( match )
									{
										pending.add ( ModelObjectAndPath.from ( p, mo ) );
									}
								}
								catch ( ModelServiceException | ModelRequestException x )
								{
									log.warn ( "Exception retrieving next object: " + x.getMessage () );
									return false;
								}
							}
							return pending.size () > 0;
						}

						@Override
						public ModelObjectAndPath<T> next ()
						{
							return pending.removeFirst ();
						}
					};
				}
			};
		}
	}
	
	@Override
	public ModelQuery startQuery ()
	{
		return new S3ModelQuery ();
	}

	private static final String kObjects = "objects";
	private static final String kRelationships = "relationships";

	private Path s3KeyToPath ( String s3Key )
	{
		if ( !s3Key.startsWith ( fPrefix  ) )
		{
			throw new IllegalArgumentException ( "The key [ " + s3Key + "] is not from this bucket." );
		}

		String cleanedUp = s3Key.substring ( fPrefix.length () );
		if ( cleanedUp.endsWith ( Path.getPathSeparatorString () ) )
		{
			cleanedUp = cleanedUp.substring ( 0, cleanedUp.length () - 1 );
		}

		if ( !cleanedUp.startsWith ( Path.getPathSeparatorString () ) )
		{
			cleanedUp = Path.getPathSeparatorString () + cleanedUp;
		}
		
		Path asPath = Path.fromString ( cleanedUp );

		asPath = asPath.makePathWithinParent ( Path.fromString ( "/" + getAcctId() ) );
		asPath = asPath.makePathWithinParent ( Path.fromString ( "/" + getId() ) );

		if ( Version.isV2OrLater ( fVersion ) )
		{
			asPath = asPath.makePathWithinParent ( Path.fromString ( "/" + kObjects ) );
		}

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
		;

		if ( Version.isV2OrLater ( fVersion ) )
		{
			p = p.makeChildItem ( Name.fromString ( kObjects ) );
		}

		p = p.makeChildPath ( path );

		return p.toString ().substring ( 1 );
	}

	private Path getRelationsPath ()
	{
		Path p = Path.getRootPath ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			p = p.makeChildItem ( Name.fromString ( fPrefix ) );
		}

		return p
			.makeChildItem ( Name.fromString ( getAcctId () ) )
			.makeChildItem ( Name.fromString ( getId () ) )
			.makeChildItem ( Name.fromString ( kRelationships ) )
		;
	}

	private String metadataS3Path ()
	{
		Path p = Path.getRootPath ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			p = p.makeChildItem ( Name.fromString ( fPrefix ) );
		}

		p = p
			.makeChildItem ( Name.fromString ( getAcctId () ) )
			.makeChildItem ( Name.fromString ( getId () ) )
		;

		return p.toString ().substring ( 1 ) + "/metadata.json";
	}

	@Override
	protected boolean objectExists ( ModelRequestContext context, Path objectPath ) throws ModelRequestException
	{
		try
		{
			final String s3Path = pathToS3Path ( objectPath );
			final ModelDataTransfer mo = fCache.read ( s3Path );
			if ( mo != null ) return true;

			final Boolean notFound = fNotFoundCache.read ( s3Path );
			if ( notFound != null ) return false;

			final boolean asObj = fS3.doesObjectExist ( fBucketId, s3Path );
			if ( asObj ) return true;

			if ( fFoldersAsObjects )
			{
				final ObjectListing ol = fS3.listObjects ( fBucketId, s3Path + "/" );
				return ( ol.getObjectSummaries ().size () > 0 );
			}
			
			return false;
		}
		catch ( SdkClientException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	@Override
	protected ModelDataTransfer loadObject ( ModelRequestContext context, final Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		final String s3Path = pathToS3Path ( objectPath );

		try ( Timer.Context timingContext = fReadTimer.time () )
		{
			ModelDataTransfer result = fCache.read ( s3Path );
			if ( result != null )
			{
				fCacheHitCounter.mark ();
				return result;
			}

			// is it known not-found?
			final Boolean notFound = fNotFoundCache.read ( s3Path );
			if ( notFound != null ) return null;
			
			// otherwise, load from S3

			fCacheMissCounter.mark ();

			try ( Timer.Context s3TimingContext = fS3ReadTimer.time () )
			{
				final S3Object o = fS3.getObject ( fBucketId, s3Path );
	
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

				result = new CommonDataTransfer ( objectPath, rawData );
				fCache.write ( s3Path, result );

				return result;
			}
			finally
			{
				// 
			}
		}
		catch ( AmazonS3Exception x ) 
		{
			if ( x.getErrorCode ().equals ( "NoSuchKey" ) )
			{
				if ( fFoldersAsObjects )
				{
					final ObjectListing ol = fS3.listObjects ( fBucketId, s3Path + "/" );
					if ( ol.getObjectSummaries ().size () > 0 )
					{
						// it's a "folder"; return an empty object
						final CommonDataTransfer result = new CommonDataTransfer ( objectPath, new JSONObject () );
						fCache.write ( s3Path, result );
						return result;
					}
				}

				// else: we're not reporting folders or we are but this isn't one
				fNotFoundCache.write ( s3Path, true );
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
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelDataTransfer o ) throws ModelRequestException, ModelServiceException
	{
		try (
			final Timer.Context timingContext = fWriteTimer.time ();
			final ByteArrayInputStream bais = new ByteArrayInputStream ( CommonDataTransfer.toDataObject ( o ).toString ( 4 ).getBytes ( kUtf8 ) )
		)
		{
			final String s3Path = pathToS3Path ( objectPath );
			fS3.putObject ( fBucketId, s3Path, bais, new ObjectMetadata () );
			fCache.write ( s3Path, o );
			fNotFoundCache.remove ( s3Path );
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
		try ( final Timer.Context timingContext = fRemoveTimer.time () )
		{
			final boolean existed = exists ( context, objectPath );
			final String s3Path = pathToS3Path ( objectPath );
			fS3.deleteObject ( fBucketId, s3Path );
			fCache.remove ( s3Path );
			fNotFoundCache.write ( s3Path, true );
			return existed;
		}
	}

	private final AmazonS3 fS3;
	private final String fBucketId;
	private final String fPrefix;
	private final S3SysRelnMgr fRelnMgr;
	private final boolean fFoldersAsObjects;

	private final ShardedExpiringCache<String,ModelDataTransfer> fCache;
	private final ShardedExpiringCache<String,Boolean> fNotFoundCache;	// because null means not-found :-(

	private enum Version
	{
		V1_IMPLIED,
		V1,
		V2;

		public static Version fromText ( String s )
		{
			if ( s == null ) return V1_IMPLIED;
			try
			{
				return Version.valueOf ( s.toUpperCase ().trim () );
			}
			catch ( IllegalArgumentException x )
			{
				// ignore
			}
			return V1_IMPLIED;
		}

		public static boolean isV2OrLater ( Version v )
		{
			return v != V1 && v != V1_IMPLIED;
		}
	};
	private final Version fVersion;
	
	private Meter fCacheHitCounter = new NoopMeter ();
	private Meter fCacheMissCounter = new NoopMeter ();
	private Timer fReadTimer = new NoopTimer ();
	private Timer fS3ReadTimer = new NoopTimer ();
	private Timer fWriteTimer = new NoopTimer ();
	private Timer fRemoveTimer = new NoopTimer ();

	static final Charset kUtf8 = Charset.forName ( "UTF8" );

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

	// we store a JSON object in the bucket/prefix folder with model metadata.
	private JSONObject readModelMetadata () throws BuildFailure
	{
		final String loc = metadataS3Path ();

		try ( Timer.Context s3TimingContext = fS3ReadTimer.time () )
		{
			final S3Object o = fS3.getObject ( fBucketId, loc );

			final JSONObject rawData;
			try ( InputStream is = o.getObjectContent () )
			{
				rawData = new JSONObject ( new CommentedJsonTokener ( is ) );
			}
			catch ( JSONException x )
			{
				throw new BuildFailure ( "The model metadata is corrupt." );
			}
			catch ( IOException x )
			{
				throw new BuildFailure ( x );
			}

			return rawData;
		}
		catch ( AmazonS3Exception x )
		{
			if ( x.getErrorCode ().equals ( "NoSuchKey" ) )
			{
				return new JSONObject ();
			}
			else
			{
				throw new BuildFailure ( x );
			}
		}
		catch ( SdkClientException x )
		{
			throw new BuildFailure ( x );
		}
	}
	
	private Version determineVersion () throws BuildFailure
	{
		final JSONObject metadata = readModelMetadata ();
		return Version.fromText ( metadata.optString ( "version", Version.V1_IMPLIED.toString () ) );
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		if ( !Version.isV2OrLater ( fVersion ) ) throw new ModelServiceException ( "not implemented" );

		checkReadOnly ();

		fRelnMgr.setRelationType ( context, relnName, rt );

		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		if ( !Version.isV2OrLater ( fVersion ) ) throw new ModelServiceException ( "not implemented" );

		checkReadOnly ();

		fRelnMgr.relate ( mr );

		return ModelRelationInstance.from ( mr );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		if ( !Version.isV2OrLater ( fVersion ) ) throw new ModelServiceException ( "not implemented" );

		checkReadOnly ();

		final boolean exists = fRelnMgr.doesRelationExist ( reln );
		fRelnMgr.unrelate ( reln );
		return exists;
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		try
		{
			checkReadOnly ();

			final ModelRelationInstance mr = ModelRelationInstance.from ( relnId );
			return unrelate ( context, mr );
		}
		catch ( IllegalArgumentException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	@Override
	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( !Version.isV2OrLater ( fVersion ) )
		{
			// these models didn't support relations
			return new LinkedList<> ();
		}
		return fRelnMgr.getInboundRelationsNamed ( forObject, named );
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( !Version.isV2OrLater ( fVersion ) )
		{
			// these models didn't support relations
			return new LinkedList<> ();
		}
		return fRelnMgr.getOutboundRelationsNamed ( forObject, named );
	}

	private static final Logger log = LoggerFactory.getLogger ( S3Model.class );
}
