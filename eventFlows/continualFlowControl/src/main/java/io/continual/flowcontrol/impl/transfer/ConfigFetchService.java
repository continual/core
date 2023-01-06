package io.continual.flowcontrol.impl.transfer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.flowcontrol.jobapi.FlowControlJobConfig;
import io.continual.flowcontrol.jobapi.FlowControlJobDb;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.StringUtils;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.JsonDataSource;
import io.continual.util.time.Clock;

public class ConfigFetchService extends SimpleService implements ConfigTransferService
{
	public ConfigFetchService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fJobDb = sc.get ( config.optString ( "jobDb", "jobDb" ), FlowControlJobDb.class );
		if ( fJobDb == null ) throw new BuildFailure ( "No job database found." );

		final ExpressionEvaluator ee = new ExpressionEvaluator ( new JsonDataSource ( config ), new EnvDataSource () ); 

		fSigningKey = ee.evaluateText ( config.getString ( "signingKey" ) );
		if ( fSigningKey.length () == 0 )
		{
			throw new BuildFailure ( "Config signing key is an empty string." );
		}

		fBaseUrl = ee.evaluateText ( config.getString ( "baseUrl" ) );
		fKeyTimeLimitSec = ee.evaluateTextToLong ( config.opt ( "timeLimitSec" ), -1L );
	}

	@Override
	public Map<String,String> deployConfiguration ( FlowControlJob job )
	{
		final HashMap<String,String> result = new HashMap<>();
		
		final String jobId = job.getId ();
		final long createdAtMs = Clock.now ();

		final StringBuilder in = new StringBuilder ();
		in
			.append ( jobId )
			.append ( "." )
			.append ( createdAtMs )
		;
		final String tag = in.toString ();
		
		final String tagEnc = TypeConvertor.base64UrlEncode ( tag );
		final String tagSigned = TypeConvertor.base64UrlEncode ( Sha256HmacSigner.sign ( tag, fSigningKey ) );

		final String key = tagEnc + "-" + tagSigned;

		log.info ( "job [" + jobId + "] => [" + key + "]" );

		result.put ( "CONFIG_KEY", key );
		result.put ( "CONFIG_URL", fBaseUrl + key );
		return result;
	}

	@Override
	public InputStream fetch ( String byKey )
	{
		final String[] parts = StringUtils.splitList ( byKey, new char[] {'-'}, new char[] {} );
		if ( parts.length != 2 )
		{
			log.info ( "bad key format" );
			return null;
		}

		final String tagPart = new String ( TypeConvertor.base64UrlDecode ( parts[0] ), StandardCharsets.UTF_8 );
		final String sigPart = new String ( TypeConvertor.base64UrlDecode ( parts[1] ), StandardCharsets.UTF_8 ); 

		final String tagSigned = Sha256HmacSigner.sign ( tagPart, fSigningKey );
		if ( !tagSigned.equals ( sigPart ) )
		{
			log.info ( "signature doesn't match" );
			return null;
		}

		final String[] idAndTimestamp = StringUtils.splitList ( tagPart, new char[] {'.'}, new char[] {} );
		if ( idAndTimestamp.length != 2 )
		{
			log.info ( "tag is malformed" );
			return null;
		}
		try
		{
			final long ts = Long.parseLong ( idAndTimestamp[1] );
			if ( fKeyTimeLimitSec > 0 && ts + fKeyTimeLimitSec < Clock.now () )
			{
				// expired
				log.info ( "tag is expired" );
				return null;
			}
		}
		catch ( NumberFormatException x )
		{
			log.info ( "tag timestamp is not a number" );
			return null;
		}
	
		try
		{
			final FlowControlJob job = fJobDb.getJobAsAdmin ( idAndTimestamp[0] );
			if ( job == null )
			{
				log.info ( "No such job." );
				return null;
			}
			final FlowControlJobConfig config = job.getConfiguration ();
			return config.readConfiguration ();
		}
		catch ( io.continual.flowcontrol.jobapi.FlowControlJobDb.ServiceException e )
		{
			log.info ( "Error loading job: " + e.getMessage () );
			return null;
		}
	}

	private final FlowControlJobDb fJobDb;
	private final String fSigningKey;
	private final String fBaseUrl;
	private final long fKeyTimeLimitSec;

	private static final Logger log = LoggerFactory.getLogger ( ConfigFetchService.class  );
}
