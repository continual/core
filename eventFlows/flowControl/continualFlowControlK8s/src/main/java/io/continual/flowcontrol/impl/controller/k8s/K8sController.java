
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
import io.continual.flowcontrol.impl.controller.k8s.FlowControlK8sElement.ElementDeployException;
import io.continual.flowcontrol.impl.controller.k8s.FlowControlK8sElement.ImagePullPolicy;
import io.continual.flowcontrol.impl.controller.k8s.FlowControlK8sElement.K8sDeployContext;
import io.continual.flowcontrol.impl.controller.k8s.FlowControlK8sElement.K8sInstallationContext;
import io.continual.flowcontrol.impl.controller.k8s.elements.SecretDeployer;
import io.continual.flowcontrol.impl.controller.k8s.impl.ContainerImageMapper;
import io.continual.flowcontrol.impl.controller.k8s.impl.NoMapImageMapper;
import io.continual.flowcontrol.impl.deployer.BaseDeployer;
import io.continual.flowcontrol.model.Encryptor;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeploymentRecord;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.model.FlowControlRuntimeState;
import io.continual.flowcontrol.model.FlowControlRuntimeSystem;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StringUtils;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

/**
 * This kubernetes controller is both a "deployer" and a "runtime system".
 */
public class K8sController extends BaseDeployer implements FlowControlRuntimeSystem
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
				final FlowControlK8sElement elementBuilder = Builder.withBaseClass ( FlowControlK8sElement.class )
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
	protected FlowControlDeploymentRecord internalDeploy ( FlowControlCallContext ctx, FlowControlDeploymentSpec ds, String configKey ) throws ServiceException, RequestException
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

				@Override
				public ImagePullPolicy getImagePullPolicy ()
				{
					// decide on an image pull policy. Normally we'd use "if not present", but we'll automatically
					// switch to "always" if we're using a snapshot image (FIXME: also should allow user control here)
					ImagePullPolicy ipp = ImagePullPolicy.IfNotPresent;
					if ( runtimeImage.endsWith ( "-SNAPSHOT" ) )
					{
						ipp = ImagePullPolicy.Always;
					}
					return ipp;
				}
			};

			// build each element
			for ( FlowControlK8sElement element : fElements )
			{
				log.info ( "deploying k8s element {}", element );
				element.deploy ( deployContext );
			}
			log.info ( "all k8s elements deployed" );

			return new IntDeployment ( tag, AccessControlList.createOpenAcl (), ds, ctx.getUser (), configKey );
		}
		catch ( ElementDeployException x )
		{
			throw new ServiceException ( x );
		}
	}

	private K8sInstallationContext makeInstallContext ( String deploymentId )
	{
		return new K8sInstallationContext ()
		{
			@Override
			public String getInstallationName () { return fInstallationName; }
			@Override
			public String getNamespace () { return fK8sNamespace; }
			@Override
			public String getDeployId () { return deploymentId; }
		};
	}

	@Override
	protected void internalUndeploy ( FlowControlCallContext ctx, String deploymentId, FlowControlDeploymentRecord deployment ) throws ServiceException
	{
		// build elements
		try
		{
			final K8sInstallationContext installationContext = makeInstallContext ( deploymentId );
			final LinkedList<FlowControlK8sElement> reversed = new LinkedList<> ( fElements );
			Collections.reverse ( reversed );
			for ( FlowControlK8sElement element : reversed )
			{
				element.undeploy ( installationContext );
			}
		}
		catch ( ElementDeployException x )
		{
			throw new ServiceException ( x );
		}
	}

	/**
	 * Get the running process information
	 * @param fccc the call context
	 * @param deploymentId the ID of a deployment
	 * @return a runtime state
	 */
	@Override
	public FlowControlRuntimeState getRuntimeState ( FlowControlCallContext fccc, String deploymentId ) throws ServiceException
	{
		// find the runtime element and return its state
		for ( FlowControlK8sElement e : fElements )
		{
			if ( e.isRuntimeProvider () )
			{
				try
				{
					final K8sInstallationContext installationContext = makeInstallContext ( deploymentId );
					if ( !e.isDeployed ( installationContext ) )
					{
						return null;
					}
					return e.getRuntimeState ( installationContext );
				}
				catch ( ElementDeployException x )
				{
					throw new ServiceException ( x );
				}
			}
		}

		throw new ServiceException ( "No Kubernetes deployment elements identify as providing a runtime environment. (This is a service configuration error.)" );
	}

	private final Encryptor fEncryptor;

	private final String fK8sNamespace;
	private final String fConfigMountLoc;
	private final String fPersistMountLoc;
	private final String fLogsMountLoc;
	private final ContainerImageMapper fImageMapper;
	private final String fInstallationName;
	private final List<String> fImgPullSecrets;
	private final LinkedList<FlowControlK8sElement> fElements;
	private final String fInternalConfigBaseUrl;

	private static final Logger log = LoggerFactory.getLogger ( K8sController.class );

	private String configKeyToUrl ( String configKey )
	{
		return StringUtils.appendIfMissing ( fInternalConfigBaseUrl, "/" ) + "config/"  + configKey;
	}

	private static String makeK8sName ( String from )
	{
		return from.toLowerCase ();
	}

	private class IntDeployment implements FlowControlDeploymentRecord
	{
		public IntDeployment ( String tag, AccessControlList acl, FlowControlDeploymentSpec ds, Identity deployer, String configKey )
		{
			fTag = tag;
			fAcl = acl;
			fDeployer = deployer;
			fDeploymentSpec = ds;
			fConfigKey = configKey;
		}

		@Override
		public String getId () { return fTag; }

		@Override
		public AccessControlList getAccessControlList () { return fAcl; }

		@Override
		public FlowControlDeploymentSpec getDeploymentSpec () { return fDeploymentSpec; }

		@Override
		public Identity getDeployer () { return fDeployer; }

		@Override
		public String getConfigToken () { return fConfigKey; }

		private final String fTag;
		private final AccessControlList fAcl;
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
				try
				{
					client = ClientBuilder
						.kubeconfig ( kc )
						.build ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					throw new BuildFailure ( x );
				}
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
//			final CoreV1Api api = new CoreV1Api ();
//			if ( 0 == api
//				.listNamespace ()
//				.labelSelector ( "kubernetes.io/metadata.name=" + namespace )
//				.execute ()
//				.getItems ()
//				.size () 
//			)
//			{
//				throw new BuildFailure ( "Namespace [" + namespace + "] is not available." );
//			}
//			log.info ( "Connected to Kubernetes and found namespace [" + namespace + "]." );
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}
//		catch ( ApiException x )
//		{
//			throw new BuildFailure ( x );
//		}
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
