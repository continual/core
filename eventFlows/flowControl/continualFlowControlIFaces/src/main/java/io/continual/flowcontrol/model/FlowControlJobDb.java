package io.continual.flowcontrol.model;

import java.util.Collection;

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
	 * Create a job using a builder
	 * @param fccc
	 * @return a new job builder
	 * @throws ServiceException
	 */
	FlowControlJobBuilder createJobBuilder ( FlowControlCallContext fccc ) throws ServiceException;

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
	 * @param jobId
	 * @return a job or null
	 * @throws ServiceException
	 * @throws AccessException 
	 */
	FlowControlJob getJob ( FlowControlCallContext ctx, String jobId ) throws ServiceException, AccessException;

	/**
	 * Get a job (or return null) without checking access rights.
	 * @param asUserId
	 * @param jobId
	 * @return a job or null
	 * @throws ServiceException
	 */
	FlowControlJob getJobAsAdmin ( String asUserId, String jobId ) throws ServiceException;

	/**
	 * Check if a job exists.
	 * @param ctx
	 * @param jobId
	 * @return true if the job exists and the user can read it.
	 * @throws ServiceException
	 */
	default boolean jobExists ( FlowControlCallContext ctx, String jobId ) throws ServiceException
	{
		try
		{
			return getJob ( ctx, jobId ) != null;
		}
		catch ( AccessException e )
		{
			return false;
		}
	}
	
	/**
	 * Store the given job. If the ID exists, it's overwritten.
	 * @param ctx
	 * @param jobId
	 * @param job
	 * @throws ServiceException
	 * @throws AccessException
	 * @throws RequestException
	 */
	void storeJob ( FlowControlCallContext ctx, String jobId, FlowControlJob job ) throws ServiceException, AccessException, RequestException;

	/**
	 * Remove the given job
	 * @param ctx
	 * @param jobId
	 * @throws ServiceException
	 * @throws AccessException
	 * @throws RequestException
	 */
	void removeJob ( FlowControlCallContext ctx, String jobId ) throws ServiceException, AccessException, RequestException;
}
