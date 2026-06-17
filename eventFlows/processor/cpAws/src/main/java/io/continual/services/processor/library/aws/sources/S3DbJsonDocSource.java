package io.continual.services.processor.library.aws.sources;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import io.continual.services.processor.engine.model.*;
import org.json.JSONObject;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

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

		fClient = S3Client.builder()
			.credentialsProvider ( EnvironmentVariableCredentialsProvider.create () )
			.region ( Region.of(fRegion) ).build ();

		fReqBuilder = ListObjectsV2Request.builder ()
			.bucket ( fBucket )
			.prefix ( fPrefix )
		;

		fPendingKeys = new LinkedList<> ();
	}

	public S3DbJsonDocSource ( String region, String bucket, String prefix )
	{
		fRegion = region;
		fBucket = bucket;
		fPrefix = prefix;

		fClient = S3Client.builder()
			.credentialsProvider ( EnvironmentVariableCredentialsProvider.create () )
			.region ( Region.of(fRegion) ).build ();

		fReqBuilder = ListObjectsV2Request.builder ()
			.bucket ( fBucket )
			.prefix ( fPrefix )
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
			try ( final ResponseInputStream<GetObjectResponse> obj = fClient.getObject ( GetObjectRequest.builder().bucket(fBucket).key(key).build() ) )
			{
				try ( final InputStream is = obj )
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
	private final S3Client fClient;
	private final ListObjectsV2Request.Builder fReqBuilder;

	private final LinkedList<String> fPendingKeys;

	private boolean fDone = false;

	@Override
	public void requeue ( MessageAndRouting msgAndRoute )
	{
		throw new UnsupportedOperationException ( "Cannot requeue to an S3 DB document source." );
	}

	private void loadNextSet ()
	{
		final ListObjectsV2Response result = fClient.listObjectsV2 ( fReqBuilder.build() );
		for ( S3Object objectSummary : result.contents () )
        {
			fPendingKeys.add ( objectSummary.key () );
        }
		fReqBuilder.continuationToken ( result.nextContinuationToken () );
	}
}
