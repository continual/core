package io.continual.flowcontrol.impl.controller.k8s.elements;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.flowcontrol.impl.controller.k8s.FlowControlK8sElement;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;

public class SecretDeployer implements FlowControlK8sElement
{
	public static final String kWorkspaceKey_Secret = "secret";
	public static final String kWorkspaceKey_SecretKeys = "secretKeys";

	public SecretDeployer ( JSONObject config )
	{
	}

	@Override
	public String toString ()
	{
		return "SecretDeployer";
	}

	@Override
	public void deploy ( K8sDeployContext ctx ) throws ElementDeployException
	{
		try
		{
			final String secretsName = tagToSecret ( ctx.getDeployId () );

			// start a secret for this job's secret data
			final Map<String,String> secrets = ctx.getDeploymentSpec ().getJob ().getSecrets ( ctx.getEncryptor () );
			final V1SecretBuilder sb = new V1SecretBuilder ()
				.withType ( "Opaque" )
				.withNewMetadata ()
					.withName ( secretsName )
				.endMetadata()
			;

			// iterate through registered secrets and add them to the secret builder
			Map<String,byte[]> secretsData = new HashMap<> ();
			for ( Map.Entry<String,String> secret : secrets.entrySet () )
			{
				final String val = secret.getValue ();
				if ( val != null )
				{
					secretsData.put ( secret.getKey (), secret.getValue ().getBytes ( StandardCharsets.UTF_8 ) );
				}
			}

			// if we have secret data, attach it to the secret builder
			if ( secretsData.size () > 0 )
			{
				sb.withData ( secretsData );
			}

			// deploy the secret (which could be empty)
			final V1Secret secret = sb.build ();
			final CoreV1Api api = new CoreV1Api ();
			try
			{
				api.createNamespacedSecret ( ctx.getNamespace (), secret ).execute ();
			}
			catch ( ApiException e )
			{
				if ( e.getCode () == HttpStatusCodes.k409_conflict )
				{
					// Conflict, the Secret already exists
					api.replaceNamespacedSecret ( secretsName, ctx.getNamespace (), secret ).execute ();
				}
				else
				{
					throw e;
				}
			}
			log.info ( "deployed secret [{}]", secret.getMetadata ().getName () );

			ctx.getWorkspace ().put ( kWorkspaceKey_Secret, secretsName );
			ctx.getWorkspace ().put ( kWorkspaceKey_SecretKeys, JsonVisitor.collectionToArray ( secrets.keySet () ) );
		}
		catch ( ApiException | GeneralSecurityException x )
		{
			throw new ElementDeployException ( x );
		}
	}

	@Override
	public boolean isDeployed ( K8sInstallationContext ctx ) throws ElementDeployException
	{
		try
		{
			final String secretsName = tagToSecret ( ctx.getDeployId () );
			new CoreV1Api()
				.readNamespacedSecret ( secretsName, ctx.getNamespace () )
				.execute ()
			;
			return true;
		}
		catch ( ApiException x )
		{
			if ( x.getCode () == HttpStatusCodes.k404_notFound )
			{
				return false;
			}
			throw new ElementDeployException ( x );
		}
	}

	@Override
	public void undeploy ( K8sInstallationContext ctx ) throws ElementDeployException
	{
		final String secretsName = tagToSecret ( ctx.getDeployId () );
		final CoreV1Api api = new CoreV1Api ();
		try
		{
			api.deleteNamespacedSecret ( secretsName, ctx.getNamespace () ).execute ();
			log.info ( "Removed {}/{}", ctx.getNamespace (), secretsName );
		}
		catch ( ApiException x )
		{
			if ( x.getCode () == HttpStatusCodes.k404_notFound )
			{
				log.info ( "Secret {} in {} did not exist.", ctx.getDeployId (), ctx.getNamespace () );
				return;
			}
			throw new ElementDeployException ( x );
		}
	}

	private static String tagToSecret ( String tag )
	{
		return ( tag.trim ().toLowerCase () ) + "-secret";
	}

	private static final Logger log = LoggerFactory.getLogger ( SecretDeployer.class );
}
