package io.continual.flowcontrol.impl.deployer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeploymentDb;
import io.continual.flowcontrol.model.FlowControlDeploymentRecord;
import io.continual.flowcontrol.model.FlowControlDeploymentService;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlDeploymentDb.DeployDbException;
import io.continual.flowcontrol.model.FlowControlDeploymentResourceSpec;
import io.continual.flowcontrol.model.FlowControlDeploymentResourceSpec.Toleration;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.StringUtils;
import io.continual.util.data.TypeConvertor;
import io.continual.util.time.Clock;

public class BaseDeployer extends SimpleService implements FlowControlDeploymentService
{
	static final String kSetting_DeployDb = "deploymentDb";

	static final String kSetting_DefaultCpuRequest = "defaultCpuRequest";
	static final String kSetting_DefaultCpuLimit = "defaultCpuLimit";
	static final String kSetting_DefaultMemLimit = "defaultMemLimit";
	static final String kSetting_DefaultPersistDiskSize = "defaultPersistDiskSize";
	static final String kSetting_DefaultLogDiskSize = "defaultLogDiskSize";

	public BaseDeployer ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		final JSONObject config = sc.getExprEval ().evaluateJsonObject ( rawConfig );

		fDeployDb = sc.getReqd ( config.optString ( kSetting_DeployDb, "deployDb" ), FlowControlDeploymentDb.class );

		fSigningKey = config.getString ( "signingKey" );
		if ( fSigningKey.length () == 0 )
		{
			throw new BuildFailure ( "Config signing key is an empty string." );
		}

		fDefCpuRequest = config.optString ( kSetting_DefaultCpuRequest, null );
		fDefCpuLimit = config.optString ( kSetting_DefaultCpuLimit, null );
		fDefMemLimit = config.optString ( kSetting_DefaultMemLimit, null );
		fDefPersistDiskSize = config.optString ( kSetting_DefaultPersistDiskSize, null );
		fDefLogDiskSize = config.optString ( kSetting_DefaultLogDiskSize, null );
	}

	@Override
	public DeploymentSpecBuilder deploymentBuilder ()
	{
		return new LocalDeploymentSpecBuilder ();
	}

	@Override
	public FlowControlDeploymentRecord deploy ( FlowControlCallContext ctx, FlowControlDeploymentSpec spec ) throws ServiceException, RequestException
	{
		final String configKey = createConfigurationKey ( spec.getJob () );

		final FlowControlDeploymentRecord deploy = internalDeploy ( ctx, spec, configKey );
		try
		{
			fDeployDb.storeDeployment ( deploy );
		}
		catch ( DeployDbException e )
		{
			internalUndeploy ( ctx, deploy.getId (), deploy );
			throw new ServiceException ( e );
		}
		return deploy;
	}

	@Override
	public void undeploy ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		try
		{
			// look up the deployment
			final FlowControlDeploymentRecord deployment = getDeployment ( ctx, deploymentId );
			if ( deployment == null )
            {
                return;
            }

			// undeploy it...
			internalUndeploy ( ctx, deploymentId, deployment );

			// remove it from our db...
			fDeployDb.removeDeployment ( deploymentId );
		}
		catch ( DeployDbException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public FlowControlDeploymentRecord getDeployment ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		try
		{
			final FlowControlDeploymentRecord d = fDeployDb.getDeploymentById ( deploymentId );
			if ( d != null && d.getDeployer ().getId ().equals ( ctx.getUser ().getId () ) )
			{
				return d;
			}
			return null;
		}
		catch ( DeployDbException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public List<FlowControlDeploymentRecord> getDeployments ( FlowControlCallContext ctx ) throws ServiceException
	{
		try
		{
			return fDeployDb.getDeploymentsForUser ( ctx );
		}
		catch ( DeployDbException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public List<FlowControlDeploymentRecord> getDeploymentsForJob ( FlowControlCallContext ctx, String jobId ) throws ServiceException
	{
		try
		{
			return fDeployDb.getDeploymentsOfJob ( jobId );
		}
		catch ( DeployDbException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public FlowControlDeploymentRecord getDeploymentByConfigKey ( String configKey ) throws ServiceException, RequestException
	{
		try
		{
			validateConfigKey ( configKey );
			return fDeployDb.getDeploymentByConfigKey ( configKey );
		}
		catch ( DeployDbException e )
		{
			throw new ServiceException ( e );
		}
	}

	private final String fSigningKey;
	private final FlowControlDeploymentDb fDeployDb;

	private final String fDefCpuLimit;
	private final String fDefCpuRequest;
	private final String fDefMemLimit;
	private final String fDefPersistDiskSize;
	private final String fDefLogDiskSize;

	private static final Logger log = LoggerFactory.getLogger ( BaseDeployer.class );
	
	/**
	 * Create the deployment
	 * @param ctx
	 * @param spec
	 * @return a deployment (don't return null)
	 * @throws ServiceException
	 * @throws RequestException
	 */
	protected FlowControlDeploymentRecord internalDeploy ( FlowControlCallContext ctx, FlowControlDeploymentSpec spec, String configKey ) throws ServiceException, RequestException
	{
		throw new ServiceException ( "Not implemented in " + this.getClass ().getName () );
	}

	/**
	 * Undeploy the deployment
	 * @param ctx
	 * @param deploymentId
	 * @param deployment 
	 * @throws ServiceException
	 */
	protected void internalUndeploy ( FlowControlCallContext ctx, String deploymentId, FlowControlDeploymentRecord deployment ) throws ServiceException
	{
		throw new ServiceException ( "Not implemented in " + this.getClass ().getName () );
	}

	protected class LocalDeploymentSpecBuilder implements DeploymentSpecBuilder
	{
		@Override
		public DeploymentSpecBuilder forJob ( FlowControlJob job )
		{
			fJob = job;
			return this;
		}

		@Override
		public DeploymentSpecBuilder withInstances ( int count )
		{
			fInstances = count;
			return this;
		}

		@Override
		public DeploymentSpecBuilder withEnv ( String key, String val )
		{
			fEnv.put ( key, val );
			return this;
		}

		@Override
		public DeploymentSpecBuilder withEnv ( Map<String, String> keyValMap )
		{
			fEnv.putAll ( keyValMap );
			return this;
		}

		@Override
		public ResourceSpecBuilder withResourceSpecs ()
		{
			return new ResourceSpecBuilder ()
			{
				@Override
				public ResourceSpecBuilder withCpuRequest ( String cpuReq )
				{
					fCpuRequest = selectValue ( cpuReq, fCpuRequest, fCpuLimit );
					return this;
				}

				@Override
				public ResourceSpecBuilder withCpuLimit ( String cpuLimit )
				{
					fCpuLimit = selectValue ( cpuLimit, fCpuLimit );
					return this;
				}

				@Override
				public ResourceSpecBuilder withMemLimit ( String memLimit )
				{
					fMemLimit = selectValue ( memLimit, fMemLimit );
					return this;
				}

				@Override
				public ResourceSpecBuilder withPersistDiskSize ( String diskSize )
				{
					fPersistDiskSize = selectValue ( diskSize, fPersistDiskSize );
					return this;
				}

				@Override
				public ResourceSpecBuilder withLogDiskSize ( String diskSize )
				{
					fLogDiskSize = selectValue ( diskSize, fLogDiskSize );
					return this;
				}

				@Override
				public ResourceSpecBuilder withToleration ( Toleration tol )
				{
					fTolerations.add ( tol );
					return this;
				}

				@Override
				public DeploymentSpecBuilder build ()
				{
					return LocalDeploymentSpecBuilder.this;
				}
			};
		}

		@Override
		public FlowControlDeploymentSpec build () throws BuildFailure
		{
			if ( fJob == null ) throw new BuildFailure ( "No job provided." );
			return new LocalDeploymentSpec ( this );
		}

		private FlowControlJob fJob;
		private int fInstances = 1;
		private HashMap<String,String> fEnv = new HashMap<> ();
		private String fCpuLimit = fDefCpuLimit;
		private String fCpuRequest = fDefCpuRequest;
		private String fMemLimit = fDefMemLimit;
		private String fPersistDiskSize = fDefPersistDiskSize;
		private String fLogDiskSize = fDefLogDiskSize;
		private LinkedList<Toleration> fTolerations = new LinkedList<>();
	}
	
	private String createConfigurationKey ( FlowControlJob job )
	{
		final String jobId = job.getId ();
		final long createdAtMs = Clock.now ();

		final StringBuilder in = new StringBuilder ();
		in
			.append ( jobId )
			.append ( "." )
			.append ( createdAtMs )
		;
		final String tag = in.toString ();

		final String tagEnc = TypeConvertor.base64UrlEncode ( tag );
		final String tagSigned = TypeConvertor.base64UrlEncode ( Sha256HmacSigner.sign ( tag, fSigningKey ) );
		final String key = tagEnc + "-" + tagSigned;

		log.info ( "job [" + jobId + "] => [" + key + "]" );

		return key;
	}

	private void validateConfigKey ( String configKey ) throws RequestException
	{
		final String[] parts = StringUtils.splitList ( configKey, new char[] {'-'}, new char[] {} );
		if ( parts.length != 2 )
		{
			throw new RequestException ( "Configuration key is malformed." );
		}

		final String tagPart = new String ( TypeConvertor.base64UrlDecode ( parts[0] ), StandardCharsets.UTF_8 );
		final String sigPart = new String ( TypeConvertor.base64UrlDecode ( parts[1] ), StandardCharsets.UTF_8 ); 

		// check the signature for this tag against the signature provided
		final String tagSigned = Sha256HmacSigner.sign ( tagPart, fSigningKey );
		if ( !tagSigned.equals ( sigPart ) )
		{
			throw new RequestException ( "Configuration key signature is incorrect." );
		}

		// pull out the tag parts
		final String[] idAndTimestamp = StringUtils.splitList ( tagPart, new char[] {'.'}, new char[] {} );
		if ( idAndTimestamp.length != 2 )
		{
			throw new RequestException ( "Configuration key's tag is malformed." );
		}
	}

	private static class LocalDeploymentSpec implements FlowControlDeploymentSpec
	{
		private final LocalDeploymentSpecBuilder fBuilder;

		public LocalDeploymentSpec ( LocalDeploymentSpecBuilder builder )
		{
			fBuilder = builder;
		}

		@Override
		public FlowControlJob getJob () { return fBuilder.fJob; }

		@Override
		public int getInstanceCount () { return fBuilder.fInstances; }

		@Override
		public Map<String, String> getEnv () { return fBuilder.fEnv; }

		@Override
		public FlowControlDeploymentResourceSpec getResourceSpecs ()
		{
			return new FlowControlDeploymentResourceSpec ()
			{
				public String cpuRequest () { return fBuilder.fCpuRequest; }
				public String cpuLimit () { return fBuilder.fCpuLimit; }
				public String memLimit () { return fBuilder.fMemLimit; }
				public String persistDiskSize () { return fBuilder.fPersistDiskSize; }
				public String logDiskSize () { return fBuilder.fLogDiskSize; }
				public List<Toleration> tolerations ()
				{
					return fBuilder.fTolerations;
				}
			};
		}
	}

	private static String selectValue ( String... values )
	{
		for ( String val : values )
		{
			if ( val != null && val.length() > 0 )
			{
				return val;
			}
		}
		return null;
	}
}
