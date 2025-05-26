package io.continual.services.processor.library.aws.sources;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import io.continual.services.processor.engine.model.*;
import org.json.JSONObject;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.services.ServiceContainer;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.CommentedJsonTokener;

/**
 * Iterate through the JSON objects in a bucket at the given prefix.
 */
public class S3DbJsonDocSource implements Source
{
	public static final String kSetting_Region = "region";
	public static final String kSetting_Bucket = "bucket";
	public static final String kSetting_Prefix = "prefix";
	
	public S3DbJsonDocSource ( ServiceContainer sc, JSONObject config )
	{
		final ExpressionEvaluator ee = sc.getExprEval ( config );
		fRegion = ee.evaluateText ( config.getString ( kSetting_Region ) );
		fBucket = ee.evaluateText ( config.getString ( kSetting_Bucket ) );
		fPrefix = ee.evaluateText ( config.optString ( kSetting_Prefix, "" ) );

		fClient = AmazonS3ClientBuilder
			.standard ()
			.withCredentials ( new EnvironmentVariableCredentialsProvider () )
			.withRegion ( fRegion ).build ();

		fReq = new ListObjectsV2Request ()
			.withBucketName ( fBucket )
			.withPrefix ( fPrefix )
		;

		fPendingKeys = new LinkedList<> ();
	}

	public S3DbJsonDocSource ( String region, String bucket, String prefix )
	{
		fRegion = region;
		fBucket = bucket;
		fPrefix = prefix;

		fClient = AmazonS3ClientBuilder
			.standard ()
			.withCredentials ( new EnvironmentVariableCredentialsProvider () )
			.withRegion ( fRegion ).build ();

		fReq = new ListObjectsV2Request ()
			.withBucketName ( fBucket )
			.withPrefix ( fPrefix )
		;

		fPendingKeys = new LinkedList<> ();
	}

	@Override
	public void close ()
	{
	}

	@Override
	public boolean isEof ()
	{
		return fDone;
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, long waitAtMost, TimeUnit waitAtMostTimeUnits )
	{
		if ( fPendingKeys.size () == 0 )
		{
			loadNextSet ();
		}

		// we may be done...
		if ( fPendingKeys.size () == 0 )
		{
			fDone = true;
			return null;
		}

		// otherwise, resolve the data
		final String key = fPendingKeys.remove ();
		if ( key != null )
		{
			try ( final S3Object obj = fClient.getObject ( new GetObjectRequest ( fBucket, key ) ) )
			{
				try ( final InputStream is = obj.getObjectContent () )
				{
					final JSONObject o = new JSONObject ( new CommentedJsonTokener ( is ) );
					return new SimpleMessageAndRouting ( Message.adoptJsonAsMessage ( o ), "default" );
				}
			}
			catch ( IOException x )
			{
				spc.warn ( "IOException getting object [" + key + "]: " + x.getMessage () );
			}
		}
		// else: that's weird!

		return null;
	}

	@Override
	public void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// ignored
	}

	private final String fRegion;
	private final String fBucket;
	private final String fPrefix;
	private final AmazonS3 fClient;
	private final ListObjectsV2Request fReq;

	private final LinkedList<String> fPendingKeys;

	private boolean fDone = false;

	@Override
	public void requeue ( MessageAndRouting msgAndRoute )
	{
		throw new UnsupportedOperationException ( "Cannot requeue to an S3 DB document source." );
	}

	private void loadNextSet ()
	{
		final ListObjectsV2Result result = fClient.listObjectsV2 ( fReq );
		for ( S3ObjectSummary objectSummary : result.getObjectSummaries () )
        {
			fPendingKeys.add ( objectSummary.getKey () );
        }
		fReq.setContinuationToken ( result.getNextContinuationToken () );
	}
}
