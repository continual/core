package io.continual.flowcontrol.impl.controller.k8s;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.controlapi.FlowControlDeployment;
import io.continual.flowcontrol.controlapi.FlowControlDeploymentService;
import io.continual.flowcontrol.controlapi.FlowControlRuntimeSpec;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.resources.ResourceLoader;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.StreamTools;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.JsonDataSource;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
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
	static final String kSetting_k8sContext = "context";
	static final String kSetting_Namespace = "namespace";

	static final String kSetting_ConfigMountLoc = "configMountLoc";
	static final String kDefault_ConfigMountLoc = "/var/flowcontrol";

	static final String kSetting_ConfigTransfer = "configTransfer";

	static final String kSetting_InitYamlResource = "deploymentYaml";
	static final String kDefault_InitYamlResource = "initDeployment.yaml";

	static final String kSetting_InitYamlSettings = "deploymentSettings";

	static final String kSetting_InitYamlImagePullSecrets = "imagePullSecrets";

	static final String kSetting_DumpInitYaml = "dumpInitYaml";

	static final String kSetting_DefaultCpuLimit = "defaultCpuLimit";
	static final String kSetting_DefaultMemLimit = "defaultMemLimit";

	public K8sController ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		final JSONObject config = sc.getExprEval ().evaluateJsonObject ( rawConfig );

		fConfigTransfer = sc.get ( config.optString ( kSetting_ConfigTransfer, "configTransfer" ), ConfigTransferService.class );
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

		fInitYamlResource = config.optString ( kSetting_InitYamlResource, kDefault_InitYamlResource );
		fInitYamlSettings = config.optJSONObject ( kSetting_InitYamlSettings );
		
		fImgPullSecrets = JsonVisitor.arrayToList ( config.optJSONArray ( kSetting_InitYamlImagePullSecrets ) );
		fDumpInitYaml = config.optBoolean ( kSetting_DumpInitYaml, false );

		fDefCpuLimit = config.optString ( kSetting_DefaultCpuLimit, null );
		fDefMemLimit = config.optString ( kSetting_DefaultMemLimit, null );
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
			final String k8sJobId = makeK8sName ( jobId );
			final String tag = k8sJobId + "-" + UniqueStringGenerator.createKeyUsingAlphabet ( jobId, "abcdefhigjklmnopqrstuvwxyz" );
			final Map<String,String> configFetchEnv = fConfigTransfer.deployConfiguration ( ds.getJob () );

			final String targetConfigFile = fConfigMountLoc + "/jobConfig.json";

			final JSONObject replacements = new JSONObject ()
				.put ( "CONFIG_URL", "${CONFIG_URL}" )	// this is to restore the evaluation text that's required during actual deployment
				.put ( "FC_DEPLOYMENT_NAME", tag )
				.put ( "FC_JOB_TAG", "job-" + tag )
				.put ( "FC_JOB_ID", jobId )
				.put ( "FC_INSTANCE_COUNT", "" + ds.getInstanceCount () )
				.put ( "FC_RUNTIME_IMAGE", fImageMapper.getImageName ( ds.getJob ().getRuntimeSpec () ) )
				.put ( "FC_CONFIG_MOUNT", fConfigMountLoc )
				.put ( "FC_CONFIG_FILE", targetConfigFile )
			;
//			replacements.put ( "FC_IMAGE_PULL_SECRET", "regcred" );

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
			try ( final InputStream deployTemplate = new ResourceLoader ()
				.usingStandardSources ( false, K8sController.this.getClass () )
				.named ( fInitYamlResource )
				.load ()
			)
			{
				if ( deployTemplate == null ) throw new ServiceException ( "Couldn't load " + fInitYamlResource );

				final String deployYaml = replaceAllTokens ( deployTemplate, replacements );
				final ByteArrayInputStream bais = new ByteArrayInputStream ( deployYaml.getBytes ( StandardCharsets.UTF_8 ) );

				dumpYaml ( tag, deployYaml );

				// build deployable item list
				final List<HasMetadata> items = fApiClient.load ( bais ).get ();

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
						final io.fabric8.kubernetes.api.model.apps.DeploymentSpec deploySpec = d.getSpec ();
						final PodTemplateSpec template = deploySpec.getTemplate ();
						final PodSpec ps = template.getSpec ();

						// apply any image pull secrets we have
						final List<LocalObjectReference> ipsList = new LinkedList<> ();
						for ( String ips : fImgPullSecrets )
						{
							ipsList.add ( new LocalObjectReference ( ips ) );
						}
						ps.setImagePullSecrets ( ipsList );

						// add env, secrets, and limits to the containers
						for ( Container c : ps.getContainers () )
						{
							pushEnvMapToContainer ( env, c );
							addSecretsToContainer ( secretsName, secrets, c );
							setLimitsOnContainer ( ds, c );
						}
						for ( Container c : ps.getInitContainers () )
						{
							pushEnvMapToContainer ( env, c );
							addSecretsToContainer ( secretsName, secrets, c );
							setLimitsOnContainer ( ds, c );
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
	private final String fConfigMountLoc;
	private final ContainerImageMapper fImageMapper;
	private final String fInitYamlResource;
	private final JSONObject fInitYamlSettings;
	private final List<String> fImgPullSecrets;
	private final boolean fDumpInitYaml;
	private final String fDefCpuLimit;
	private final String fDefMemLimit;

	private static String makeK8sName ( String from )
	{
		return "s-" + from.toLowerCase ();
	}
	
	private static class SimpleImageMapper implements ContainerImageMapper
	{
		@Override
		public String getImageName ( FlowControlRuntimeSpec rs )
		{
			return rs.getName () + ":" + rs.getVersion ();
		}
	}

	private void dumpYaml ( String tag, String deployYaml )
	{
		if ( fDumpInitYaml )
		{
			final File tmpDir = new File ( "/tmp/flowControlYamls" );
			tmpDir.mkdir ();
	
			final File yamlFile = new File ( tmpDir, tag + ".yaml" );
			try ( final FileWriter fw = new FileWriter ( yamlFile ) )
			{
				fw.write ( deployYaml );
			}
			catch ( IOException x )
			{
				log.warn ( "Couldn't write {}", yamlFile );
			}
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

	private void setLimitsOnContainer ( DeploymentSpec ds, Container c )
	{
		final HashMap<String, Quantity> map = new HashMap<> ();

		final String cpu = ds.getCpuLimitSpec ();
		if ( cpu != null )
		{
			map.put ( "cpu", new Quantity ( cpu ) );
		}

		final String mem = ds.getMemLimitSpec ();
		if ( mem != null )
		{
			map.put ( "memory", new Quantity ( mem ) );
		}

		if ( map.size () > 0 )
		{
			ResourceRequirements rr = c.getResources ();
			if ( rr == null )
			{
				rr = new ResourceRequirements ();
				c.setResources ( rr );
			}
			rr.setLimits ( map );
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
	private String replaceAllTokens ( InputStream src, JSONObject replacements ) throws IOException
	{
		final byte[] bytes = StreamTools.readBytes ( src );
		final String origText = new String ( bytes, StandardCharsets.UTF_8 );

		return ExpressionEvaluator.evaluateText ( origText,
			new JsonDataSource ( fInitYamlSettings ),
			new JsonDataSource ( replacements ),
			new EnvDataSource ()
		);
	}

	private class LocalDeploymentSpecBuilder implements DeploymentSpecBuilder
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
		public DeploymentSpecBuilder withCpuLimit ( String cpuLimit )
		{
			fCpuLimit = cpuLimit;
			return this;
		}

		@Override
		public DeploymentSpecBuilder withMemLimit ( String memLimit )
		{
			fMemLimit = memLimit;
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
		private String fCpuLimit = fDefCpuLimit;
		private String fMemLimit = fDefMemLimit;
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

		@Override
		public String getCpuLimitSpec () { return fBuilder.fCpuLimit; }

		@Override
		public String getMemLimitSpec () { return fBuilder.fMemLimit; }
	}

	private static final Logger log = LoggerFactory.getLogger ( K8sController.class );
}
