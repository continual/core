package io.continual.flowcontrol.impl.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Map;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.Encryptor;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlJobConfig;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.model.FlowControlJobBuilder;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.util.data.StreamTools;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.standards.MimeTypes;
import io.continual.util.time.Clock;

/**
 * A job builder backed by a JSON data structure.
 */
public class JsonJobBuilder implements FlowControlJobBuilder
{
	public JsonJobBuilder ( FlowControlCallContext fccc, Encryptor enc )
	{
		fEnc = enc;
		
		fBuildingData = new JSONObject ();

		withOwner ( fccc.getUser ().getId () );
		withAccess ( AccessControlEntry.kOwner, AccessControlList.READ, AccessControlList.UPDATE, AccessControlList.DELETE );
	}

	@Override
	public JsonJobBuilder withId ( String id )
	{
		fBuildingData.put ( JsonJob.kId, id );
		return this;
	}
	
	@Override
	public JsonJobBuilder withName ( String name )
	{
		fBuildingData.put ( JsonJob.kDisplayName, name );
		return this;
	}

	@Override
	public JsonJobBuilder withOwner ( String owner )
	{
		fBuildingData.put ( JsonJob.kAcl,
			AccessControlList.deserialize ( fBuildingData.optJSONObject ( JsonJob.kAcl ) )
				.setOwner ( owner )
				.asJson ()
		);
		return this;
	}

	@Override
	public JsonJobBuilder withAccess ( String user, String... ops )
	{
		fBuildingData.put ( JsonJob.kAcl,
			AccessControlList.deserialize ( fBuildingData.optJSONObject ( JsonJob.kAcl ) )
				.permit ( user, ops )
				.asJson ()
		);
		return this;
	}

	@Override
	public JsonJobBuilder clone ( FlowControlJob existingJob ) throws GeneralSecurityException, IOException
	{
		// short cut if we're working with our own type
		if ( existingJob instanceof JsonJob )
		{
			fBuildingData = JsonUtil.clone ( ((JsonJob)existingJob).fData );
			return this;
		}

		// otherwise...
		fBuildingData = new JSONObject ();

		fBuildingData
			.put ( JsonJob.kDisplayName, existingJob.getName () )
			.put ( JsonJob.kAcl, existingJob.getAccessControlList ().asJson () )
			.put ( JsonJob.kCreateTime, existingJob.getCreateTimestampMs () )
			.put ( JsonJob.kUpdateTime, existingJob.getUpdateTimestampMs () )
		;

		// configuration
		setConfiguration ( existingJob.getConfiguration () );

		// runtime spec
		setRuntimeSpec ( existingJob.getRuntimeSpec () );

		// secrets
		for ( Map.Entry<String,String> e : existingJob.getSecrets ( fEnc ).entrySet () )
		{
			registerSecret ( e.getKey (), e.getValue () );
		}

		return this;
	}

	@Override
	public JsonJobBuilder setConfiguration ( FlowControlJobConfig config ) throws IOException
	{
		final JSONObject configCopy = new JSONObject ();

		final String type = config.getDataType ();

		// we know about certain types specifically....
		if ( type.equalsIgnoreCase ( MimeTypes.kAppJson ) || type.equalsIgnoreCase ( MimeTypes.kPlainText ) )
		{
			// these are simple UTF-8 character streams; read them and write to a plain text value in our new job
			final byte[] bytes = StreamTools.readBytes ( config.readConfiguration () );
			configCopy
				.put ( JsonJob.kConfigType, type )
				.put ( JsonJob.kConfigData, new String ( bytes, StandardCharsets.UTF_8 ) )
			;
		}
		else if ( type.equalsIgnoreCase ( MimeTypes.kAppGenericBinary ) )
		{
			final byte[] bytes = StreamTools.readBytes ( config.readConfiguration () );
			configCopy
				.put ( JsonJob.kConfigType, MimeTypes.kAppGenericBinary )
				.put ( JsonJob.kConfigData, TypeConvertor.base64Encode ( bytes ) )
			;
		}
		else
		{
			throw new IllegalArgumentException ( "Unrecognized config type: " + type );
		}

		fBuildingData.put ( JsonJob.kConfig, configCopy );
		timestampNow ();

		return this;
	}

	@Override
	public JsonJobBuilder setRuntimeSpec ( FlowControlRuntimeSpec runtimeSpec )
	{
		fBuildingData.put ( JsonJob.kRuntime, new JSONObject ()
			.put ( JsonJob.kName, runtimeSpec.getName () )
			.put ( JsonJob.kVersion, runtimeSpec.getVersion () )
		);
		timestampNow ();
		return this;
	}

	@Override
	public JsonJobBuilder registerSecret ( String key, String value ) throws GeneralSecurityException
	{
		JSONObject secrets = fBuildingData.optJSONObject ( JsonJob.kSecrets );
		if ( secrets == null )
		{
			secrets = new JSONObject ();
			fBuildingData.put ( JsonJob.kSecrets, secrets );
		}
		secrets.put ( key, fEnc.encrypt ( value ) );
		timestampNow ();

		return this;
	}

	@Override
	public JsonJobBuilder removeSecretRef ( String key )
	{
		JSONObject secrets = fBuildingData.optJSONObject ( JsonJob.kSecrets );
		if ( secrets != null )
		{
			secrets.remove ( key );
		}
		timestampNow ();

		return this;
	}

	@Override
	public JsonJob build () throws BuildFailure
	{
		return new JsonJob ( getBuildingData () );
	}

	private final Encryptor fEnc;
	
	private JSONObject fBuildingData;

	protected JSONObject getBuildingData ()
	{
		return fBuildingData;
	}

	private void timestampNow ()
	{
		final long now = Clock.now ();

		fBuildingData.put ( JsonJob.kUpdateTime, now );
		if ( !fBuildingData.has ( JsonJob.kCreateTime ) )
		{
			fBuildingData.put ( JsonJob.kCreateTime, now );
		}
	}
}
