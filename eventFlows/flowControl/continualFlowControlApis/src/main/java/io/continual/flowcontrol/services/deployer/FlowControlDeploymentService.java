package io.continual.flowcontrol.services.deployer;

import java.util.List;
import java.util.Map;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlJob;
import io.continual.services.Service;

/**
 * The deployment service deploys jobs to its runtime environment, e.g. a Kubernetes cluster.
 * In practice, we use k8s in almost all scenarios, but alternative deployment mechanisms exist
 * in some environments. In general, if a deployment environment doesn't support a concept here
 * (e.g. Toleration), that part of the deployment specification is ignored.
 */
public interface FlowControlDeploymentService extends Service
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

	/**
	 * For deployment environments that support it, a toleration. This is modeled after
	 * the Kubernetes concept of the same name.
	 */
	interface Toleration
	{
		default String effect () { return null; }
		default String key () { return null; }
		default String operator () { return null; }
		default Long seconds () { return null; }
		default String value () { return null; }
	}
	
	/**
	 * For deployment environments that support it, resource request and limits. 
	 */
	interface ResourceSpecs
	{
		default String cpuRequest () { return null; }
		default String cpuLimit () { return null; }
		default String memLimit () { return null; }
		default String persistDiskSize () { return null; }
		default String logDiskSize () { return null; }
		default List<Toleration> tolerations () { return null; }
	}

	/**
	 * A builder for resource specs. Note that this interface very lightly abstracts
	 * the actual controller. In practice, you'll likely need to understand the deployment environment
	 * in order to set resource specifications properly. We do not currently attempt to use a 
	 * platform-neutral expression.
	 */
	interface ResourceSpecBuilder
	{
		/**
		 * Make a CPU request
		 * @param cpuReq a CPU sizing value understood by the underlying controller
		 * @return this builder
		 */
		ResourceSpecBuilder withCpuRequest ( String cpuReq );

		/**
		 * Make a CPU limit 
		 * @param cpuLimit a CPU sizing value understood by the underlying controller
		 * @return this builder
		 */
		ResourceSpecBuilder withCpuLimit ( String cpuLimit );

		/**
		 * Make a memory limit 
		 * @param memLimit a memory sizing value understood by the underlying controller
		 * @return this builder
		 */
		ResourceSpecBuilder withMemLimit ( String memLimit );

		/**
		 * For processing systems that require storage for persisting data across runs, what size is needed?
		 * @param diskSize
		 * @return this builder
		 */
		ResourceSpecBuilder withPersistDiskSize ( String diskSize );

		/**
		 * For processing environments in which logging is stored to disk, what size is needed?
		 * @param diskSize
		 * @return this builder
		 */
		ResourceSpecBuilder withLogDiskSize ( String diskSize );

		/**
		 * Use the given toleration in scheduling, when supported.
		 * @param tol
		 * @return this builder
		 */
		ResourceSpecBuilder withToleration ( Toleration tol );

		/**
		 * Use the given tolerations in scheduling, when supported
		 * @param tols
		 * @return this builder
		 */
		default ResourceSpecBuilder withTolerations ( List<Toleration> tols )
		{
			for ( Toleration tol : tols )
			{
				withToleration ( tol );
			}
			return this;
		}

		/**
		 * Incorporate the specified settings and return to the enclosing deployment
		 * spec builder's scope.
		 * @return a deployment spec builder
		 */
		DeploymentSpecBuilder build ();
	};

	/**
	 * A deployment specification.
	 */
	interface DeploymentSpec
	{
		/**
		 * The job to deploy
		 * @return the job
		 */
		FlowControlJob getJob ();

		/**
		 * The number of instances to deploy
		 * @return a number, min 1
		 */
		int getInstanceCount ();

		/**
		 * The process environment to provide in the deployment
		 * @return a map of keys/values
		 */
		Map<String,String> getEnv ();

		/**
		 * Get resource specs. This result is always non-null.
		 * @return a resource spec
		 */
		ResourceSpecs getResourceSpecs ();
	}

	/**
	 * A builder for deployment specs
	 */
	interface DeploymentSpecBuilder
	{
		/**
		 * Deploy the given job
		 * @param job
		 * @return this builder
		 */
		DeploymentSpecBuilder forJob ( FlowControlJob job );

		/**
		 * Use the given instance count
		 * @param count
		 * @return this builder
		 */
		DeploymentSpecBuilder withInstances ( int count );

		/**
		 * Set an environment value for the process. This call will add to existing environment
		 * settings and overwrite any setting with the same key.
		 * @param key
		 * @param val
		 * @return this builder
		 */
		DeploymentSpecBuilder withEnv ( String key, String val );

		/**
		 * Set multiple environment values for the process. This call will add to existing environment
		 * settings and overwrite any with the same key(s).
		 * @param keyValMap
		 * @return this builder
		 */
		DeploymentSpecBuilder withEnv ( Map<String, String> keyValMap );

		/**
		 * Start a resource spec builder within the scope of this deployment spec builder
		 * @return a resource spec builder
		 */
		ResourceSpecBuilder withResourceSpecs ();

		/**
		 * Set the resource specs for this deployment. This call overwrites any previously set.
		 * @param spec
		 * @return this builder
		 */
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

		/**
		 * Produce the deployment spec
		 * @return a deployment spec
		 * @throws BuildFailure
		 */
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
	 * Get all deployments visible to the caller
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

	/**
	 * Get an deployment based on a config key 
	 * @param configKey
	 * @return a deployment or null
	 * @throws ServiceException
	 */
	FlowControlDeployment getDeploymentByConfigKey ( String configKey ) throws ServiceException;
}
