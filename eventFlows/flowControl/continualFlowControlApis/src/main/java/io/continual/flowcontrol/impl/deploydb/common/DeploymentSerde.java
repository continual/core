package io.continual.flowcontrol.impl.deploydb.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.flowcontrol.impl.common.JsonJob;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlResourceSpecs;
import io.continual.flowcontrol.model.FlowControlResourceSpecs.Toleration;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;

public class DeploymentSerde
{
	public static JSONObject serialize ( FlowControlDeployment d, Encryptor enc )
	{
		return new JSONObject ()
			.put ( "id", d.getId () )
			.put ( kField_Owner, d.getDeployer ().getId () )
			.put ( kField_ConfigKey, d.getConfigToken () )
			.put ( kField_JobId, d.getDeploymentSpec ().getJob ().getId () )	// pull this up for easy access and indexing 
			.put ( "spec", serialize ( d.getDeploymentSpec () ) )
		;
	}

	public static FlowControlDeployment deserialize ( JSONObject data )
	{
		return new LocalDeployment ( data );
	}
	
	public static final String kField_Owner = "deployer";
	public static final String kField_ConfigKey = "configKey";
	public static final String kField_JobId = "jobId";
	public static final String kField_JobVersion = "version";

	private static JSONObject serialize ( FlowControlDeploymentSpec ds )
	{
		return new JSONObject ()
			.put ( "job", serialize ( ds.getJob () ) )
			.put ( "instanceCount", ds.getInstanceCount () )
			.put ( "env", JsonVisitor.mapOfStringsToObject ( ds.getEnv () ) )
			.put ( "resources", serialize ( ds.getResourceSpecs () ) )
		;
	}

	private static JSONObject serialize ( FlowControlJob job )
	{
		if ( job instanceof JsonJob ) return ((JsonJob) job).toJson ();
		throw new RuntimeException ( "The DeploymentSerde only works with JsonJob FlowControlJobs." );
	}

	private static JSONObject serialize ( FlowControlResourceSpecs rs )
	{
		return new JSONObject ()
			.put ( "cpuReq", rs.cpuRequest () )
			.put ( "cpuLim", rs.cpuLimit () )
			.put ( "memLim", rs.memLimit () )
			.put ( "persistDiskSize", rs.persistDiskSize () )
			.put ( "logDiskSize", rs.logDiskSize () )
			.put ( "tolerations", JsonVisitor.listToArray ( rs.tolerations (), new ItemRenderer<Toleration,JSONObject> ()
			{
				@Override
				public JSONObject render ( Toleration t ) throws IllegalArgumentException
				{
					return new JSONObject ()
						.put ( "effect", t.effect () )
						.put ( "key", t.key () )
						.put ( "operator", t.operator () )
						.put ( "seconds", t.seconds () )
						.put ( "value", t.value () )
					;
				}
			}  ) )
		;
	}

	private static final class LocalDeployment implements FlowControlDeployment
	{
		public LocalDeployment ( JSONObject top )
		{
			fJson = top;
		}

		@Override
		public String getId ()
		{
			return fJson.optString ( "id", null );
		}

		@Override
		public AccessControlList getAccessControlList ()
		{
			return AccessControlList.deserialize ( fJson.optJSONObject ( "acl" ) );
		}

		@Override
		public Identity getDeployer ()
		{
			return new SimpleIdentityReference ( fJson.optString ( DeploymentSerde.kField_Owner, null ) );
		}

		@Override
		public String getConfigToken ()
		{
			return fJson.optString ( DeploymentSerde.kField_ConfigKey, null );
		}

		@Override
		public FlowControlDeploymentSpec getDeploymentSpec ()
		{
			final JSONObject specData = fJson.optJSONObject ( "spec" );
			if ( specData != null )
			{
				return new FlowControlDeploymentSpec ()
				{
					@Override
					public FlowControlJob getJob ()
					{
						final JSONObject job = specData.optJSONObject ( "job" );
						return new JsonJob ( job );
					}

					@Override
					public int getInstanceCount ()
					{
						return specData.optInt ( "instanceCount", 1 );
					}

					@Override
					public Map<String, String> getEnv ()
					{
						return JsonVisitor.objectToMap ( specData.optJSONObject ( "env" ) );
					}

					@Override
					public FlowControlResourceSpecs getResourceSpecs ()
					{
						final JSONObject rs = specData.optJSONObject ( "resources" );
						if ( rs == null ) return null;

						return new FlowControlResourceSpecs ()
						{
							public String cpuRequest () { return rs.optString ( "cpuReq", null ); }
							public String cpuLimit () { return rs.optString ( "cpuLim", null ); }
							public String memLimit () { return rs.optString ( "memLim", null ); }
							public String persistDiskSize () { return rs.optString ( "persistDiskSize", null ); }
							public String logDiskSize () { return rs.optString ( "logDiskSize", null ); }
							public List<Toleration> tolerations ()
							{
								final JSONArray tolArray = rs.optJSONArray ( "tolerations" );

								// we don't have to do this check for JsonVisitor.forEachElement, but when no tolerations
								// have been set, we get a null response from tolerations() vs. an empty array, so this
								// gives us consistent behavior.
								if ( tolArray == null ) return null;
								
								final LinkedList<Toleration> result = new LinkedList<> ();
								JsonVisitor.forEachElement ( tolArray, new ArrayVisitor<JSONObject,JSONException> ()
								{
									@Override
									public boolean visit ( JSONObject tol ) throws JSONException
									{
										result.add ( new Toleration ()
										{
											public String effect () { return tol.optString ( "effect", null ); }
											public String key () { return tol.optString ( "key", null ); }
											public String operator () { return tol.optString ( "operator", null ); }
											public Long seconds () { return tol.optLongObject ( "seconds", null ); }
											public String value () { return tol.optString ( "value", null ); }
										} );
										return true;
									}
								} );
								return result;
							}
						};
					}
				};
			}
			return null;
		}

		private final JSONObject fJson;
	}
}
