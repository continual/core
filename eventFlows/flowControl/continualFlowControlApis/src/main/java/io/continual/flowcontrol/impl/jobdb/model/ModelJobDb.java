package io.continual.flowcontrol.impl.jobdb.model;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJobBuilder;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelPathListPage;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class ModelJobDb extends SimpleService implements FlowControlJobDb
{
	public ModelJobDb ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fModel = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( kSetting_ModelName, kDefault_ModelName ) ), Model.class );
		fModelUser = new CommonJsonIdentity ( "flowControlUser", CommonJsonIdentity.initializeIdentity (), null );

		fEnc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( "encryptor", "encryptor" ) ), Encryptor.class );
	}

	@Override
	public FlowControlJobBuilder createJobBuilder ( FlowControlCallContext fccc )
	{
		return new ModelJob.ModelJobBuilder ( fccc, fEnc );
	}

	@Override
	public Collection<FlowControlJob> getJobsFor ( FlowControlCallContext fccc ) throws ServiceException
	{
		try ( final ModelRequestContext mrc = buildContext () )
		{
			final Path path = getBaseJobPath ();
			final ModelPathListPage pathList = fModel.listChildrenOfPath ( mrc, path );	// FIXME: does this check READ rights already?

			final LinkedList<FlowControlJob> result = new LinkedList<> ();
			if ( pathList != null )
			{
				for ( Path p : pathList )
				{
					try
					{
						final ModelJob job = internalLoadJob ( mrc, p.getItemName ().toString () );
						if ( job != null && job.getAccessControlList ().canUser ( fccc.getUser (), AccessControlList.READ ) )
						{
							result.add ( job );
						}
					}
					catch ( IamSvcException e )
					{
						throw new ServiceException ( e );
					}
				}
			}
			return result;
		}
		catch ( ModelItemDoesNotExistException e )
		{
			// the jobs db has not been initialized
			return new LinkedList<> ();
		}
		catch ( BuildFailure | ModelServiceException | ModelRequestException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public FlowControlJob getJob ( FlowControlCallContext fccc, String name ) throws ServiceException, AccessException
	{
		try ( final ModelRequestContext mrc = buildContext () )
		{
			final FlowControlJob job = internalLoadJob ( mrc, name );
			checkAccess ( job, fccc, AccessControlList.READ );
			return job;
		}
		catch ( BuildFailure e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public FlowControlJob getJobAsAdmin ( String name ) throws ServiceException
	{
		try ( final ModelRequestContext mrc = buildContext () )
		{
			return internalLoadJob ( mrc, name );
		}
		catch ( BuildFailure e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public void storeJob ( FlowControlCallContext fccc, String jobId, FlowControlJob job ) throws ServiceException, AccessException
	{
		try ( final ModelRequestContext mrc = buildContext () )
		{
			final FlowControlJob existing = internalLoadJob ( mrc, jobId );
			checkAccess ( existing, fccc, AccessControlList.UPDATE );
			internalStoreJob ( mrc, jobId, job );
		}
		catch ( BuildFailure e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public void removeJob ( FlowControlCallContext fccc, String name ) throws ServiceException, AccessException
	{
		try ( final ModelRequestContext mrc = buildContext () )
		{
			checkAccess ( internalLoadJob ( mrc, name ), fccc, AccessControlList.UPDATE );
			final Path path = jobIdToPath ( name );
			fModel.remove ( mrc, path );
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new ServiceException ( e );
		}
	}

	private final Model fModel;
	private final Identity fModelUser;
	private final Encryptor fEnc;

	private static final String kSetting_ModelName = "model";
	private static final String kDefault_ModelName = "jobDbModel";

	private ModelJob internalLoadJob ( ModelRequestContext mrc, String jobId ) throws ServiceException
	{
		try
		{
			return fModel.load ( mrc, jobIdToPath ( jobId ), ModelJob.class );
		}
		catch ( ModelItemDoesNotExistException e )
		{
			return null;
		}
		catch ( ModelServiceException | ModelRequestException e )
		{
			throw new ServiceException ( e );
		}
	}

	private ModelJob internalStoreJob ( ModelRequestContext mrc, String jobId, FlowControlJob job ) throws ServiceException
	{
		try
		{
			final Path path = jobIdToPath ( jobId );

			fModel.createUpdate ( mrc, path )
				.overwriteData ( new JsonModelObject ( ((ModelJob)job).toJson() ) )
				.execute ()
			;

			return internalLoadJob ( mrc, jobId );
		}
		catch ( ModelRequestException | ModelServiceException e )
		{
			throw new ServiceException ( e );
		}
	}

	private static Path getBaseJobPath ( )
	{
		return Path.fromString ( "/jobs" );
	}

	private static Path jobIdToPath ( String jobId )
	{
		return getBaseJobPath().makeChildItem ( Name.fromString ( jobId ) );
	}

	private void checkAccess ( final FlowControlJob job, FlowControlCallContext fccc, String op ) throws AccessException, ServiceException
	{
		try
		{
			if ( job == null ) return;
			if ( !job.getAccessControlList ().canUser ( fccc.getUser (), op ) )
			{
				throw new AccessException ( fccc.getUser() + " may not " + op + " job " + job.getId () + "." );
			}
		}
		catch ( IamSvcException e )
		{
			throw new ServiceException ( e );
		}
	}

	private ModelRequestContext buildContext () throws BuildFailure
	{
		return fModel.getRequestContextBuilder ().forUser ( fModelUser ).build ();
	}
}
