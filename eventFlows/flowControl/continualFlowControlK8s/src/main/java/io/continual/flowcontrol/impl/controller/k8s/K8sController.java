package io.continual.flowcontrol.impl.controller.k8s;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.deployer.BaseDeployer;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.model.FlowControlResourceSpecs;
import io.continual.flowcontrol.model.FlowControlResourceSpecs.Toleration;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService;
import io.continual.flowcontrol.services.deployer.FlowControlRuntimeState;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.identity.Identity;
import io.continual.resources.ResourceLoader;
import io.continual.services.ServiceContainer;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateEngine.TemplateParseException;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.templating.impl.dollarEval.DollarEvalTemplateEngine;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
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
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetList;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.PodResource;

public class K8sController extends BaseDeployer
{
	static final String kSetting_k8sContext = "context";
	static final String kSetting_Namespace = "namespace";

	static final String kSetting_ConfigMountLoc = "configMountLoc";
	static final String kDefault_ConfigMountLoc = "/var/flowcontrol/config";

	static final String kSetting_PersistMountLoc = "persistMountLoc";
	static final String kDefault_PersistMountLoc = "/var/flowcontrol/persistence";

	static final String kSetting_LogsMountLoc = "logsMountLoc";
	static final String kDefault_LogsMountLoc = "/var/flowcontrol/logs";

	static final String kSetting_ConfigTransfer = "configTransfer";

	static final String kSetting_InitYamlResource = "deploymentYaml";
	static final String kDefault_InitYamlResource = "initDeployment.yaml";

	static final String kSetting_InitYamlSettings = "deploymentSettings";

	static final String kSetting_InitYamlImagePullSecrets = "imagePullSecrets";

	static final String kSetting_StorageClass = "storageClass";
	static final String kDefault_StorageClass = "standard";

	static final String kSetting_InstallationName = "installationName";

	static final String kSetting_DumpInitYaml = "dumpInitYaml";

	static final String kSetting_DeploySpecCtxPop = "deploymentSpecToContext";
	static final String kSetting_TemplateEngine = "templateEngine";

	static final String kSetting_EncryptorSvc = "encryptor";
	static final String kDefault_EncryptorSvc = "encryptor";

	public K8sController ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		super ( sc, rawConfig );

		final JSONObject config = sc.getExprEval ().evaluateJsonObject ( rawConfig );

		final String contextName = config.optString ( kSetting_k8sContext, null );
		if ( contextName != null && contextName.length () > 0 )
		{
			final Config cfgWithContext = Config.autoConfigure ( contextName );
			fApiClient = new KubernetesClientBuilder().withConfig ( cfgWithContext ).build ();
		}
		else
		{
			fApiClient = new KubernetesClientBuilder().build();
		}
		fNamespace = config.getString ( kSetting_Namespace );
		fConfigMountLoc = config.optString ( kSetting_ConfigMountLoc, kDefault_ConfigMountLoc );
		fPersistMountLoc = config.optString ( kSetting_PersistMountLoc, kDefault_PersistMountLoc );
		fLogsMountLoc = config.optString ( kSetting_LogsMountLoc, kDefault_LogsMountLoc );

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
		fInitYamlStorageClass= config.optString ( kSetting_StorageClass, kDefault_StorageClass );

		fInstallationName = config.optString ( kSetting_InstallationName, "" );

		fImgPullSecrets = JsonVisitor.arrayToList ( config.optJSONArray ( kSetting_InitYamlImagePullSecrets ) );
		fDumpInitYaml = config.optBoolean ( kSetting_DumpInitYaml, false );

		final String deploySpecCtxPopSpec = config.optString ( kSetting_DeploySpecCtxPop, null );
		if ( deploySpecCtxPopSpec != null )
		{
			fDeploySpecPopulator = sc.getReqd ( deploySpecCtxPopSpec, DeploySpecTranslator.class );
		}
		else
		{
			log.info ( "No deployment spec to context translator specified; defaulting to {}.", StdDeploySpecTranslator.class.getSimpleName () );
			fDeploySpecPopulator = new StdDeploySpecTranslator ( sc, new JSONObject () );
		}
		
		final String templateEngineSpec = config.optString ( kSetting_TemplateEngine, null );
		if ( templateEngineSpec != null )
		{
			fTemplateEngine = sc.getReqd ( templateEngineSpec, ContinualTemplateEngine.class );
		}
		else
		{
			log.info ( "No templating engine specified; defaulting to ${} evals." );
			fTemplateEngine = new DollarEvalTemplateEngine ( sc, new JSONObject () );
		}

		fEncryptor = sc.getReqd ( config.optString ( kSetting_EncryptorSvc, kDefault_EncryptorSvc ), Encryptor.class );
	}

	@Override
	protected void onStopRequested ()
	{
		super.onStopRequested ();

		fApiClient.close ();
	}

	@Override
	protected FlowControlDeployment internalDeploy ( FlowControlCallContext ctx, FlowControlDeploymentSpec ds, String configKey ) throws ServiceException, RequestException
	{
		try
		{
			// setup job for transfer via config transfer service
			final String jobId = ds.getJob ().getId ();
			final String k8sJobId = makeK8sName ( jobId );
			final String tag = k8sJobId;

			final String targetConfigFile = fConfigMountLoc + "/jobConfig.json";

			// place any secrets from this job
			final String secretsName = tagToSecret ( tag );

			// start a secret for this job's secret data
			final Map<String,String> secrets = ds.getJob ().getSecrets ( fEncryptor );
			SecretBuilder sb = new SecretBuilder ()
				.withType ( "Opaque" )
				.withNewMetadata ()
					.withName ( secretsName )
				.endMetadata ()
			;

			// iterate through registered secrets and add them to the secret builder
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
				fApiClient.secrets ().inNamespace ( fNamespace ).resource ( sb.build () ).serverSideApply ();
			}

			// Get deployment installed.  
			try ( final InputStream deployTemplate = new ResourceLoader ()
				.usingStandardSources ( false, K8sController.class )
				.named ( fInitYamlResource )
				.load ()
			)
			{
				if ( deployTemplate == null ) throw new ServiceException ( "Couldn't load " + fInitYamlResource );

				// get a template context and initialize it with basic information
				final ContinualTemplateContext templateCtx = fTemplateEngine.createContext ();
				templateCtx
					.putAll ( System.getenv () )
					.put ( "CONFIG_URL", "${CONFIG_URL}" )	// this is to restore the evaluation text that's required during actual deployment
					.put ( "FC_DEPLOYMENT_NAME", tag )
					.put ( "FC_JOB_TAG", "job-" + tag )
					.put ( "FC_JOB_ID", jobId )
					.put ( "FC_CONFIG_MOUNT", fConfigMountLoc )
					.put ( "FC_PERSISTENCE_MOUNT", fPersistMountLoc )
					.put ( "FC_LOGS_MOUNT", fLogsMountLoc )
					.put ( "FC_CONFIG_FILE", targetConfigFile )
					.put ( "FC_RUNTIME_IMAGE", fImageMapper.getImageName ( ds.getJob ().getRuntimeSpec () ) )
					.put ( "FC_STORAGE_CLASS", fInitYamlStorageClass )
				;

				// allow our deployment spec populator to add to the context
				fDeploySpecPopulator.populate ( ds, templateCtx );

				// add init yaml settings (these are fixed)
				templateCtx.putAll ( JsonVisitor.objectToMap ( fInitYamlSettings ) );

				// expand the template into our output stream
				final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
				fTemplateEngine.renderTemplate ( ContinualTemplateSource.fromInputStream ( deployTemplate ), templateCtx, baos );
				final String deployYaml = new String ( baos.toByteArray (), StandardCharsets.UTF_8 );
				final ByteArrayInputStream bais = new ByteArrayInputStream ( deployYaml.getBytes ( StandardCharsets.UTF_8 ) );

				dumpYaml ( tag, deployYaml );

				// build deployable item list
				final List<HasMetadata> items = fApiClient.load ( bais ).get ();

				// push environment
				final HashMap<String,String> env = new HashMap<String,String> ();
				env.putAll ( ds.getEnv () );
				env.put ( "CONFIG_URL", configKeyToUrl ( configKey ) );
				env.put ( "FC_INSTALLATION_NAME", fInstallationName );
				env.put ( "FC_CONFIG_DIR", fConfigMountLoc );
				env.put ( "FC_PERSISTENCE_DIR", fPersistMountLoc );
				env.put ( "FC_LOGS_DIR", fLogsMountLoc );
				env.put ( "JOB_ID", jobId );
				env.put ( "CONFIG_FILE", targetConfigFile );
				updateEnv ( env );

				for ( HasMetadata md : items )
				{
					final String kind = md.getKind ();
					final String name = md.getMetadata ().getName ();
					log.info ( "Manifest includes {} {}.", kind, name );

					PodTemplateSpec template = null;
					if ( kind.equals ( "Deployment" ) )
					{
						final Deployment d = (Deployment) md;
						final io.fabric8.kubernetes.api.model.apps.DeploymentSpec deploySpec = d.getSpec ();
						template = deploySpec.getTemplate ();
					}
					else if ( kind.equals ( "StatefulSet" ) )
					{
						final StatefulSet ss = (StatefulSet) md;
						final StatefulSetSpec sss = ss.getSpec ();
						template = sss.getTemplate ();
					}
					if ( template != null )
					{
						updateTemplate ( ds, template, env, secretsName, secrets );
					}
				}

				fApiClient
					.resourceList ( items )
					.inNamespace ( fNamespace )
					.serverSideApply ()
				;
			}
			catch ( IOException | TemplateNotFoundException | TemplateParseException e )
			{
				throw new ServiceException ( e );
			}
			catch ( KubernetesClientException x )
			{
				mapException ( x );
			}
	
			return new IntDeployment ( tag, jobId, ds, ctx.getUser (), configKey );
		}
		catch ( GeneralSecurityException x )
		{
			throw new ServiceException ( x );
		}
	}

	private String configKeyToUrl ( String configKey )
	{
		return "http://foo.bar/"  + configKey;
	}
	
	@Override
	protected void internalUndeploy ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		final K8sDeployWrapper dw = getDeployment ( deploymentId );
		if ( dw != null )
		{
			dw.delete ();
		
			try
			{
				final Secret secret = fApiClient
					.secrets ()
					.inNamespace ( fNamespace )
					.withName ( tagToSecret ( deploymentId ) )
					.get ()
				;
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
	}
/*
	@Override
	public FlowControlDeployment getDeployment ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException
	{
		try
		{
			final K8sDeployWrapper dw = getDeployment ( deploymentId );
			if ( dw == null ) return null;
			
			return new IntDeployment ( dw.getMetadata ().getName (), getJobIdFrom ( dw, "(unknown)" ) );
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
			for ( K8sDeployWrapper dw : getK8sDeployments () )
			{
				result.add ( new IntDeployment ( dw.getMetadata ().getName (), getJobIdFrom ( dw, "(unknown)" ) ) );
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
			for ( K8sDeployWrapper dw : getK8sDeployments () )
			{
				final String thisJobId = getJobIdFrom ( dw, null );
				if ( jobId.equals ( thisJobId ) )
				{
					result.add ( new IntDeployment ( dw.getMetadata ().getName (), thisJobId ) );
				}
			}
		}
		catch ( KubernetesClientException x )
		{
			mapExceptionSvcOnly ( x );
		}
		return result;
	}
*/
	private final ContinualTemplateEngine fTemplateEngine;
	private final Encryptor fEncryptor;

	private final KubernetesClient fApiClient;
	private final String fNamespace;
	private final String fConfigMountLoc;
	private final String fPersistMountLoc;
	private final String fLogsMountLoc;
	private final ContainerImageMapper fImageMapper;
	private final String fInitYamlResource;
	private final DeploySpecTranslator fDeploySpecPopulator;
	private final JSONObject fInitYamlSettings;
	private final String fInitYamlStorageClass;
	private final String fInstallationName;
	private final List<String> fImgPullSecrets;
	private final boolean fDumpInitYaml;

	protected void updateEnv ( HashMap<String,String> env )
	{
	}

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

	private void updateTemplate ( FlowControlDeploymentSpec ds, PodTemplateSpec template, HashMap<String,String> env, String secretsName, Map<String,String> secrets )
	{
		final PodSpec ps = template.getSpec ();

		// apply any image pull secrets we have
		final List<LocalObjectReference> ipsList = new LinkedList<> ();
		for ( String ips : fImgPullSecrets )
		{
			ipsList.add ( new LocalObjectReference ( ips ) );
			log.info ( "Registering image pull secret {}...", ips );
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

		// apply tolerations
		final LinkedList<io.fabric8.kubernetes.api.model.Toleration> tols = new LinkedList<> ();
		for ( Toleration t : ds.getResourceSpecs ().tolerations () )
		{
			tols.add ( new io.fabric8.kubernetes.api.model.Toleration ( t.effect (), t.key (), t.operator (), t.seconds (), t.value () ) );
		}
		if ( tols.size () > 0 )
		{
			ps.setTolerations ( tols );
		}
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

	private void setLimitsOnContainer ( FlowControlDeploymentSpec ds, Container c )
	{
		final FlowControlResourceSpecs rs = ds.getResourceSpecs ();
		final String cpuReq = rs.cpuRequest ();
		final String cpuLimit = rs.cpuLimit ();
		final String mem = rs.memLimit ();

		HashMap<String, Quantity> map = new HashMap<> ();
		if ( mem != null )
		{
			map.put ( "memory", new Quantity ( mem ) );
		}
		if ( cpuReq != null )
		{
			map.put ( "cpu", new Quantity ( cpuReq ) );
		}
		if ( map.size () > 0 )
		{
			ResourceRequirements rr = c.getResources ();
			if ( rr == null )
			{
				rr = new ResourceRequirements ();
				c.setResources ( rr );
			}
			rr.setRequests ( map );
		}

		map = new HashMap<> ();
		if ( mem != null )
		{
			map.put ( "memory", new Quantity ( mem ) );
		}
		if ( cpuLimit != null )
		{
			map.put ( "cpu", new Quantity ( cpuLimit ) );
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

	private class K8sDeployWrapper
	{
		public K8sDeployWrapper ( Deployment d ) { fDeployment = d; fStatefulSet = null; }
		public K8sDeployWrapper ( StatefulSet ss ) { fDeployment = null; fStatefulSet = ss; }

		public io.fabric8.kubernetes.api.model.ObjectMeta getMetadata ()
		{
			if ( fDeployment != null ) return fDeployment.getMetadata (); 
			if ( fStatefulSet != null ) return fStatefulSet.getMetadata ();
			return null;
		}

		public void delete ()
		{
			if ( fDeployment != null ) fApiClient.resource ( fDeployment ).delete (); 
			if ( fStatefulSet != null ) fApiClient.resource ( fStatefulSet ).delete ();
		}

		public FlowControlRuntimeState.DeploymentStatus getStatus ()
		{
			if ( fDeployment != null )
			{
				boolean progressing = false;
				boolean available = false;
				
				final DeploymentStatus ds = fDeployment.getStatus ();
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
					return FlowControlRuntimeState.DeploymentStatus.RUNNING;
				}
				else if ( progressing && !available )
				{
					return FlowControlRuntimeState.DeploymentStatus.PENDING;
				}
			}
			else if ( fStatefulSet != null )
			{
				final int replReqd = safeInt ( fStatefulSet.getSpec ().getReplicas () );
				final StatefulSetStatus sss = fStatefulSet.getStatus ();
				final int ready = safeInt ( sss.getReadyReplicas () );
				final int repls = safeInt ( sss.getReplicas () );

				log.info ( "Sts {}: {} reqd, {} created, {} ready", fStatefulSet.getMetadata ().getName (), replReqd, repls, ready );

				if ( ready < replReqd )
				{
					return FlowControlRuntimeState.DeploymentStatus.PENDING;
				}
				else if ( ready == replReqd )
				{
					return FlowControlRuntimeState.DeploymentStatus.RUNNING;
				}
			}
			return FlowControlRuntimeState.DeploymentStatus.UNKNOWN;
		}

		public int getReplicaCount ()
		{
			if ( fDeployment != null )
			{
				final DeploymentStatus ds = fDeployment.getStatus ();
				return ds.getReplicas ();
			}
			return -1;
		}
		
		private final Deployment fDeployment;
		private final StatefulSet fStatefulSet;
	}
	
	private class IntDeployment implements FlowControlDeployment
	{
		public IntDeployment ( String tag, String jobId, FlowControlDeploymentSpec ds, Identity deployer, String configKey )
		{
			fTag = tag;
			fDeployer = deployer;
			fJobId = jobId;
			fDeploymentSpec = ds;
			fConfigKey = configKey;
		}

		@Override
		public String getId () { return fTag; }

		@Override
		public FlowControlDeploymentSpec getDeploymentSpec () { return fDeploymentSpec; }

		@Override
		public Identity getDeployer () { return fDeployer; }

		@Override
		public String getConfigToken () { return fConfigKey; }

		private final String fTag;
		private final String fJobId;
		private final Identity fDeployer;
		private final FlowControlDeploymentSpec fDeploymentSpec;
		private final String fConfigKey;
	}

	private List<String> getLogFor ( PodResource pod, String sinceRfc3339Time )
	{
		final LinkedList<String> result = new LinkedList<> ();

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

		return result;
	}
	
	private K8sDeployWrapper getDeployment ( String tag )
	{
		// First look for a stateful set. Issuing a call for a named item as a deployment and then
		// as a stateful set (since we don't know which was used in the init yaml) seemed to cause trouble
		// either for the fabric8 client or for the service, with the 2nd call throwing "not found" so instead
		// we're just grabbing the list and searching locally, and also running the stateful set search first
		// since it's our normal.

		try
		{
			// get the stateful set list
			final StatefulSetList ssl = fApiClient.apps().statefulSets ().inNamespace ( fNamespace ).list ();
			for ( StatefulSet ss : ssl.getItems () )
			{
				if ( ss.getMetadata ().getName ().equals ( tag ) )
				{
					return new K8sDeployWrapper ( ss );
				}
			}
		}
		catch ( KubernetesClientException | IllegalStateException x )
		{
			// spec says object should be null if it doesn't exist, but testing implies this exception is thrown instead
			log.warn ( x.getMessage () );
		}

		try
		{
			// now try deployment list
			final DeploymentList dl = fApiClient.apps().deployments ().inNamespace ( fNamespace ).list ();
			for ( Deployment d : dl.getItems () )
			{
				if ( d.getMetadata ().getName ().equals ( tag ) )
				{
					return new K8sDeployWrapper ( d );
				}
			}
		}
		catch ( KubernetesClientException | IllegalStateException x )
		{
			// spec says object should be null if it doesn't exist, but testing implies this exception is thrown instead
			log.warn ( x.getMessage () );
		}

		return null;
	}

	private List<K8sDeployWrapper> getK8sDeployments ( )
	{
		final LinkedList<K8sDeployWrapper> result = new LinkedList<> ();
		for ( Deployment d : fApiClient.apps().deployments().inNamespace ( fNamespace ).list ().getItems () )
		{
			result.add ( new K8sDeployWrapper ( d ) );
		}
		for ( StatefulSet d : fApiClient.apps().statefulSets ().inNamespace ( fNamespace ).list ().getItems () )
		{
			result.add ( new K8sDeployWrapper ( d ) );
		}
		return result;
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

	private static final Logger log = LoggerFactory.getLogger ( K8sController.class );

	private static int safeInt ( Integer i )
	{
		return i == null ? 0 : i;
	}

	private static String getJobIdFrom ( K8sDeployWrapper dw, String defval )
	{
		// make no assumptions about the existence of structures here!
		if ( dw != null )
		{
			final ObjectMeta om = dw.getMetadata ();
			if ( om != null )
			{
				final Map<String,String> labels = om.getLabels ();
				if ( labels != null )
				{
					final String jobId = labels.get ( "flowcontroljob" );
					if ( jobId != null )
					{
						return jobId;
					}
				}
			}
		}
		return defval;
	}
}
