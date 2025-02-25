package io.continual.flowcontrol.impl.common;

import java.util.Map;

import org.json.JSONObject;

import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlDeploymentResourceSpec;
import io.continual.util.data.json.JsonVisitor;

/**
 * A flow control deployment spec backed by a JSON data structure
 */
public class JsonDeploymentSpec implements FlowControlDeploymentSpec
{
	private static final String kInstanceCount = "instanceCount";
	private static final String kEnvMap = "environment";

	public JsonDeploymentSpec ( JSONObject data )
	{
		fData = data;
	}

	@Override
	public int getInstanceCount ()
	{
		return fData.optInt ( kInstanceCount, 1 );
	}

	@Override
	public Map<String, String> getEnv ()
	{
		return JsonVisitor.objectToMap ( fData.optJSONObject ( kEnvMap ) );
	}

	@Override
	public FlowControlJob getJob ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowControlDeploymentResourceSpec getResourceSpecs ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final JSONObject fData;
}
