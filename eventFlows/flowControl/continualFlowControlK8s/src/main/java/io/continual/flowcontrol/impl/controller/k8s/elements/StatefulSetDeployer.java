package io.continual.flowcontrol.impl.controller.k8s.elements;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.controller.k8s.K8sElement;
import io.continual.flowcontrol.model.FlowControlResourceSpecs;
import io.continual.flowcontrol.model.FlowControlResourceSpecs.Toleration;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.QuantityFormatException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimBuilder;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1ResourceRequirementsBuilder;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetBuilder;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.kubernetes.client.openapi.models.V1TolerationBuilder;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.openapi.models.V1VolumeMountBuilder;

public class StatefulSetDeployer implements K8sElement
{
	public static final String kSetting_InitContainerImage = "initContainerImage";
	public static final String kDefault_InitContainerImage = "curlimages/curl:7.87.0";

	public static final String kSetting_ConfigMountPath = "configMountPath";
	public static final String kDefault_ConfigMountPath = "/var/flowcontrol/config";

	public static final String kSetting_DeploymentEnvSettings = "applyEnv";

	public static final String kSetting_PersistenceMountPath = "persistenceMountPath";
	public static final String kDefault_PersistenceMountPath = "/var/flowcontrol/persistence";
	public static final String kSetting_PersistenceSize = "persistenceStorageSize";
	public static final String kDefault_PersistenceSize = "8Gi";
	public static final String kSetting_PersistenceStorageClass = "persistenceStorageClass";
	public static final String kDefault_PersistenceStorageClass = "standard";

	public static final String kSetting_LoggingMountPath = "logsMountPath";
	public static final String kDefault_LoggingMountPath = "/var/flowcontrol/logs";
	public static final String kSetting_LoggingSize = "loggingStorageSize";
	public static final String kDefault_LoggingSize = "8Gi";
	public static final String kSetting_LoggingStorageClass = "loggingStorageClass";
	public static final String kDefault_LoggingStorageClass = "standard";

	public StatefulSetDeployer ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fInitContainerImage = config.optString ( kSetting_InitContainerImage, kDefault_InitContainerImage );
			fConfigMountPath = config.optString ( kSetting_ConfigMountPath, kDefault_ConfigMountPath );

			fDeploymentEnvSettings = JsonVisitor.objectToMap ( config.optJSONObject ( kSetting_DeploymentEnvSettings ) );

			fPersistenceMountPath = config.optString ( kSetting_PersistenceMountPath, kDefault_PersistenceMountPath );
			fPersistenceStorageSize = new Quantity ( config.optString ( kSetting_PersistenceSize, kDefault_PersistenceSize ) );
			fPersistenceStorageClass = config.optString ( kSetting_PersistenceStorageClass, kDefault_PersistenceStorageClass );
	
			fLoggingMountPath = config.optString ( kSetting_LoggingMountPath, kDefault_LoggingMountPath );
			fLoggingStorageSize = new Quantity ( config.optString ( kSetting_LoggingSize, kDefault_LoggingSize ) );
			fLoggingStorageClass = config.optString ( kSetting_LoggingStorageClass, kDefault_LoggingStorageClass );
		}
		catch ( QuantityFormatException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public void deploy ( K8sDeployContext ctx ) throws ElementDeployException
	{
		try
		{
			final String ssName = tagToStatefulSetName ( ctx.getDeployId () );
			final String secretConfigName = ctx.getWorkspace ().getString ( SecretDeployer.kWorkspaceKey_Secret );

			// build secrets
			final LinkedList<V1EnvVar> secretRefs = new LinkedList<> ();
			for ( String secretKey : JsonVisitor.arrayToList ( ctx.getWorkspace ().getJSONArray ( SecretDeployer.kWorkspaceKey_SecretKeys ) ) )
			{
				secretRefs.add ( new V1EnvVarBuilder()
					.withName ( secretKey )
					.withNewValueFrom ()
						.withNewSecretKeyRef ()
							.withKey ( secretKey )
							.withName ( secretConfigName )
						.endSecretKeyRef ()
					.endValueFrom ()
					.build ()
				);
			}

			// build image pull secrets
			final List<V1LocalObjectReference> ipsList = new LinkedList<> ();
			for ( String ips : ctx.getImagePullSecrets () )
			{
				ipsList.add ( new V1LocalObjectReference ().name ( ips ) );
				log.info ( "Registering image pull secret {}...", ips );
			}

			// build env
			final LinkedList<V1EnvVar> envs = new LinkedList<> ();
			for ( Map.Entry<String,String> envVal : ctx.getEnvironment ().entrySet () )
			{
				envs.add ( new V1EnvVarBuilder()
					.withName ( envVal.getKey () )
					.withValue ( envVal.getValue () )
					.build ()
				);
			}
			for ( Map.Entry<String,String> envVal : fDeploymentEnvSettings.entrySet () )
			{
				envs.add ( new V1EnvVarBuilder()
					.withName ( envVal.getKey () )
					.withValue ( envVal.getValue () )
					.build ()
				);
			}
			envs.add ( new V1EnvVarBuilder()
				.withName ( "FC_INSTALLATION_NAME" )
				.withValue ( ctx.getInstallationName () )
				.build ()
			);

			// setup resource specs
			final FlowControlResourceSpecs rs = ctx.getDeploymentSpec ().getResourceSpecs ();
			final V1ResourceRequirements resourceReqs = buildResourceReqs ( rs );

			// setup tolerations
			final LinkedList<V1Toleration> tols = new LinkedList<> ();
			for ( Toleration t : rs.tolerations () )
			{
				tols.add (
					new V1TolerationBuilder ()
						.withEffect ( t.effect () )
						.withKey ( t.key () )
						.withOperator ( t.operator () )
						.withTolerationSeconds ( t.seconds () )
						.withValue ( t.value () )
						.build ()
				);
			}

			
			// build the statefulset
			final V1StatefulSet ss = new V1StatefulSetBuilder ()
				.withNewMetadata ()
					.withName ( ssName )
					.addToLabels ( "app", ctx.getDeployId () )
					.addToLabels ( "flowcontroljob", ctx.getDeployId () )
				.endMetadata ()
				.withNewSpec ()
					.withServiceName ( ctx.getDeployId () )
					.withReplicas ( ctx.getDeploymentSpec ().getInstanceCount () )
					.withNewSelector ()
						.addToMatchLabels ( "app", ctx.getDeployId () )
					.endSelector ()
					.withNewTemplate ()
						.withNewMetadata ()
							.addToLabels ( "app", ctx.getDeployId () )
						.endMetadata ()
						.withNewSpec ()

							.withImagePullSecrets ( ipsList )

							.withTolerations ( tols )

							.withNewSecurityContext ()
								.withRunAsUser ( 1000L )
								.withRunAsGroup ( 3000L )
								.withFsGroup ( 2000L )
							.endSecurityContext ()

							.withVolumes (

								// configdisk emptydir
								new V1VolumeBuilder()
									.withName ( "configdisk" )
									.withNewEmptyDir ()
									.endEmptyDir ()
									.build (),

								// sysprep configmap mount
								new V1VolumeBuilder()
									.withName ( "sysprep" )
									.withNewConfigMap ()
										.withName ( ctx.getWorkspace().getString ( ConfigPullScriptDeployer.kWorkspaceKey_ConfigPullScriptConfigMap ) )
										.withDefaultMode ( 0755 )
									.endConfigMap ()
									.build ()
							)
							.withInitContainers (
								
								// initializer
								new V1ContainerBuilder ()
									.withName ( "initializer" )
									.withImage ( fInitContainerImage )
									.withVolumeMounts (

										// config disk
										new V1VolumeMountBuilder ()
											.withName ( "configdisk" )
											.withMountPath ( fConfigMountPath )
											.build (),

										// sysprep script
										new V1VolumeMountBuilder ()
											.withName ( "sysprep" )
											.withMountPath ( "/usr/local/bin" )
											.build ()
									)

									// environment
									.addAllToEnv ( secretRefs )
									.addAllToEnv ( envs )

									.withCommand ( "/usr/local/bin/sysprep" )
									.build ()
							)
							.withContainers (

								// runtime engine
								new V1ContainerBuilder ()
									.withName ( "processor" )
									.withImage ( ctx.getRuntimeImage () )
									.withVolumeMounts (
	
										// config disk
										new V1VolumeMountBuilder ()
											.withName ( "configdisk" )
											.withMountPath ( fConfigMountPath )
											.build (),
	
										// persistence disk
										new V1VolumeMountBuilder ()
											.withName ( "persistence" )
											.withMountPath ( fPersistenceMountPath )
											.build (),

										// logging disk
										new V1VolumeMountBuilder ()
											.withName ( "logging" )
											.withMountPath ( fLoggingMountPath )
											.build ()
									)

									.withResources ( resourceReqs )
									
									// environment
									.addAllToEnv ( secretRefs )
									.addAllToEnv ( envs )

									.build ()
							)
						.endSpec ()
					.endTemplate ()

					.withVolumeClaimTemplates (

						// persistence
						new V1PersistentVolumeClaimBuilder ()
							.withNewMetadata ()
								.withName ( "persistence" )
							.endMetadata ()
							.withNewSpec ()
								.withAccessModes ( "ReadWriteOnce" )
								.withNewResources ()
									.addToRequests ( "storage", fPersistenceStorageSize )
								.endResources ()
								.withStorageClassName ( fPersistenceStorageClass )
							.endSpec ()
							.build (),

						// logging
						new V1PersistentVolumeClaimBuilder ()
							.withNewMetadata ()
								.withName ( "logging" )
							.endMetadata ()
							.withNewSpec ()
								.withAccessModes ( "ReadWriteOnce" )
								.withNewResources ()
									.addToRequests ( "storage", fLoggingStorageSize )
								.endResources ()
								.withStorageClassName ( fLoggingStorageClass )
							.endSpec ()
							.build ()
					)

				.endSpec ()
				.build ()
			;

			// deploy the stateful set
			final AppsV1Api api = new AppsV1Api ();
			try
			{
				api.createNamespacedStatefulSet ( ctx.getNamespace (), ss ).execute ();
			}
			catch ( ApiException e )
			{
				if ( e.getCode () == HttpStatusCodes.k409_conflict )
				{
					// Conflict, the element already exists
					api.replaceNamespacedStatefulSet ( ssName, ctx.getNamespace (), ss ).execute ();
				}
				else
				{
					throw e;
				}
			}
			log.info ( "deployed stateful set [{}]", ssName );
		}
		catch ( ApiException x )
		{
			throw new ElementDeployException ( x );
		}
	}

	@Override
	public void undeploy ( String namespace, String deployId ) throws ElementDeployException
	{
		final String ssName = tagToStatefulSetName ( deployId );
		final AppsV1Api api = new AppsV1Api ();
		try
		{
			api.deleteNamespacedStatefulSet ( ssName, namespace ).execute ();
			log.info ( "Removed {}/{}", namespace, ssName );
		}
		catch ( ApiException x )
		{
			if ( x.getCode () == HttpStatusCodes.k404_notFound )
			{
				log.info ( "Element {} in {} did not exist.", ssName, namespace );
				return;
			}
			throw new ElementDeployException ( x );
		}
	}

	private static String tagToStatefulSetName ( String deployId )
	{
		return "s-" + deployId.trim ().toLowerCase ();
	}

	private V1ResourceRequirements buildResourceReqs ( FlowControlResourceSpecs rs )
	{
		final V1ResourceRequirementsBuilder resourceReqsBuilder = new V1ResourceRequirementsBuilder ();

		HashMap<String, Quantity> map = new HashMap<> ();

		final String mem = rs.memLimit ();
		if ( mem != null )
		{
			map.put ( "memory", new Quantity ( mem ) );
		}
		final String cpuReq = rs.cpuRequest ();
		if ( cpuReq != null )
		{
			map.put ( "cpu", new Quantity ( cpuReq ) );
		}
		if ( map.size () > 0 )
		{
			resourceReqsBuilder.withRequests ( map );
		}

		map.clear ();
		final String cpuLimit = rs.cpuLimit ();
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
			resourceReqsBuilder.withLimits ( map );
		}

		return resourceReqsBuilder.build ();
	}

	private final String fInitContainerImage;
	private final String fConfigMountPath;

	private final String fPersistenceMountPath;
	private final Quantity fPersistenceStorageSize;
	private final String fPersistenceStorageClass;

	private final String fLoggingMountPath;
	private final Quantity fLoggingStorageSize;
	private final String fLoggingStorageClass;

	private final Map<String,String> fDeploymentEnvSettings;

	private static final Logger log = LoggerFactory.getLogger ( StatefulSetDeployer.class );
}