package io.continual.flowcontrol.impl.deploydb.memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.deploydb.common.DeploymentSerde;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.services.deploydb.DeploymentDb;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.collections.MultiMap;

/**
 * A memory deployment database provided mainly for test.
 */
public class MemoryDeployDb extends SimpleService implements DeploymentDb
{
	public MemoryDeployDb ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fEnc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( "encryptor", "encryptor" ) ), Encryptor.class );

		fDeploys = new HashMap<> ();
		fConfigKeyToId = new HashMap<> ();
		fUserToDeploys = new MultiMap<> ();
		fJobIdToDeploys = new MultiMap<> ();
	}

	@Override
	public synchronized void storeDeployment ( FlowControlDeployment deployment ) throws DeployDbException
	{
		final String deployId = deployment.getId ();
		final JSONObject ser = DeploymentSerde.serialize ( deployment, fEnc );

		fDeploys.put ( deployId, ser );
	
		// add to our indexes using the serialized values for consistency with the removal below
		fConfigKeyToId.put ( ser.optString ( DeploymentSerde.kField_ConfigKey ), deployId );
		fUserToDeploys.put ( ser.optString ( DeploymentSerde.kField_Owner ) , deployId );
		fJobIdToDeploys.put ( ser.optString ( DeploymentSerde.kField_JobId ), deployId );
	}

	@Override
	public synchronized FlowControlDeployment removeDeployment ( String deployId ) throws DeployDbException
	{
		final JSONObject obj = fDeploys.remove ( deployId );

		fConfigKeyToId.remove ( obj.optString ( DeploymentSerde.kField_ConfigKey ) );
		fUserToDeploys.remove ( obj.optString ( DeploymentSerde.kField_Owner ), deployId );
		fJobIdToDeploys.remove ( obj.optString ( DeploymentSerde.kField_JobId ), deployId );

		return DeploymentSerde.deserialize ( obj );
	}

	@Override
	public synchronized FlowControlDeployment getDeploymentById ( String deployId ) throws DeployDbException
	{
		final JSONObject obj = fDeploys.get ( deployId );
		if ( obj != null )
		{
			return DeploymentSerde.deserialize ( obj );
		}
		return null;
	}

	@Override
	public synchronized List<FlowControlDeployment> getDeploymentsForUser ( Identity userId ) throws DeployDbException
	{
		final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
		for ( String did : fUserToDeploys.get ( userId.getId () ) )
		{
			result.add ( getDeploymentById ( did ) );
		}
		return result;
	}

	@Override
	public synchronized List<FlowControlDeployment> getDeploymentsOfJob ( String jobId ) throws DeployDbException
	{
		final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
		for ( String did : fJobIdToDeploys.get ( jobId ) )
		{
			result.add ( getDeploymentById ( did ) );
		}
		return result;
	}

	@Override
	public synchronized FlowControlDeployment getDeploymentByConfigKey ( String configKey ) throws DeployDbException
	{
		final String deployId = fConfigKeyToId.get ( configKey );
		if ( deployId != null )
		{
			return getDeploymentById ( deployId );
		}
		return null;
	}

	private final HashMap<String,JSONObject> fDeploys;
	private final HashMap<String,String> fConfigKeyToId;
	private MultiMap<String,String> fUserToDeploys;
	private MultiMap<String,String> fJobIdToDeploys;

	private final Encryptor fEnc;
}
