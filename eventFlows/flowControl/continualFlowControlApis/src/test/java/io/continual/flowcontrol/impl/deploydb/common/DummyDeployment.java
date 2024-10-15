package io.continual.flowcontrol.impl.deploydb.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.common.JsonJob;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlJobConfig;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.model.FlowControlResourceSpecs;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.util.standards.MimeTypes;

public class DummyDeployment implements FlowControlDeployment
{
	public DummyDeployment ( Encryptor enc ) throws BuildFailure
	{
		this ( buildDefaultJob ( enc ) );
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
	public String getConfigToken () { return "configKey"; }

	@Override
	public FlowControlDeploymentSpec getDeploymentSpec ()
	{
		return new FlowControlDeploymentSpec ()
		{
			@Override
			public FlowControlJob getJob () { return fJob; }

			@Override
			public int getInstanceCount () { return 1; }

			@Override
			public Map<String, String> getEnv () { return new HashMap<String,String>(); }

			@Override
			public FlowControlResourceSpecs getResourceSpecs () { return new FlowControlResourceSpecs () {}; }
		};
	}

	private final FlowControlJob fJob;

	private static JsonJob buildDefaultJob ( Encryptor enc ) throws BuildFailure
	{
		try
		{
			final FlowControlCallContext fccc = new FlowControlCallContext ()
			{
				@Override
				public Identity getUser () { return new SimpleIdentityReference ( "user" ); }
			};
	
			return new JsonJob.JsonJobBuilder ( fccc, enc )
	
				.withId ( "id" )

				.setConfiguration ( new FlowControlJobConfig ()
				{
					@Override
					public String getDataType () { return MimeTypes.kAppJson; }
	
					@Override
					public InputStream readConfiguration () { return new ByteArrayInputStream ( new JSONObject().put ( "foo", "bar" ).toString().getBytes ( StandardCharsets.UTF_8 ) ); }
				} )
	
				.setRuntimeSpec ( FlowControlRuntimeSpec.from ( "myEngine", "1.0" ) )
	
				.registerSecret ( "topSecret", "shh" )
	
				.build ()
			;
		}
		catch ( IOException | GeneralSecurityException x )
		{
			throw new BuildFailure ( x );
		}
	}
}
