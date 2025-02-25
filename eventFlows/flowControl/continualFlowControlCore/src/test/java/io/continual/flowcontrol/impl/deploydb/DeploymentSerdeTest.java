package io.continual.flowcontrol.impl.deploydb;

import java.security.GeneralSecurityException;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.common.Enc;
import io.continual.flowcontrol.model.FlowControlDeploymentRecord;
import io.continual.flowcontrol.model.FlowControlJobDb.ServiceException;
import io.continual.services.ServiceContainer;
import junit.framework.TestCase;

public class DeploymentSerdeTest extends TestCase
{
	@Test
	public void testBasicSerde () throws BuildFailure, ServiceException, GeneralSecurityException
	{
		final ServiceContainer sc = new ServiceContainer ();

		final Enc enc = new Enc ( sc, new JSONObject ().put ( "key", "foo" ) );
		sc.add ( "enc", enc );

		final FlowControlDeploymentRecord d1 = new DummyDeployment ( enc );
		final JSONObject ser = DeploymentSerde.serialize ( d1, enc );

		final FlowControlDeploymentRecord d2 = DeploymentSerde.deserialize ( ser );
		assertEquals ( "shh", d2.getDeploymentSpec ().getJob ().getSecrets ( enc ).get ( "topSecret" ) );
	}
}
