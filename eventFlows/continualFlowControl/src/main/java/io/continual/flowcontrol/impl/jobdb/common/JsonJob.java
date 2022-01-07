package io.continual.flowcontrol.impl.jobdb.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.flowcontrol.controlapi.FlowControlRuntimeSpec;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.flowcontrol.jobapi.FlowControlJobConfig;
import io.continual.flowcontrol.jobapi.FlowControlJobDb;
import io.continual.flowcontrol.jobapi.FlowControlJobDb.ServiceException;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.util.data.StreamTools;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class JsonJob implements FlowControlJob, JsonSerialized
{
	private static final String kName = "name";
	private static final String kVersion = "version";
	private static final String kAcl = "acl";
	private static final String kRuntime = "runtime";
	private static final String kConfig = "config";
	private static final String kSecrets = "secrets";
	private static final String kSecretValue = "value";

	public interface Encryptor
	{
		String encrypt ( String val ) throws GeneralSecurityException;
		String decrypt ( String val ) throws GeneralSecurityException;
	}

	public JsonJob ( String name, Encryptor enc )
	{
		fData = new JSONObject ()
			.put ( kName, name )
			.put ( kAcl, AccessControlList.initialize ( null ).asJson () )
			.put ( kRuntime, new JSONObject ()
				.put ( "name", "current" )	// FIXME: that doesn't make sense
				.put ( "version", "current" )
			)
			.put ( kConfig, buildEmptyConfig () )
			.put ( kSecrets, new JSONObject () )
		;
		fEncryptor = enc;
	}

	public JsonJob ( String name, Encryptor enc, JSONObject persisted )
	{
		this ( name, enc );
		
		// copy persisted data into our std starting template
		JsonUtil.copyInto ( persisted, fData );
		
		// but make sure the name didn't get overwritten by rogue data
		fData.put ( kName, name );
	}

	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fData );
	}

	@Override
	public AccessControlList getAccessControlList ()
	{
		return AccessControlList.deserialize ( fData.getJSONObject ( kAcl ), new AclUpdateListener ()
		{
			@Override
			public void onAclUpdate ( AccessControlList accessControlList )
			{
				fData.put ( kAcl, accessControlList.asJson () );
			}
		} );
	}

	@Override
	public String getId ()
	{
		return getName();
	}

	@Override
	public String getName ()
	{
		return fData.getString ( kName );
	}

	@Override
	public FlowControlJobConfig getConfiguration ()
	{
		final JSONObject config = fData.getJSONObject ( kConfig );

		return new FlowControlJobConfig ()
		{
			@Override
			public String getDataType ()
			{
				return config.getString ( "type" );
			}

			@Override
			public InputStream readConfiguration ()
			{
				final String type = getDataType ();
				
				// we know about certain types specifically....
				if ( type.equalsIgnoreCase ( MimeTypes.kAppJson  ) )
				{
					final JSONObject data = config.getJSONObject ( "data" );
					return new ByteArrayInputStream ( data.toString ().getBytes ( StandardCharsets.UTF_8 ) );
				}
				else if ( type.equalsIgnoreCase ( MimeTypes.kPlainText  ) )
				{
					final String data = config.getString ( "data" );
					return new ByteArrayInputStream ( data.getBytes ( StandardCharsets.UTF_8 ) );
				}
				else if ( type.equalsIgnoreCase ( MimeTypes.kAppGenericBinary ) )
				{
					final String data = config.getString ( "data" );
					return new ByteArrayInputStream ( TypeConvertor.base64Decode ( data ) );
				}
				else
				{
					throw new IllegalArgumentException ( "Unrecognized config type: " + type );
				}
			}
		};
	}

	@Override
	public FlowControlJob setConfiguration ( FlowControlJobConfig config ) throws IOException
	{
		final JSONObject configData = fData.getJSONObject ( kConfig );

		try ( final InputStream is = config.readConfiguration () )
		{
			final String type = config.getDataType ();
			configData.put ( "type", type );
	
			if ( type.equalsIgnoreCase ( MimeTypes.kAppJson  ) )
			{
				final JSONObject data = new JSONObject ( new CommentedJsonTokener ( is ) );
				configData.put ( "data", data );
			}
			else if ( type.equalsIgnoreCase ( MimeTypes.kPlainText  ) )
			{
				final byte[] bytes = StreamTools.readBytes ( is );
				final String data = new String ( bytes, StandardCharsets.UTF_8 );
				configData.put ( "data", data );
			}
			else if ( type.equalsIgnoreCase ( MimeTypes.kAppGenericBinary ) )
			{
				final byte[] bytes = StreamTools.readBytes ( is );
				final String data = TypeConvertor.base64Encode ( bytes );
				configData.put ( "data", data );
			}
			else
			{
				throw new IllegalArgumentException ( "Unrecognized config type: " + type );
			}
		}
		
		return this;
	}

	@Override
	public FlowControlRuntimeSpec getRuntimeSpec ()
	{
		final JSONObject runtime = fData.getJSONObject ( kRuntime );
		return new FlowControlRuntimeSpec () 
		{
			@Override
			public String getName ()
			{
				return runtime.getString ( kName );
			}

			@Override
			public String getVersion ()
			{
				return runtime.getString ( kVersion );
			}
		};
	}

	@Override
	public FlowControlJob setRuntimeSpec ( FlowControlRuntimeSpec runtimeSpec )
	{
		fData.put ( kRuntime, new JSONObject ()
			.put ( kName,  runtimeSpec.getName () )
			.put ( kVersion, runtimeSpec.getVersion () )
		);
		return this;
	}

	@Override
	public Set<String> getSecretRefs ()
	{
		final TreeSet<String> result = new TreeSet<>();
		result.addAll ( fData.getJSONObject ( kSecrets ).keySet () );
		return result;
	}

	@Override
	public FlowControlJob registerSecret ( String key, String value ) throws ServiceException
	{
		fData.getJSONObject ( kSecrets )
			.put ( key, buildSecretValue ( value ) )
		;
		return this;
	}

	@Override
	public FlowControlJob removeSecretRef ( String key )
	{
		fData.getJSONObject ( kSecrets ).remove ( key );
		return this;
	}

	@Override
	public Map<String, String> getSecrets () throws ServiceException
	{
		final HashMap<String,String> result = new HashMap<> ();
		JsonVisitor.forEachElement ( fData.getJSONObject ( kSecrets ), new ObjectVisitor<JSONObject,FlowControlJobDb.ServiceException> ()
		{
			@Override
			public boolean visit ( String secretName, JSONObject secretValueHolder ) throws JSONException, FlowControlJobDb.ServiceException
			{
				final String val = secretValueHolder.getString ( kSecretValue );
				try
				{
					result.put ( secretName, fEncryptor.decrypt ( val ) );
				}
				catch ( GeneralSecurityException e )
				{
					throw new FlowControlJobDb.ServiceException ( e );
				}
				return true;
			}
		} );
		return result;
	}

	private final JSONObject fData;
	private final Encryptor fEncryptor;

	private JSONObject buildSecretValue ( String value ) throws FlowControlJobDb.ServiceException
	{
		try
		{
			return new JSONObject ()
				.put ( kSecretValue, fEncryptor.encrypt ( value ) )
			;
		}
		catch ( GeneralSecurityException e )
		{
			throw new FlowControlJobDb.ServiceException ( e );
		}
	}

	private JSONObject buildEmptyConfig ( )
	{
		return new JSONObject ()
			.put ( "type", MimeTypes.kAppJson )
			.put ( "data", new JSONObject () )
		;
	}
}
