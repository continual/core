package io.continual.flowcontrol.impl.jobdb.memory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.common.JsonJob;
import io.continual.flowcontrol.impl.common.JsonJobBuilder;
import io.continual.flowcontrol.model.Encryptor;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJobBuilder;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * A memory-backed job database for test
 */
public class MemoryJobDb extends SimpleService implements FlowControlJobDb
{
	public MemoryJobDb ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fEnc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( "encryptor", "encryptor" ) ), Encryptor.class );
		fUserToJobs = new HashMap<> ();
	}

	@Override
	public FlowControlJobBuilder createJobBuilder ( FlowControlCallContext fccc )
	{
		return new JsonJobBuilder ( fccc, fEnc );
	}

	@Override
	public Collection<FlowControlJob> getJobsFor ( FlowControlCallContext fccc ) throws ServiceException
	{
		final LinkedList<FlowControlJob> result = new LinkedList<> ();

		final String userId = fccc.getUser ().getId ();

		final HashMap<String,JsonJob> userJobs = fUserToJobs.get ( userId );
		if ( userJobs != null )
		{
			for ( JsonJob job : userJobs.values () )
			{
				if ( hasAccess ( job, fccc, AccessControlList.READ ) )
				{
					result.add ( job );
				}
			}
		}

		return result;
	}

	@Override
	public FlowControlJob getJob ( FlowControlCallContext fccc, String jobId ) throws ServiceException, AccessException
	{
		final String userId = fccc.getUser ().getId ();

		final HashMap<String,JsonJob> userJobs = fUserToJobs.get ( userId );
		if ( userJobs != null )
		{
			final JsonJob job = userJobs.get ( jobId );
			if ( job != null )
			{
				checkAccess ( job, fccc, AccessControlList.READ );
				return job;
			}
		}
		return null;
	}

	@Override
	public FlowControlJob getJobAsAdmin ( String asUserId, String jobId )
	{
		final HashMap<String,JsonJob> userJobs = fUserToJobs.get ( asUserId );
		if ( userJobs != null )
		{
			return userJobs.get ( jobId );
		}
		return null;
	}

	@Override
	public void storeJob ( FlowControlCallContext fccc, String jobId, FlowControlJob job ) throws RequestException, AccessException, ServiceException
	{
		final String userId = fccc.getUser ().getId ();

		HashMap<String,JsonJob> userJobs = fUserToJobs.get ( userId );
		if ( userJobs == null )
		{
			userJobs = new HashMap<> ();
			fUserToJobs.put ( userId, userJobs );
		}

		final JsonJob existing = userJobs.get ( jobId );
		if ( existing != null )
        {
			checkAccess ( existing, fccc, AccessControlList.UPDATE );
        }

		userJobs.put ( jobId, cloneJob ( job ) );
	}

	@Override
	public void removeJob ( FlowControlCallContext fccc, String jobId ) throws AccessException, ServiceException
	{
		final String userId = fccc.getUser ().getId ();

		final HashMap<String,JsonJob> userJobs = fUserToJobs.get ( userId );
		if ( userJobs != null )
		{
			final JsonJob existing = userJobs.get ( jobId );
			if ( existing != null )
	        {
				checkAccess ( existing, fccc, AccessControlList.DELETE );
	        }
			fUserToJobs.remove ( jobId );
		}
	}

	private final Encryptor fEnc;
	private final HashMap<String,HashMap<String,JsonJob>> fUserToJobs;

	private void checkAccess ( final FlowControlJob job, FlowControlCallContext fccc, String op ) throws AccessException, ServiceException
	{
		if ( !hasAccess ( job, fccc, op ) )
		{
			throw new AccessException ( fccc.getUser() + " may not " + op + " job " + job.getId () + "." );
		}
	}

	private boolean hasAccess ( final FlowControlJob job, FlowControlCallContext fccc, String op ) throws ServiceException
	{
		try
		{
			if ( job == null ) return true;
			return job.getAccessControlList ().canUser ( fccc.getUser (), op );
		}
		catch ( IamSvcException e )
		{
			throw new ServiceException ( e );
		}
	}

	private JsonJob cloneJob ( FlowControlJob job ) throws RequestException 
	{
		if ( ! ( job instanceof JsonJob ) )
		{
			throw new RequestException ( "Job not created here." );
		}

		final JsonJob jj = (JsonJob) job;
		final JSONObject clonedData = jj.toJson ();
		return new JsonJob ( clonedData );
	}
}
