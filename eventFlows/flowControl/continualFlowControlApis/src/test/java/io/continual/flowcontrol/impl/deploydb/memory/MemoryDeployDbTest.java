package io.continual.flowcontrol.impl.deploydb.memory;

import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.deploydb.common.DeploymentSerde;
import io.continual.flowcontrol.impl.deploydb.common.DummyDeployment;
import io.continual.flowcontrol.impl.enc.Enc;
import io.continual.flowcontrol.services.deploydb.DeploymentDb.DeployDbException;
import io.continual.flowcontrol.services.deployer.FlowControlDeployment;
import io.continual.services.ServiceContainer;
import io.continual.util.data.json.JsonUtil;
import junit.framework.TestCase;

public class MemoryDeployDbTest extends TestCase
{
	@Test
	public void testMemDb () throws BuildFailure, DeployDbException
	{
		final ServiceContainer sc = new ServiceContainer ();

		final Enc enc = new Enc ( sc, new JSONObject ().put ( "key", "password" ) );
		sc.add ( "encryptor", enc );

		final MemoryDeployDb db = new MemoryDeployDb ( sc, new JSONObject () );

		final FlowControlDeployment d1 = new DummyDeployment ( enc );
		db.storeDeployment ( d1 );
		final JSONObject d1ser = DeploymentSerde.serialize ( d1, enc );

		final FlowControlDeployment d2 = db.getDeploymentById ( d1.getId () );
		assertNotNull ( d2 );
		final JSONObject d2ser = DeploymentSerde.serialize ( d2, enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d2ser ) );

		final FlowControlDeployment d3 = db.getDeploymentByConfigKey ( d1.getConfigKey () );
		assertNotNull ( d3 );
		final JSONObject d3ser = DeploymentSerde.serialize ( d3, enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d3ser ) );

		final FlowControlDeployment d4 = db.getDeploymentByConfigKey ( d1.getConfigKey () + "bogus" );
		assertNull ( d4 );

		final List<FlowControlDeployment> d5 = db.getDeploymentsForUser ( d1.getDeployer () );
		assertNotNull ( d5 );
		assertEquals ( 1, d5.size () );
		final JSONObject d5ser = DeploymentSerde.serialize ( d5.get ( 0 ), enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d5ser ) );

		final List<FlowControlDeployment> d6 = db.getDeploymentsOfJob ( d1.getDeploymentSpec ().getJob ().getId () );
		assertNotNull ( d6 );
		assertEquals ( 1, d6.size () );
		final JSONObject d6ser = DeploymentSerde.serialize ( d5.get ( 0 ), enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d6ser ) );
	}
}
