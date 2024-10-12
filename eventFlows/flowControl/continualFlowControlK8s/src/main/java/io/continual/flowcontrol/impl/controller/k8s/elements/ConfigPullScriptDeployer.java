package io.continual.flowcontrol.impl.controller.k8s.elements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.flowcontrol.impl.controller.k8s.K8sElement;
import io.continual.resources.ResourceLoader;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapBuilder;

public class ConfigPullScriptDeployer implements K8sElement
{
	public static final String kWorkspaceKey_ConfigPullScriptConfigMap = "configPullConfigMap";
	public static final String kWorkspaceKey_ConfigPullScriptName = "configfPullScriptName";
	public static final String kWorkspaceVal_ConfigPullScriptName = "sysprep";

	public ConfigPullScriptDeployer ( JSONObject config )
	{
		fConfigMapScriptResource = config.getString ( "scriptResource" );
	}

	@Override
	public void deploy ( K8sDeployContext ctx ) throws ElementDeployException
	{
		try
		{
			final String configMapName = tagToConfigPullScriptConfigMap ( ctx.getDeployId () );

			final HashMap<String,String> dataMap = new HashMap<> ();
			dataMap.put ( kWorkspaceVal_ConfigPullScriptName, loadResource ( fConfigMapScriptResource ) );

			final V1ConfigMap cm = new V1ConfigMapBuilder ()
				.withKind ( "ConfigMap" )
				.withNewMetadata ()
					.withName ( configMapName )
				.endMetadata ()
				.withData ( dataMap )
				.build ()
			;

			// deploy the config map
			final CoreV1Api api = new CoreV1Api ();
			try
			{
				api.createNamespacedConfigMap ( ctx.getNamespace (), cm ).execute ();
			}
			catch ( ApiException e )
			{
				if ( e.getCode () == HttpStatusCodes.k409_conflict )
				{
					// Conflict, the element already exists
					api.replaceNamespacedConfigMap ( configMapName, ctx.getNamespace (), cm ).execute ();
				}
				else
				{
					throw e;
				}
			}
			log.info ( "deployed config map [{}]", configMapName );

			ctx.getWorkspace ().put ( kWorkspaceKey_ConfigPullScriptConfigMap, configMapName );
			ctx.getWorkspace ().put ( kWorkspaceKey_ConfigPullScriptName, kWorkspaceVal_ConfigPullScriptName );
		}
		catch ( ApiException x )
		{
			throw new ElementDeployException ( x );
		}
	}

	@Override
	public void undeploy ( String namespace, String deployId ) throws ElementDeployException
	{
		final String configMapName = tagToConfigPullScriptConfigMap ( deployId );
		final CoreV1Api api = new CoreV1Api ();
		try
		{
			api.deleteNamespacedConfigMap ( configMapName, namespace ).execute ();
			log.info ( "Removed {}/{}", namespace, configMapName );
		}
		catch ( ApiException x )
		{
			if ( x.getCode () == HttpStatusCodes.k404_notFound )
			{
				log.info ( "Element {} in {} did not exist.", deployId, namespace );
				return;
			}
			throw new ElementDeployException ( x );
		}
	}

	private static String tagToConfigPullScriptConfigMap ( String tag )
	{
		return ( tag.trim ().toLowerCase () ) + "-configpull";
	}

	private static String loadResource ( String named ) throws ElementDeployException
	{
		try ( final InputStream is = new ResourceLoader ()
			.named ( named )
			.usingStandardSources ( true, ConfigPullScriptDeployer.class )
			.load ()
		)
		{
			if ( is == null ) throw new ElementDeployException ( "Couldn't load resource " + named );

			final byte[] data = StreamTools.readBytes ( is );
			return new String ( data, StandardCharsets.UTF_8 );
		}
		catch ( IOException e )
		{
			throw new ElementDeployException ( e );
		}
	}

	private final String fConfigMapScriptResource;

	private static final Logger log = LoggerFactory.getLogger ( ConfigPullScriptDeployer.class );
}
