package io.continual.flowcontrol.impl.controller.k8s;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.services.encryption.Encryptor;

public interface K8sElement
{
	public static final String kSetting_TemplateResource = "templateResource";

	public static class ElementDeployException extends Exception
	{
		public ElementDeployException ( String t ) { super(t); }
		public ElementDeployException ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}
	
	interface K8sDeployContext
	{
		String getInstallationName ();
		String getNamespace ();
		String getDeployId ();
		FlowControlDeploymentSpec getDeploymentSpec();
		String getRuntimeImage ();
		Encryptor getEncryptor ();
		JSONObject getWorkspace ();
		Map<String,String> getEnvironment ();
		List<String> getImagePullSecrets ();
	}
	
	void deploy ( K8sDeployContext ctx ) throws ElementDeployException;
	void undeploy ( String namespace, String deploymentId ) throws ElementDeployException;
}
