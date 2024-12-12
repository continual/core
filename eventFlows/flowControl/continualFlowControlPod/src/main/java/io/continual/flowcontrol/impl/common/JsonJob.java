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
import io.continual.flowcontrol.model.FlowControlJobVersion;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.access.AccessControlList;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.standards.MimeTypes;

public class JsonJob implements FlowControlJob, JsonSerialized
{
	static final String kId = "id";
	static final String kDisplayName = "name";
	static final String kAcl = "acl";

	static final String kConfig = "config";
	static final String kConfigType = "type";
	static final String kConfigData = "data";

	static final String kRuntime = "runtime";
	static final String kName = "name";
	static final String kVersion = "version";

	static final String kSecrets = "secrets";

	static final String kCreateTime = "createTimeMs";
	static final String kUpdateTime = "updateTimeMs";

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
	public String getName ()
	{
		return fData.optString ( kDisplayName, getId() );
	}

	@Override
	public FlowControlJobVersion getVersion ()
	{
		return new JsonJobVersion ( fData.optJSONObject ( kVersion ) );
	}

	@Override
	public long getCreateTimestampMs ()
	{
		return fData.optLong ( kCreateTime, 0L );
	}

	@Override
	public long getUpdateTimestampMs ()
	{
		return fData.optLong ( kUpdateTime, 0L );
	}

	@Override
	public AccessControlList getAccessControlList ()
	{
		return AccessControlList.deserialize ( fData.optJSONObject ( kAcl ) );
	}

	@Override
	public FlowControlJobConfig getConfiguration ()
	{
		final JSONObject config = fData.optJSONObject ( kConfig );

		// in practical cases, config shouldn't be null. If it is, just return an empty object
		if ( config == null )
		{
			return new FlowControlJobConfig ()
			{
				@Override
				public String getDataType () { return MimeTypes.kAppJson; }

				@Override
				public InputStream readConfiguration ()
				{
					return new ByteArrayInputStream ( new JSONObject ().toString ().getBytes ( StandardCharsets.UTF_8 ) );
				}
			};
		}

		// return the actual config
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
				
				// we know about certain types specifically.... note that JSON is stored as plain text so that
				// we preserve user structure and comments, etc.
				if ( type.equalsIgnoreCase ( MimeTypes.kAppJson  ) || type.equalsIgnoreCase ( MimeTypes.kPlainText  ) )
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
		final JSONObject runtime = fData.optJSONObject ( kRuntime );
		if ( runtime == null ) return null;

		return FlowControlRuntimeSpec.from ( runtime.getString ( kName ), runtime.getString ( kVersion ) );
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
		JsonVisitor.forEachElement ( fData.optJSONObject ( kSecrets ), new ObjectVisitor<String,GeneralSecurityException> ()
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

	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fData );
	}

	final JSONObject fData;

	protected JSONObject directDataAccess () { return fData; }
}
