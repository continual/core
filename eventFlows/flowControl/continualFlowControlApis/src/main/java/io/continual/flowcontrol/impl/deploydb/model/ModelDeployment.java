package io.continual.flowcontrol.impl.deploydb.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.flowcontrol.impl.common.JsonJob;
import io.continual.flowcontrol.impl.deploydb.common.DeploymentSerde;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlResourceSpecs;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class ModelDeployment implements FlowControlDeployment
{
	public ModelDeployment ( ModelObjectFactory.ObjectCreateContext<?> ctx )
	{
		fJson = JsonModelObject.modelObjectToJson ( ctx.getData () );
	}

	@Override
	public String getId ()
	{
		return fJson.optString ( "id", null );
	}

	@Override
	public AccessControlList getAccessControlList ()
	{
		final JSONObject acl = fJson.optJSONObject ( "acl" );
		return acl == null ? AccessControlList.createOpenAcl () : AccessControlList.deserialize ( acl );
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
