package io.continual.flowcontrol.impl.transfer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.flowcontrol.jobapi.FlowControlJobConfig;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.StreamTools;
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
		final ExpressionEvaluator ee = new ExpressionEvaluator ( new JsonDataSource ( config ), new EnvDataSource () ); 

		fSigningKey = ee.evaluateText ( config.getString ( "signingKey" ) );
		if ( fSigningKey.length () == 0 )
		{
			throw new BuildFailure ( "Config signing key is an empty string." );
		}

		fBaseUrl = ee.evaluateText ( config.getString ( "baseUrl" ) );
		fKeyTimeLimitSec = ee.evaluateTextToLong ( config.opt ( "timeLimitSec" ), -1L );

		fConfigMap = new HashMap<> ();
		fBackgroundWork = Executors.newScheduledThreadPool ( 1 );
	}

	@Override
	public Map<String,String> deployConfiguration ( FlowControlJob job ) throws ServiceException
	{
		try
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

			// copy the config into our cache
			final FlowControlJobConfig config = job.getConfiguration ();
			putConfig ( key,
				new CachedConfig (
					config.getDataType (),
					StreamTools.readBytes ( config.readConfiguration () )
				)
			);

			result.put ( "CONFIG_KEY", key );
			result.put ( "CONFIG_URL", fBaseUrl + key );
			return result;
		}
		catch ( IOException x )
		{
			throw new ServiceException ( x );
		}
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

		// check the signature for this tag against the signature provided
		final String tagSigned = Sha256HmacSigner.sign ( tagPart, fSigningKey );
		if ( !tagSigned.equals ( sigPart ) )
		{
			log.info ( "signature doesn't match" );
			return null;
		}

		// pull out the tag parts
		final String[] idAndTimestamp = StringUtils.splitList ( tagPart, new char[] {'.'}, new char[] {} );
		if ( idAndTimestamp.length != 2 )
		{
			log.info ( "tag is malformed" );
			return null;
		}

		// check the timing
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

		// pull the config
		final FlowControlJobConfig job = readConfig ( idAndTimestamp[0] );
		if ( job == null )
		{
			log.info ( "No such job." );
			return null;
		}
		return job.readConfiguration ();
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		fBackgroundWork.scheduleAtFixedRate ( () -> { cullConfigs (); },
			kCullerPeriodSec, kCullerPeriodSec, TimeUnit.SECONDS
		);
	}

	@Override
	protected void onStopRequested ()
	{
		fBackgroundWork.shutdown ();
		try
		{
			fBackgroundWork.awaitTermination ( kShutdownTimeoutSec, TimeUnit.SECONDS );
		}
		catch ( InterruptedException e )
		{
			log.warn ( "Interrupted while awaiting termination of ConfigFetchService background executor." );
		}
	}

	private synchronized FlowControlJobConfig readConfig ( String key )
	{
		return fConfigMap.get ( key );
	}

	private synchronized void putConfig ( String key, CachedConfig cc )
	{
		fConfigMap.put ( key, cc );
	}

	private synchronized void cullConfigs ()
	{
		final long expirationMs = Clock.now () - (fKeyTimeLimitSec * 1000L); 
		
		final LinkedList<String> removals = new LinkedList<> ();
		for ( Map.Entry<String,CachedConfig> entry : fConfigMap.entrySet () )
		{
			if ( entry.getValue ().cachedTime () < expirationMs )
			{
				removals.add ( entry.getKey () );
			}
		}
		for ( String key : removals )
		{
			fConfigMap.remove ( key );
		}
	}

	private final String fSigningKey;
	private final String fBaseUrl;
	private final long fKeyTimeLimitSec;
	private final HashMap<String,CachedConfig> fConfigMap;
	private final ScheduledExecutorService fBackgroundWork;

	private static final Logger log = LoggerFactory.getLogger ( ConfigFetchService.class  );
	private static final long kCullerPeriodSec = 60;
	private static final long kShutdownTimeoutSec = 60;

	private class CachedConfig implements FlowControlJobConfig
	{
		public CachedConfig ( String dataType, byte[] configData )
		{
			fDataType = dataType;
			fConfigData = configData;
			fCachedTimeMs = Clock.now ();
		}

		@Override
		public String getDataType () { return fDataType; }

		@Override
		public InputStream readConfiguration ()
		{
			return new ByteArrayInputStream ( fConfigData );
		}

		public long cachedTime () { return fCachedTimeMs; }

		private final String fDataType;
		private final long fCachedTimeMs;
		private final byte[] fConfigData;
	}
}
