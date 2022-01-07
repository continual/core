package io.continual.flowcontrol.jobapi;

import java.util.Collection;

import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.iam.access.AccessException;

public interface FlowControlJobDb
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

	interface Builder
	{
		Builder withName ( String name );
		Builder withOwner ( String ownerId );
		Builder withAccess ( String user, String... ops );

		FlowControlJob build () throws RequestException, ServiceException, AccessException;
	};

	Builder createJob ( FlowControlCallContext fccc ) throws ServiceException;

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
