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

	interface Toleration
	{
		default String effect () { return null; }
		default String key () { return null; }
		default String operator () { return null; }
		default Long seconds () { return null; }
		default String value () { return null; }
	}
	
	interface ResourceSpecs
	{
		default String cpuRequest () { return null; }
		default String cpuLimit () { return null; }
		default String memLimit () { return null; }
		default String persistDiskSize () { return null; }
		default String logDiskSize () { return null; }
		default List<Toleration> tolerations () { return null; }
	}

	interface ResourceSpecBuilder
	{
		ResourceSpecBuilder withCpuRequest ( String cpuReq );
		ResourceSpecBuilder withCpuLimit ( String cpuLimit );
		ResourceSpecBuilder withMemLimit ( String memLimit );
		ResourceSpecBuilder withPersistDiskSize ( String diskSize );
		ResourceSpecBuilder withLogDiskSize ( String diskSize );
		ResourceSpecBuilder withToleration ( Toleration tol );
		default ResourceSpecBuilder withTolerations ( List<Toleration> tols )
		{
			for ( Toleration tol : tols )
			{
				withToleration ( tol );
			}
			return this;
		}

		DeploymentSpecBuilder build ();
	};
	
	interface DeploymentSpec
	{
		FlowControlJob getJob ();
		int getInstanceCount ();
		Map<String,String> getEnv ();

		/**
		 * Get resource specs. This result is always non-null.
		 * @return a resource spec
		 */
		ResourceSpecs getResourceSpecs ();
	}

	interface DeploymentSpecBuilder
	{
		DeploymentSpecBuilder forJob ( FlowControlJob job );
		DeploymentSpecBuilder withInstances ( int count );
		DeploymentSpecBuilder withEnv ( String key, String val );
		DeploymentSpecBuilder withEnv ( Map<String, String> keyValMap );

		ResourceSpecBuilder withResourceSpecs ();
		default DeploymentSpecBuilder withResourceSpecs ( ResourceSpecs spec )
		{
			return withResourceSpecs ()
				.withCpuRequest ( spec.cpuRequest () )
				.withCpuLimit ( spec.cpuLimit () )
				.withMemLimit ( spec.memLimit () )
				.withPersistDiskSize ( spec.persistDiskSize () )
				.withLogDiskSize ( spec.logDiskSize () )
				.withTolerations ( spec.tolerations () )
				.build ()
			;
		}
		
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
