package io.continual.flowcontrol.services.jobdb;

import java.util.Collection;

import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlJob;
import io.continual.iam.access.AccessException;
import io.continual.services.Service;

/**
 * The FlowControl job database stores event processing jobs for users. Jobs are deployed from this
 * database.
 */
public interface FlowControlJobDb extends Service
{
	class RequestException extends Exception
	{
		public RequestException ( String msg ) { super(msg); }
		public RequestException ( Throwable t ) { super(t); }
		public RequestException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}

	class ServiceException extends Exception
	{
		public ServiceException ( String msg ) { super(msg); }
		public ServiceException ( Throwable t ) { super(t); }
		public ServiceException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * a builder for job entries
	 */
	interface Builder
	{
		/**
		 * Use the given name for this job
		 * @param name
		 * @return this builder
		 */
		Builder withName ( String name );

		/**
		 * Use the given owner ID for this job
		 * @param ownerId
		 * @return this builder
		 */
		Builder withOwner ( String ownerId );

		/**
		 * Grant the given user the given operations
		 * @param user
		 * @param ops
		 * @return this builder
		 */
		Builder withAccess ( String user, String... ops );

		/**
		 * Build the job
		 * @return a job
		 * @throws RequestException
		 * @throws ServiceException
		 * @throws AccessException
		 */
		FlowControlJob build () throws RequestException, ServiceException, AccessException;
	};

	/**
	 * Create a job using a builder
	 * @param fccc
	 * @return a new job builder
	 * @throws ServiceException
	 */
	Builder createJob ( FlowControlCallContext fccc ) throws ServiceException;

	/**
	 * Get the jobs for the given user
	 * @param fccc
	 * @return a collection of 0 or more jobs
	 * @throws ServiceException
	 */
	Collection<FlowControlJob> getJobsFor ( FlowControlCallContext fccc ) throws ServiceException;

	/**
	 * Get a flow control job or return null if it does not exist for the calling user.
	 * @param ctx
	 * @param name
	 * @return a job or null
	 * @throws ServiceException
	 * @throws AccessException 
	 */
	FlowControlJob getJob ( FlowControlCallContext ctx, String name ) throws ServiceException, AccessException;

	/**
	 * Get a job (or return null) without checking access rights.
	 * @param name
	 * @return a job or null
	 * @throws ServiceException
	 */
	FlowControlJob getJobAsAdmin ( String name ) throws ServiceException;

	/**
	 * Check if a job exists.
	 * @param ctx
	 * @param name
	 * @return true if the job exists and the user can read it.
	 * @throws ServiceException
	 */
	default boolean jobExists ( FlowControlCallContext ctx, String name ) throws ServiceException
	{
		try
		{
			return getJob ( ctx, name ) != null;
		}
		catch ( AccessException e )
		{
			return false;
		}
	}
	
	/**
	 * Store the give job
	 * @param ctx
	 * @param name
	 * @param job
	 * @throws ServiceException
	 * @throws AccessException
	 * @throws RequestException
	 */
	void storeJob ( FlowControlCallContext ctx, String name, FlowControlJob job ) throws ServiceException, AccessException, RequestException;

	/**
	 * Remove the given job
	 * @param ctx
	 * @param name
	 * @throws ServiceException
	 * @throws AccessException
	 * @throws RequestException
	 */
	void removeJob ( FlowControlCallContext ctx, String name ) throws ServiceException, AccessException, RequestException;
}
