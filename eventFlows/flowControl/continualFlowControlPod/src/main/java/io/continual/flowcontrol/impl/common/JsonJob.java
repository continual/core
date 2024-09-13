package io.continual.flowcontrol.impl.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.standards.MimeTypes;

public class JsonJob implements FlowControlJob
{
	private static final String kId = "id";

	private static final String kConfig = "config";
	private static final String kConfigType = "type";
	private static final String kConfigData = "data";

	private static final String kRuntime = "runtime";
	private static final String kName = "name";
	private static final String kVersion = "version";

	private static final String kSecrets = "secrets";

	public JsonJob ( JSONObject data )
	{
		fData = data;
	}

	@Override
	public String getId ()
	{
		return fData.getString ( kId );
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
				return config.getString ( kConfigType );
			}

			@Override
			public InputStream readConfiguration ()
			{
				final String type = getDataType ();
				
				// we know about certain types specifically....
				if ( type.equalsIgnoreCase ( MimeTypes.kAppJson  ) )
				{
					final JSONObject data = config.getJSONObject ( kConfigData );
					return new ByteArrayInputStream ( data.toString ().getBytes ( StandardCharsets.UTF_8 ) );
				}
				else if ( type.equalsIgnoreCase ( MimeTypes.kPlainText  ) )
				{
					final String data = config.getString ( kConfigData );
					return new ByteArrayInputStream ( data.getBytes ( StandardCharsets.UTF_8 ) );
				}
				else if ( type.equalsIgnoreCase ( MimeTypes.kAppGenericBinary ) )
				{
					final String data = config.getString ( kConfigData );
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
	public FlowControlRuntimeSpec getRuntimeSpec ()
	{
		final JSONObject runtime = fData.getJSONObject ( kRuntime );
		return new FlowControlRuntimeSpec () 
		{
			@Override
			public String getName () { return runtime.getString ( kName ); }

			@Override
			public String getVersion () { return runtime.getString ( kVersion ); }
		};
	}

	@Override
	public Set<String> getSecretRefs ()
	{
		final TreeSet<String> result = new TreeSet<> ();
		final JSONObject secrets = fData.optJSONObject ( kSecrets );
		if ( secrets != null )
		{
			result.addAll ( secrets.keySet () );
		}
		return result;
	}

	@Override
	public Map<String, String> getSecrets ( Encryptor enc ) throws GeneralSecurityException
	{
		final HashMap<String,String> result = new HashMap<> ();
		JsonVisitor.forEachElement ( fData.getJSONObject ( kSecrets ), new ObjectVisitor<String,GeneralSecurityException> ()
		{
			@Override
			public boolean visit ( String secretName, String secretValue ) throws JSONException, GeneralSecurityException
			{
				result.put ( secretName, enc.decrypt ( secretValue ) );
				return true;
			}
		} );
		return result;
	}

	private final JSONObject fData;
}
