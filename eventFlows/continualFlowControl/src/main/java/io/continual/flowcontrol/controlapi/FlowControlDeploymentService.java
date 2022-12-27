package io.continual.flowcontrol.controlapi;

import java.util.List;
import java.util.Map;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.jobapi.FlowControlJob;

public interface FlowControlDeploymentService
{
	class ServiceException extends Exception
	{
		public ServiceException ( String msg ) { super(msg); }
		public ServiceException ( Throwable t ) { super(t); }
		public ServiceException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}

	class RequestException extends Exception
	{
		public RequestException ( String msg ) { super(msg); }
		public RequestException ( Throwable t ) { super(t); }
		public RequestException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}

	interface DeploymentSpec
	{
		FlowControlJob getJob ();
		int getInstanceCount ();
		Map<String,String> getEnv ();

		default String getCpuLimitSpec () { return null; }
		default String getMemLimitSpec () { return null; };
	}

	interface DeploymentSpecBuilder
	{
		DeploymentSpecBuilder forJob ( FlowControlJob job );
		DeploymentSpecBuilder withInstances ( int count );
		DeploymentSpecBuilder withEnv ( String key, String val );
		DeploymentSpecBuilder withEnv ( Map<String, String> keyValMap );
		DeploymentSpecBuilder withCpuLimit ( String cpuLimit );
		DeploymentSpecBuilder withMemLimit ( String memLimit );
		DeploymentSpec build () throws BuildFailure;
	}

	/**
	 * Get a deployment spec builder for this deployment service
	 * @return
	 */
	DeploymentSpecBuilder deploymentBuilder ();
	
	/**
	 * Deploy a job
	 * @param ctx
	 * @param spec
	 * @return a deployment instance
	 * @throws ServiceException
	 * @throws RequestException 
	 */
	FlowControlDeployment deploy ( FlowControlCallContext ctx, DeploymentSpec spec ) throws ServiceException, RequestException;

	/**
	 * Remove a job from deployment
	 * @param ctx
	 * @param deploymentId
	 * @throws ServiceException
	 */
	void undeploy ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException;

	/**
	 * Get a deployment by its deployment ID
	 * @param ctx
	 * @param deploymentId
	 * @return a deployment, or null if no such deployment 
	 * @throws ServiceException
	 */
	FlowControlDeployment getDeployment ( FlowControlCallContext ctx, String deploymentId ) throws ServiceException;

	/**
	 * Get all deployments
	 * @param ctx
	 * @return a list of deployments
	 * @throws ServiceException
	 */
	List<FlowControlDeployment> getDeployments ( FlowControlCallContext ctx ) throws ServiceException;

	/**
	 * Get deployments of a particular job
	 * @param ctx
	 * @param jobId
	 * @return a list of deployments
	 * @throws ServiceException
	 */
	List<FlowControlDeployment> getDeploymentsForJob ( FlowControlCallContext ctx, String jobId ) throws ServiceException;
}
