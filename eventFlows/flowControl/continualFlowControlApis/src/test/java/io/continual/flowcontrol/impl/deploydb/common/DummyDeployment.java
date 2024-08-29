package io.continual.flowcontrol.impl.deploydb.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.jobdb.common.JsonJob;
import io.continual.flowcontrol.services.deployer.FlowControlDeployment;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.DeploymentSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ResourceSpecs;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.flowcontrol.services.jobdb.FlowControlJob;
import io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.util.standards.MimeTypes;

public class DummyDeployment implements FlowControlDeployment
{
	public DummyDeployment ( Encryptor enc ) throws BuildFailure
	{
		this ( new DefaultJob ( enc ) );
	}

	public DummyDeployment ( FlowControlJob job )
	{
		fJob = job;
	}

	@Override
	public String getId () { return "id"; }

	@Override
	public Identity getDeployer () { return new SimpleIdentityReference ( "user" ); }

	@Override
	public String getConfigKey () { return "configKey"; }

	@Override
	public DeploymentSpec getDeploymentSpec ()
	{
		return new DeploymentSpec ()
		{
			@Override
			public FlowControlJob getJob () { return fJob; }

			@Override
			public int getInstanceCount () { return 1; }

			@Override
			public Map<String, String> getEnv () { return new HashMap<String,String>(); }

			@Override
			public ResourceSpecs getResourceSpecs () { return new ResourceSpecs () {}; }
		};
	}

	private final FlowControlJob fJob;

	private static class DefaultJob extends JsonJob
	{
		public DefaultJob ( Encryptor enc ) throws BuildFailure
		{
			super ( "myJob", enc );

			try
			{
				setConfiguration ( new FlowControlJobConfig ()
				{
					@Override
					public String getDataType () { return MimeTypes.kAppJson; }
	
					@Override
					public InputStream readConfiguration () { return new ByteArrayInputStream ( new JSONObject().put ( "foo", "bar" ).toString().getBytes ( StandardCharsets.UTF_8 ) ); }
				} );
	
				setRuntimeSpec ( new FlowControlRuntimeSpec ()
				{
					@Override
					public String getName () { return "myEngine"; }
	
					@Override
					public String getVersion () { return "1.0"; }
				} );
	
				registerSecret ( "topSecret", "shh" );
			}
			catch ( IOException | ServiceException x )
			{
				throw new BuildFailure ( x );
			}
		}
	};
}
