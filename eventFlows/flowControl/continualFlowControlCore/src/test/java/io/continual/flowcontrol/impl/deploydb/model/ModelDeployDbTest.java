package io.continual.flowcontrol.impl.deploydb.model;

import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.common.Enc;
import io.continual.flowcontrol.impl.deploydb.DeploymentSerde;
import io.continual.flowcontrol.impl.deploydb.DeploymentTestContext;
import io.continual.flowcontrol.impl.deploydb.DummyDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentDb.DeployDbException;
import io.continual.flowcontrol.model.FlowControlDeploymentRecord;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.mem.InMemoryModel;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class ModelDeployDbTest extends TestCase
{
	@Test
	public void testDb () throws BuildFailure, DeployDbException, ModelRequestException, ModelServiceException
	{
		final ServiceContainer sc = new ServiceContainer ();

		final Enc enc = new Enc ( sc, new JSONObject ().put ( "key", "password" ) );
		sc.add ( "encryptor", enc );

		final Model mod = new InMemoryModel ( "testDb" );
		sc.add ( "deployDbModel", mod );

		try ( ModelRequestContext modelContext = mod.getRequestContextBuilder ()
			.forSimpleIdentity ( "test@test.me" )
			.build ()
		)
		{
			mod.createUpdate ( modelContext, Path.fromString ( "/deployments" ) )
				.overwriteData ( new JsonModelObject () )
				.execute ()
			;
		}

		final ModelDeployDb db = new ModelDeployDb ( sc, new JSONObject () );

		final FlowControlDeploymentRecord d1 = new DummyDeployment ( enc );
		db.storeDeployment ( d1 );
		final JSONObject d1ser = DeploymentSerde.serialize ( d1, enc );

		final FlowControlDeploymentRecord d2 = db.getDeploymentById ( d1.getId () );
		assertNotNull ( d2 );
		final JSONObject d2ser = DeploymentSerde.serialize ( d2, enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d2ser ) );

		final FlowControlDeploymentRecord d3 = db.getDeploymentByConfigKey ( d1.getConfigToken () );
		assertNotNull ( d3 );
		final JSONObject d3ser = DeploymentSerde.serialize ( d3, enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d3ser ) );

		final FlowControlDeploymentRecord d4 = db.getDeploymentByConfigKey ( d1.getConfigToken () + "bogus" );
		assertNull ( d4 );

		final List<FlowControlDeploymentRecord> d5 = db.getDeploymentsForUser ( new DeploymentTestContext ( d1.getDeployer () ) );
		assertNotNull ( d5 );
		assertEquals ( 1, d5.size () );
		final JSONObject d5ser = DeploymentSerde.serialize ( d5.get ( 0 ), enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d5ser ) );

		final List<FlowControlDeploymentRecord> d6 = db.getDeploymentsOfJob ( d1.getDeploymentSpec ().getJob ().getId () );
		assertNotNull ( d6 );
		assertEquals ( 1, d6.size () );
		final JSONObject d6ser = DeploymentSerde.serialize ( d5.get ( 0 ), enc );
		assertEquals ( JsonUtil.writeConsistently ( d1ser ), JsonUtil.writeConsistently ( d6ser ) );
	}
}
