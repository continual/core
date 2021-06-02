package io.continual.flowcontrol.impl.k8s;

import java.io.IOException;
import java.util.Arrays;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.Config;

public class TestK8sDeploy
{
	private static final String kNamespace = "api-test-namespace";
	private static final String kTestAppName = "test-app";
	
	public static void main ( String[] args ) throws IOException, ApiException
	{
		final ApiClient client = Config.defaultClient ();
		Configuration.setDefaultApiClient ( client );

		final CoreV1Api api = new CoreV1Api ();

		V1Namespace thisNs = null;
		final V1NamespaceList namespaces = api.listNamespace ( null, null, null, null, null, null, null, null, null, null );
		for ( V1Namespace ns : namespaces.getItems () )
		{
			if ( kNamespace.equals ( ns.getMetadata ().getName () ) )
			{
				thisNs = ns;
				break;
			}
		}
		if ( thisNs == null )
		{
			thisNs = api.createNamespace ( new V1NamespaceBuilder ()
				.withNewMetadata ()
					.withName ( kNamespace )
				.endMetadata ()
				.build(),
				null, null, kTestAppName );
		}

		final V1Pod pod = new V1PodBuilder ()
			.withNewMetadata ()
				.withName ( "apod" )
			.endMetadata ()
			.withNewSpec ()
				.addNewContainer ()
					.withName ( "www" )
					.withImage ( "nginx" )
				.endContainer ()
			.endSpec ()
			.build ()
		;

		api.createNamespacedPod ( kNamespace, pod, null, null, null );

		final V1Pod pod2 = new V1Pod ()
			.metadata ( new V1ObjectMeta ().name ( "anotherpod" ) )
			.spec ( new V1PodSpec ().containers ( Arrays.asList (
				new V1Container ().name ( "www" ).image ( "nginx" ) ) ) );

		api.createNamespacedPod ( kNamespace, pod2, null, null, null );

		final V1PodList list = api.listNamespacedPod ( kNamespace, null, null, null, null, null, null, null, null, null, null );
		for ( V1Pod item : list.getItems () )
		{
			System.out.println ( item.getMetadata ().getName () );
		}
	}
}
