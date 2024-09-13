package io.continual.flowcontrol.impl.deploydb.common;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.enc.Enc;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException;
import io.continual.services.ServiceContainer;
import junit.framework.TestCase;

public class DeploymentSerdeTest extends TestCase
{
	@Test
	public void testBasicSerde () throws BuildFailure, ServiceException
	{
		final ServiceContainer sc = new ServiceContainer ();

		final Enc enc = new Enc ( sc, new JSONObject ().put ( "key", "foo" ) );
		sc.add ( "enc", enc );

		final FlowControlDeployment d1 = new DummyDeployment ( enc );
		final JSONObject ser = DeploymentSerde.serialize ( d1, enc );

		final FlowControlDeployment d2 = DeploymentSerde.deserialize ( ser, enc );
		assertEquals ( "shh", d2.getDeploymentSpec ().getJob ().getSecrets ().get ( "topSecret" ) );
	}
}
