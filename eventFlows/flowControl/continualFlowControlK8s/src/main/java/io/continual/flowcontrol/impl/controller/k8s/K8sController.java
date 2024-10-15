package io.continual.flowcontrol.impl.controller.k8s;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.introspector.PropertySubstitute;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.flowcontrol.impl.controller.k8s.K8sElement.ElementDeployException;
import io.continual.flowcontrol.impl.controller.k8s.K8sElement.K8sDeployContext;
import io.continual.flowcontrol.impl.controller.k8s.elements.SecretDeployer;
import io.continual.flowcontrol.impl.controller.k8s.impl.NoMapImageMapper;
import io.continual.flowcontrol.impl.deployer.BaseDeployer;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StringUtils;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

public class K8sController extends BaseDeployer
{
	static final String kSetting_ConfigMountLoc = "configMountLoc";
	static final String kDefault_ConfigMountLoc = "/var/flowcontrol/config";

	static final String kSetting_PersistMountLoc = "persistMountLoc";
	static final String kDefault_PersistMountLoc = "/var/flowcontrol/persistence";

	static final String kSetting_LogsMountLoc = "logsMountLoc";
	static final String kDefault_LogsMountLoc = "/var/flowcontrol/logs";

	static final String kSetting_InitYamlResource = "deploymentYaml";
	static final String kDefault_InitYamlResource = "initDeployment.yaml";

	static final String kSetting_ImagePullSecrets = "imagePullSecrets";

	static final String kSetting_InstallationName = "installationName";
	static final String kSetting_InternalConfigUrl = "internalConfigUrl";

	static final String kSetting_DumpInitYaml = "dumpInitYaml";

	static final String kSetting_Elements = "elements";
	
	static final String kSetting_EncryptorSvc = "encryptor";
	static final String kDefault_EncryptorSvc = "encryptor";

	static final String kSetting_ImageMapper = "imageMapper";

	static final String kSetting_UseKubeConfig = "useKubeConfig";
	static final boolean kDefault_UseKubeConfig = true;

	static final String kSetting_KubeConfigFile = "kubeConfig";
	static final String kDefault_KubeConfigFile = System.getenv("HOME") + "/.kube/config";

	static final String kSetting_k8sContext = "kubeConfigContext";
	static final String kSetting_K8sNamespace = "namespace";

	public K8sController ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		super ( sc, rawConfig );

		// evaluate the config in bulk...
		final JSONObject config = sc.getExprEval ().evaluateJsonObject ( rawConfig );

		// get k8s client config setup
		fK8sNamespace = config.getString ( kSetting_K8sNamespace );
		setupK8sConfig ( config, fK8sNamespace );

		// get the image mapper
		final JSONObject mapperSpec = config.optJSONObject ( kSetting_ImageMapper );
		if ( mapperSpec != null )
		{
			log.info ( "Building image mapper from {} setting.", kSetting_ImageMapper );
			fImageMapper = Builder.fromJson ( ContainerImageMapper.class, mapperSpec, sc );
		}
		else
		{
			log.info ( "Using default (name:version) image mapper" );
			fImageMapper = new NoMapImageMapper ();
		}

		// an encryption service
		fEncryptor = sc.getReqd ( config.optString ( kSetting_EncryptorSvc, kDefault_EncryptorSvc ), Encryptor.class );

		// kubernetes API settings
		fImgPullSecrets = JsonVisitor.arrayToList ( config.optJSONArray ( kSetting_ImagePullSecrets ) );

		//
		//	FIXME: the remaining settings feel like they should be in a list of arbitrary items that
		//	meet a container image spec for the system.  For example, why can't this system just dictate
		//	"here's where to put your logs?" why not always put them into /opt/logs, for example, and 
		//	mount a volume there (or don't)....  Or why does this system need to dictate to the processing
		//	engine container where to find its config? Why can't that image just deal with the URL 
		//	provided in any way it prefers?
		//

		// on-pod mount points
		fConfigMountLoc = config.optString ( kSetting_ConfigMountLoc, kDefault_ConfigMountLoc );
		fPersistMountLoc = config.optString ( kSetting_PersistMountLoc, kDefault_PersistMountLoc );
		fLogsMountLoc = config.optString ( kSetting_LogsMountLoc, kDefault_LogsMountLoc );

		fInstallationName = config.optString ( kSetting_InstallationName, "" );
		fInternalConfigBaseUrl = config.optString ( kSetting_InternalConfigUrl, "localhost:8080" );

		fElements = new LinkedList<> ();
		JsonVisitor.forEachElement ( config.optJSONArray ( kSetting_Elements ), new ArrayVisitor<JSONObject,BuildFailure> ()
		{
			@Override
			public boolean visit ( JSONObject element ) throws JSONException, BuildFailure
			{
				final K8sElement elementBuilder = Builder.withBaseClass ( K8sElement.class )
					.withClassNameInData ()
					.searchingPath ( SecretDeployer.class.getPackageName () )
					.usingData ( new BuilderJsonDataSource ( element ) )
					.build ()
				;
				fElements.add ( elementBuilder );
				return true;
			}
		} );
	}

	
	@Override
	protected FlowControlDeployment internalDeploy ( FlowControlCallContext ctx, FlowControlDeploymentSpec ds, String configKey ) throws ServiceException, RequestException
	{
		try
		{
			// setup job for transfer via config transfer service
			final String jobId = ds.getJob ().getId ();
			final String k8sDeployId = makeK8sName ( jobId );
			final String tag = k8sDeployId;

			// get the runtime spec 
			final FlowControlRuntimeSpec runtimeSpec = ds.getJob ().getRuntimeSpec ();
			if ( runtimeSpec == null ) throw new RequestException ( "There's no runtime spec on this job." );
			final String runtimeImage = fImageMapper.getImageName ( runtimeSpec );

			// build the container environment starting with what's in the spec from the user
			final HashMap<String,String> env = new HashMap<> ();

			// user settings
			env.putAll ( ds.getEnv () );

			// forced environment from flow control (after user settings so they're not changed)
			env.put ( "FC_DEPLOYMENT_NAME", k8sDeployId );
			env.put ( "FC_JOB_TAG", "job-" + k8sDeployId );
			env.put ( "FC_JOB_ID", jobId );
			env.put ( "FC_CONFIG_URL", configKeyToUrl ( configKey ) );
			env.put ( "FC_CONFIG_MOUNT", fConfigMountLoc );
			env.put ( "FC_CONFIG_FILE", fConfigMountLoc + "/jobConfig.json" );
			env.put ( "FC_PERSISTENCE_MOUNT", fPersistMountLoc );
			env.put ( "FC_LOGS_MOUNT", fLogsMountLoc );
			env.put ( "FC_RUNTIME_IMAGE", runtimeImage );

			// FIXME: temporarily while balancing container setup reqs for flowcontrol vs. general use container images
			env.put ( "EP_CMDLINE_ARGS", fConfigMountLoc + "/jobConfig.json"  );
			
			// builder workspace
			final JSONObject workspace = new JSONObject ();

			final K8sDeployContext deployContext = new K8sDeployContext ()
			{
				@Override
				public String getInstallationName () { return fInstallationName; }
				@Override
				public String getNamespace () { return fK8sNamespace; }
				@Override
				public String getDeployId () { return k8sDeployId; }
				@Override
				public FlowControlDeploymentSpec getDeploymentSpec () { return ds; }
				@Override
				public String getRuntimeImage () { return runtimeImage; }
				@Override
				public Encryptor getEncryptor () { return fEncryptor; }
				@Override
				public JSONObject getWorkspace () { return workspace; }
				@Override
				public Map<String, String> getEnvironment () { return env; }
				@Override
				public List<String> getImagePullSecrets () { return fImgPullSecrets; }
			};
			
			// build each element
			for ( K8sElement element : fElements )
			{
				element.deploy ( deployContext );
			}

			return new IntDeployment ( tag, ds, ctx.getUser (), configKey );
		}
		catch ( ElementDeployException x )
		{
			throw new ServiceException ( x );
		}
	}

	@Override
	protected void internalUndeploy ( FlowControlCallContext ctx, String deploymentId, FlowControlDeployment deployment ) throws ServiceException
	{
		// build elements
		try
		{
			final LinkedList<K8sElement> reversed = new LinkedList<> ( fElements );
			Collections.reverse ( reversed );
			for ( K8sElement element : reversed )
			{
				element.undeploy ( fK8sNamespace, deploymentId );
			}
		}
		catch ( ElementDeployException x )
		{
			throw new ServiceException ( x );
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
	private final Encryptor fEncryptor;

	private final String fK8sNamespace;
	private final String fConfigMountLoc;
	private final String fPersistMountLoc;
	private final String fLogsMountLoc;
	private final ContainerImageMapper fImageMapper;
	private final String fInstallationName;
	private final List<String> fImgPullSecrets;
	private final LinkedList<K8sElement> fElements;
	private final String fInternalConfigBaseUrl;

	private static final Logger log = LoggerFactory.getLogger ( K8sController.class );

	private String configKeyToUrl ( String configKey )
	{
		return StringUtils.appendIfMissing ( fInternalConfigBaseUrl, "/" )  + configKey;
	}

	private static String makeK8sName ( String from )
	{
		return from.toLowerCase ();
	}

/*
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
*/
	private class IntDeployment implements FlowControlDeployment
	{
		public IntDeployment ( String tag, FlowControlDeploymentSpec ds, Identity deployer, String configKey )
		{
			fTag = tag;
			fDeployer = deployer;
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
		private final Identity fDeployer;
		private final FlowControlDeploymentSpec fDeploymentSpec;
		private final String fConfigKey;
	}
/*
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
			final StatefulSetList ssl = fApiClient.apps().statefulSets ().inNamespace ( fK8sNamespace ).list ();
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
			final DeploymentList dl = fApiClient.apps().deployments ().inNamespace ( fK8sNamespace ).list ();
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
		for ( Deployment d : fApiClient.apps().deployments().inNamespace ( fK8sNamespace ).list ().getItems () )
		{
			result.add ( new K8sDeployWrapper ( d ) );
		}
		for ( StatefulSet d : fApiClient.apps().statefulSets ().inNamespace ( fK8sNamespace ).list ().getItems () )
		{
			result.add ( new K8sDeployWrapper ( d ) );
		}
		return result;
	}

	private List<Pod> getPodsFor ( String tag )
	{
		final PodList pl = fApiClient
			.pods ()
			.inNamespace ( fK8sNamespace )
			.withLabel ( "app", "job-" + tag )

			.list ()
		;
		return pl.getItems ();
	}

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
*/
	private void setupK8sConfig ( JSONObject config, String namespace ) throws BuildFailure
	{
		// We can run in-cluster or via kube config file. Adapted from https://github.com/kubernetes-client/java/wiki/3.-Code-Examples, Oct 2024 
		try
		{
			final ApiClient client;

			final boolean useKubeConfig = config.optBoolean ( kSetting_UseKubeConfig, kDefault_UseKubeConfig );
			final String kubeConfigFile = config.optString ( kSetting_KubeConfigFile, kDefault_KubeConfigFile );

			if ( useKubeConfig || ( config.has ( kSetting_KubeConfigFile ) && StringUtils.isNotEmpty ( kubeConfigFile ) ) )
			{
				log.info ( "Building k8s API config from kube config [" + kubeConfigFile + "]" );

				// user has asked for kube config read either via use-kube-config boolean or by setting a
				// non-empty filename
				final KubeConfig kc = KubeConfig.loadKubeConfig ( new FileReader ( kubeConfigFile ) );
				
				// get the k8s context name
				final String contextName = config.optString ( kSetting_k8sContext, null );
				if ( StringUtils.isNotEmpty ( contextName ) )
				{
					log.info ( "Using kubectl context [{}]", contextName );
					kc.setContext ( contextName );
				}
				else
				{
					log.warn ( "🤔 Using kubectl's current context. (It's a good idea to explicitly configure '{}'.)", kSetting_k8sContext );
				}

				// build
				client = ClientBuilder
					.kubeconfig ( kc )
					.build ()
				;
			}
			else
			{
				log.info ( "Building k8s API config from in-cluster service account data" );

				// use service account setup
				client = ClientBuilder.cluster ().build ();

			    // if you prefer not to refresh service account token, please use:
			    // ApiClient client = ClientBuilder.oldCluster().build();
			}

		    // set the global default api-client to the in-cluster one from above
			Configuration.setDefaultApiClient ( client );

			// test connectivity by checking for the assigned namespace
			final CoreV1Api api = new CoreV1Api ();
			if ( 0 == api
				.listNamespace ()
				.labelSelector ( "kubernetes.io/metadata.name=" + namespace )
				.execute ()
				.getItems ()
				.size () 
			)
			{
				throw new BuildFailure ( "Namespace [" + namespace + "] is not available." );
			}
			log.info ( "Connected to Kubernetes and found namespace [" + namespace + "]." );
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}
		catch ( ApiException x )
		{
			throw new BuildFailure ( x );
		}
	}

	// see https://github.com/kubernetes-client/java/issues/2741 (this doesn't seem to be helping any)
	static
	{
		java.util.logging.Logger snakeLog = java.util.logging.Logger.getLogger ( PropertySubstitute.class.getPackage().getName() );
		snakeLog.setLevel ( java.util.logging.Level.SEVERE );
		snakeLog = java.util.logging.Logger.getLogger ( "org.yaml.snakeyaml.introspector" );
		snakeLog.setLevel ( java.util.logging.Level.SEVERE );
	}
}
