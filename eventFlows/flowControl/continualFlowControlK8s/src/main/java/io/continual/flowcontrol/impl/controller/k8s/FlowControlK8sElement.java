package io.continual.flowcontrol.impl.controller.k8s;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import io.continual.flowcontrol.model.Encryptor;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlRuntimeState;

/**
 * A kubernetes element (eg. statefulset, secret, etc.) that can be deployed or undeployed. In general,
 * these map to k8s "kinds"
 */
public interface FlowControlK8sElement
{
	public static final String kSetting_TemplateResource = "templateResource";

	public static class ElementDeployException extends Exception
	{
		public ElementDeployException ( String t ) { super(t); }
		public ElementDeployException ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}

	enum ImagePullPolicy
	{
		Always,
		IfNotPresent,
		Never
	}

	interface K8sInstallationContext
	{
		String getInstallationName ();
		String getNamespace ();
		String getDeployId ();
	}

	interface K8sDeployContext extends K8sInstallationContext
	{
		FlowControlDeploymentSpec getDeploymentSpec ();

		String getRuntimeImage ();
		ImagePullPolicy getImagePullPolicy ();

		Encryptor getEncryptor ();
		JSONObject getWorkspace ();
		Map<String,String> getEnvironment ();
		List<String> getImagePullSecrets ();
	}

	/**
	 * Deploy this element with the given context. Elements should have names based on the deployment ID
	 * because that's what's presented during undeploy.
	 * @param ctx
	 * @throws ElementDeployException
	 */
	void deploy ( K8sDeployContext ctx ) throws ElementDeployException;

	/**
	 * Is this element deployed? 
	 * @param ctx
	 * @throws ElementDeployException
	 */
	boolean isDeployed ( K8sInstallationContext ctx ) throws ElementDeployException;

	/**
	 * Return true if this element is a runtime provider. Generally the statefulset element would return true
	 * and anything else would return false;
	 * @return true if this element is a runtime provider
	 */
	default boolean isRuntimeProvider () { return false; }

	/**
	 * If appropriate, get the runtime state associated with this element.  If the element is not 
	 * current deployed, return null.
	 * @param ctx
	 * @return a runtime state or null
	 * @throws ElementDeployException
	 */
	default FlowControlRuntimeState getRuntimeState ( K8sInstallationContext ctx ) throws ElementDeployException { return null; }
	
	/**
	 * Undeploy this element given the namespace and deployment ID
	 * @param ctx
	 * @throws ElementDeployException
	 */
	void undeploy ( K8sInstallationContext ctx ) throws ElementDeployException;
}
