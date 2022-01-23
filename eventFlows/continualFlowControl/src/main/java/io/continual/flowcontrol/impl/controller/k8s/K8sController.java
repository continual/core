package io.continual.flowcontrol.impl.controller.k8s;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.controlapi.FlowControlDeployment;
import io.continual.flowcontrol.controlapi.FlowControlDeploymentService;
import io.continual.flowcontrol.controlapi.FlowControlRuntimeSpec;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.StreamTools;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.standards.HttpStatusCodes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.PodResource;

public class K8sController extends SimpleService implements FlowControlDeploymentService
{
	private static final String kSetting_k8sContext = "context";
	private static final String kSetting_Namespace = "namespace";

	private static final String kSetting_StorageClass = "storageClass";
	private static final String kDefault_StorageClass = "";

	private static final String kSetting_ConfigMountLoc = "configMountLoc";
	private static final String kDefault_ConfigMountLoc = "/var/flowcontrol";
	
	public K8sController ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fConfigTransfer = sc.get ( config.optString ( "configTransfer", "configTransfer" ), ConfigTransferService.class );
		if ( fConfigTransfer == null ) throw new BuildFailure ( "No configTransfer service" );

		final String contextName = config.optString ( kSetting_k8sContext, null );
		if ( contextName != null )
		{
			final Config cfgWithContext = Config.autoConfigure ( contextName );
			fApiClient = new DefaultKubernetesClient ( cfgWithContext );
		}
		else
		{
			fApiClient = new DefaultKubernetesClient ();
		}
		fNamespace = config.getString ( kSetting_Namespace );
		fStorageClass = config.optString ( kSetting_StorageClass, kDefault_StorageClass );
		fConfigMountLoc = config.optString ( kSetting_ConfigMountLoc, kDefault_ConfigMountLoc );

		final JSONObject mapperSpec = config.optJSONObject ( "imageMapper" );
		if ( mapperSpec != null )
		{
			fImageMapper = Builder.fromJson ( ContainerImageMapper.class, mapperSpec, sc );
		}
		else
		{
			fImageMapper = new SimpleImageMapper ();
		}
	}

	@Override
	protected void onStopRequested ()
	{
		super.onStopRequested ();

		fApiClient.close ();
	}

	@Override
	public DeploymentSpecBuilder deploymentBuilder ()
	{
		return new LocalDeploymentSpecBuilder ();
	}

	@Override
	public FlowControlDeployment deploy ( FlowControlCallContext ctx, DeploymentSpec ds ) throws ServiceException, RequestException
	{
		try
		{
			final String jobId = ds.getJob ().getId ();
			final String tag = UniqueStringGenerator.createKeyUsingAlphabet ( jobId, "abcdefhigjklmnopqrstuvwxyz" );
			final Map<String,String> configFetchEnv = fConfigTransfer.deployConfiguration ( ds.getJob () );

			final String targetConfigFile = fConfigMountLoc + "/jobConfig.json";
			
			// warning: don't use a key that's a substring of another key, because we don't pay attn to order here
			final HashMap<String,String> replacements = new HashMap<>();
			replacements.put ( "FC_DEPLOYMENT_NAME", tag );
			replacements.put ( "FC_JOB_TAG", "job-" + tag );
			replacements.put ( "FC_JOB_ID", jobId );
			replacements.put ( "FC_STORAGE_CLASS", fStorageClass );
			replacements.put ( "FC_INSTANCE_COUNT", "" + ds.getInstanceCount () );
			replacements.put ( "FC_RUNTIME_IMAGE", fImageMapper.getImageName ( ds.getJob ().getRuntimeSpec () ) );
			replacements.put ( "FC_CONFIG_MOUNT", fConfigMountLoc );
			replacements.put ( "FC_CONFIG_FILE", targetConfigFile );
			replacements.put ( "FC_INITER_IMAGE", "busybox:1.28" );

			// place any secrets from this job
			final String secretsName = tagToSecret ( tag );

			// start a secret for this job's secret data
			final Map<String,String> secrets = ds.getJob ().getSecrets ();
			SecretBuilder sb = new SecretBuilder ()
				.withType ( "Opaque" )
				.withNewMetadata ()
					.withName ( secretsName )
				.endMetadata ()
			;
			boolean anyInternalSecrets = false;
			for ( Map.Entry<String,String> secret : secrets.entrySet () )
			{
				final String val = secret.getValue ();
				final boolean isInternal = val != null;

				if ( isInternal )
				{
					anyInternalSecrets = true;
					sb = sb.addToData ( secret.getKey (), TypeConvertor.base64Encode ( secret.getValue () ) );
				}
			}
			if ( anyInternalSecrets )
			{
				fApiClient.secrets ().inNamespace ( fNamespace ).createOrReplace ( sb.build () );
			}

			// get deployment installed
			try ( final InputStream deployTemplate = getClass ().getResourceAsStream ( "initDeployment.yaml" ) )
			{
				if ( deployTemplate == null ) throw new ServiceException ( "Couldn't load resource yaml" );
				final List<HasMetadata> items = fApiClient.load ( replaceAllTokens ( deployTemplate, replacements ) ).get ();

				// push environment
				final HashMap<String,String> env = new HashMap<String,String> ();
				env.putAll ( ds.getEnv () );
				env.putAll ( configFetchEnv );
				env.put ( "FC_CONFIG_DIR", fConfigMountLoc );
				env.put ( "CONFIG_FILE", targetConfigFile );

				for ( HasMetadata md : items )
				{
					if ( md.getKind ().equals ( "Deployment" ) )
					{
						final Deployment d = (Deployment) md;
						final PodSpec ps = d.getSpec ().getTemplate ().getSpec ();

						for ( Container c : ps.getContainers () )
						{
							pushEnvMapToContainer ( env, c );
							addSecretsToContainer ( secretsName, secrets, c );
						}
						for ( Container c : ps.getInitContainers () )
						{
							pushEnvMapToContainer ( env, c );
							addSecretsToContainer ( secretsName, secrets, c );
						}
					}
				}
				
				fApiClient
					.resourceList ( items )
					.inNamespace ( fNamespace )
					.createOrReplace ()
				;
			}
			catch ( IOException e )
			{
				throw new ServiceException ( e );
			}
			catch ( KubernetesClientException x )
			{
				mapException ( x );
			}
	
			return new IntDeployment ( tag, jobId );
		}
		catch ( ConfigTransferService.ServiceException x )
		{
			throw new ServiceException ( x );
		}
		catch ( io.continual.flowcontrol.jobapi.FlowControlJobDb.ServiceException x )
		{
			throw new ServiceException ( x );
		}
	}

	@Override
	public void undeploy ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		// FIXME: does user own deployment?

		try
		{
			final Deployment d = fApiClient.apps().deployments ().inNamespace ( fNamespace ).withName ( deploymentId ).get ();
			if ( d != null )
			{
				fApiClient.resource ( d ).delete ();
			}
		}
		catch ( KubernetesClientException | IllegalStateException x )
		{
			// spec says object should be null if it doesn't exist, but testing implies this exception is thrown instead
		}
	
		try
		{
			final Secret secret = fApiClient.secrets ().inNamespace ( fNamespace ).withName ( tagToSecret ( deploymentId ) ).get ();
			if ( secret != null )
			{
				fApiClient.resource ( secret ).delete ();
			}
		}
		catch ( KubernetesClientException | IllegalStateException x )
		{
			// spec says object should be null if it doesn't exist, but testing implies this exception is thrown instead
		}
	}

	@Override
	public FlowControlDeployment getDeployment ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		try
		{
			final Deployment d = fApiClient.apps().deployments().inNamespace ( fNamespace ).withName ( deploymentId ).get ();
			if ( d == null ) return null;
			
			final String jobId = d.getMetadata ().getLabels ().get ( "flowcontroljob" );
			return new IntDeployment ( d.getMetadata ().getName (), jobId == null ? "(unknown)" : jobId );
		}
		catch ( KubernetesClientException x )
		{
			final Throwable cause = x.getCause ();
			if ( cause instanceof java.net.ProtocolException )
			{
				// this is a "not found" symptom
				return null;
			}
			
			mapExceptionSvcOnly ( x );
			return null;	// unreachable
		}
		catch ( IllegalStateException x )
		{
			return null;
		}
	}

	@Override
	public List<FlowControlDeployment> getDeployments ( FlowControlCallContext ctx ) throws ServiceException
	{
		final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
		try
		{
			for ( Deployment d : fApiClient.apps().deployments().inNamespace ( fNamespace ).list ().getItems ()  )
			{
				final String jobId = d.getMetadata ().getLabels ().get ( "flowcontroljob" );
				result.add ( new IntDeployment ( d.getMetadata ().getName (), jobId == null ? "(unknown)" : jobId ) );
			}
		}
		catch ( KubernetesClientException x )
		{
			mapExceptionSvcOnly ( x );
		}
		return result;
	}

	@Override
	public List<FlowControlDeployment> getDeploymentsForJob ( FlowControlCallContext ctx, String jobId ) throws ServiceException
	{
		final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
		try
		{
			for ( Deployment d : fApiClient.apps().deployments().inNamespace ( fNamespace ).list ().getItems ()  )
			{
				final String thisJobId = d.getMetadata ().getLabels ().get ( "flowcontroljob" );
				if ( jobId.equals ( thisJobId ) )
				{
					result.add ( new IntDeployment ( d.getMetadata ().getName (), thisJobId ) );
				}
			}
		}
		catch ( KubernetesClientException x )
		{
			mapExceptionSvcOnly ( x );
		}
		return result;
	}

	private final ConfigTransferService fConfigTransfer;
	private final KubernetesClient fApiClient;
	private final String fNamespace;
	private final String fStorageClass;
	private final String fConfigMountLoc;
	private final ContainerImageMapper fImageMapper;

	private static class SimpleImageMapper implements ContainerImageMapper
	{
		@Override
		public String getImageName ( FlowControlRuntimeSpec rs )
		{
			return rs.getName () + ":" + rs.getVersion ();
		}
	}

	private static String tagToSecret ( String tag )
	{
		return "secret-" + tag;
	}

	private void addSecretsToContainer ( String secretName, Map<String, String> secrets, Container c )
	{
		List<EnvVar> list = c.getEnv ();

		for ( Map.Entry<String,String> e : secrets.entrySet () )
		{
			if ( e.getValue () != null )
			{
				list.add ( new EnvVar ( e.getKey (), null, new EnvVarSource ( null, null, null, new SecretKeySelector ( e.getKey (), secretName, true ) ) ) );
			}
		}
	}

	private void pushEnvMapToContainer ( Map<String,String> env, Container c )
	{
		List<EnvVar> list = c.getEnv ();
		for ( Map.Entry<String,String> e : env.entrySet () )
		{
			list.add ( new EnvVar ( e.getKey (), e.getValue (), null ) );
		}
	}

	private static void mapException ( KubernetesClientException x ) throws RequestException, ServiceException
	{
		// relay this exception...
		final int status = x.getStatus ().getCode ();
		if ( HttpStatusCodes.isClientFailure ( status ) )
		{
			switch ( status )
			{
				case HttpStatusCodes.k404_notFound:
					throw new FlowControlDeploymentService.RequestException ( "Object not found." );
				case HttpStatusCodes.k400_badRequest:
					throw new FlowControlDeploymentService.RequestException ( "Bad request." );
				default:
					throw new FlowControlDeploymentService.RequestException ( x );
			}
		}
		else
		{
			throw new FlowControlDeploymentService.ServiceException ( x );
		}
	}

	private static void mapExceptionSvcOnly ( KubernetesClientException x ) throws ServiceException
	{
		throw new FlowControlDeploymentService.ServiceException ( x );
	}

	private class IntDeployment implements FlowControlDeployment
	{
		public IntDeployment ( String tag, String jobId )
		{
			fTag = tag;
			fJobId = jobId;
		}

		@Override
		public String getId ()
		{
			return fTag;
		}

		@Override
		public String getJobId ()
		{
			return fJobId;
		}

		@Override
		public Status getStatus ()
		{
			final Deployment d = getDeployment ( fTag );
			if ( d != null )
			{
				boolean progressing = false;
				boolean available = false;
				
				final DeploymentStatus ds = d.getStatus ();
				for ( DeploymentCondition dc : ds.getConditions () )
				{
					final String type = dc.getType ();
					final String status = dc.getStatus ();

					if ( type.equalsIgnoreCase ( "progressing" ) && status.equalsIgnoreCase ( "true" ) )
					{
						progressing = true;
					}
					else if ( type.equalsIgnoreCase ( "available" ) && status.equalsIgnoreCase ( "true" ) )
					{
						available = true;
					}
				}

				if ( progressing && available )
				{
					return Status.RUNNING;
				}
				else if ( progressing && !available )
				{
					return Status.PENDING;
				}
			}

			return Status.UNKNOWN;
		}

		@Override
		public int instanceCount ()
		{
			final Deployment d = getDeployment ( fTag );
			if ( d != null )
			{
				final DeploymentStatus ds = d.getStatus ();
				return ds.getReplicas ();
			}

			return -1;
		}

		@Override
		public Set<String> instances ()
		{
			final TreeSet<String> result = new TreeSet<> ();
			final List<Pod> pods = getPodsFor ( fTag );
			for ( Pod p : pods )
			{
				result.add ( p.getMetadata ().getName () );
			}
			return result;
		}

		@Override
		public List<String> getLog ( String instanceId, String sinceRfc3339Time ) throws RequestException, ServiceException
		{
			final LinkedList<String> result = new LinkedList<> ();
			try
			{
				final PodResource<Pod> pod = fApiClient.pods ()
					.inNamespace ( fNamespace )
					.withName ( instanceId )
				;

				final String logText;
				if ( sinceRfc3339Time != null )
				{
					logText = pod.sinceTime ( sinceRfc3339Time ).getLog (); 
				}
				else
				{
					logText = pod.getLog ();
				}

				final String[] lines = logText.split ( "\\n" );
				for ( String line : lines )
				{
					result.add ( line );
				}
			}
			catch ( KubernetesClientException x )
			{
				mapException ( x );
			}
			return result;
		}

		private final String fTag;
		private final String fJobId;
	}

	private Deployment getDeployment ( String tag )
	{
		return fApiClient.apps().deployments ().inNamespace ( fNamespace ).withName ( tag ).get ();
	}

	private List<Pod> getPodsFor ( String tag )
	{
		final PodList pl = fApiClient
			.pods ()
			.inNamespace ( fNamespace )
			.withLabel ( "app", "job-" + tag )

			.list ()
		;
		return pl.getItems ();
	}

	// this is intended for small scale stream handling only
	private static InputStream replaceAllTokens ( InputStream src, Map<String,String> replacements ) throws IOException
	{
		final byte[] bytes = StreamTools.readBytes ( src );
		final String origText = new String ( bytes, StandardCharsets.UTF_8 );
		String newText = origText;
		for ( Map.Entry<String,String> e : replacements.entrySet () )
		{
			newText = newText.replaceAll ( e.getKey (), e.getValue () );
		}
		return new ByteArrayInputStream ( newText.getBytes ( StandardCharsets.UTF_8 ) );
	}

	private static class LocalDeploymentSpecBuilder implements DeploymentSpecBuilder
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
		public DeploymentSpec build () throws BuildFailure
		{
			if ( fJob == null ) throw new BuildFailure ( "No job provided." );
			return new LocalDeploymentSpec ( this );
		}

		private FlowControlJob fJob;
		private int fInstances = 1;
		private HashMap<String,String> fEnv = new HashMap<> ();
	}

	private static class LocalDeploymentSpec implements DeploymentSpec
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
	}
}
