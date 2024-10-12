package io.continual.flowcontrol.impl.common;

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

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJobBuilder;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.util.data.StreamTools;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.standards.MimeTypes;

public class JsonJob implements FlowControlJob, JsonSerialized
{
	private static final String kId = "id";
	private static final String kDisplayName = "name";
	private static final String kAcl = "acl";

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
	public String getName ()
	{
		return fData.optString ( kDisplayName, getId() );
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
		final JSONObject runtime = fData.optJSONObject ( kRuntime );
		if ( runtime == null ) return null;

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

	private final JSONObject fData;

	protected JSONObject directDataAccess () { return fData; }

	public static class JsonJobBuilder implements FlowControlJobBuilder
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
			fBuildingData.put ( kId, id );
			return this;
		}
		
		@Override
		public JsonJobBuilder withName ( String name )
		{
			fBuildingData.put ( kDisplayName, name );
			return this;
		}

		@Override
		public JsonJobBuilder withOwner ( String owner )
		{
			fBuildingData.put ( kAcl,
				AccessControlList.deserialize ( fBuildingData.optJSONObject ( kAcl ) )
					.setOwner ( owner )
					.asJson ()
			);
			return this;
		}

		@Override
		public JsonJobBuilder withAccess ( String user, String... ops )
		{
			fBuildingData.put ( kAcl,
				AccessControlList.deserialize ( fBuildingData.optJSONObject ( kAcl ) )
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
				.put ( kDisplayName, existingJob.getName () )
				.put ( kAcl, existingJob.getAccessControlList ().asJson () )
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
					.put ( kConfigType, MimeTypes.kPlainText )
					.put ( kConfigData, new String ( bytes, StandardCharsets.UTF_8 ) )
				;
			}
			else if ( type.equalsIgnoreCase ( MimeTypes.kAppGenericBinary ) )
			{
				final byte[] bytes = StreamTools.readBytes ( config.readConfiguration () );
				configCopy
					.put ( kConfigType, MimeTypes.kAppGenericBinary )
					.put ( kConfigData, TypeConvertor.base64Encode ( bytes ) )
				;
			}
			else
			{
				throw new IllegalArgumentException ( "Unrecognized config type: " + type );
			}

			fBuildingData.put ( kConfig, configCopy );

			return this;
		}

		@Override
		public JsonJobBuilder setRuntimeSpec ( FlowControlRuntimeSpec runtimeSpec )
		{
			fBuildingData.put ( kRuntime, new JSONObject ()
				.put ( kName, runtimeSpec.getName () )
				.put ( kVersion, runtimeSpec.getVersion () )
			);
			return this;
		}

		@Override
		public JsonJobBuilder registerSecret ( String key, String value ) throws GeneralSecurityException
		{
			JSONObject secrets = fBuildingData.optJSONObject ( kSecrets );
			if ( secrets == null )
			{
				secrets = new JSONObject ();
				fBuildingData.put ( kSecrets, secrets );
			}
			secrets.put ( key, fEnc.encrypt ( value ) );

			return this;
		}

		@Override
		public JsonJobBuilder removeSecretRef ( String key )
		{
			JSONObject secrets = fBuildingData.optJSONObject ( kSecrets );
			if ( secrets != null )
			{
				secrets.remove ( key );
			}
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
	}

}
