package io.continual.flowcontrol.impl.jobdb.model;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.impl.common.JsonJobWas;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.flowcontrol.services.jobdb.FlowControlJobDb;
import io.continual.iam.access.AccessControlEntry;
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
import io.continual.services.model.core.data.BasicModelObject;
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
		final JSONObject modelData = config.getJSONObject ( "model" );
		fModel = io.continual.builder.Builder.fromJson ( Model.class, modelData, sc );
		fModelUser = new CommonJsonIdentity ( "flowControlUser", CommonJsonIdentity.initializeIdentity (), null );

		fEnc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( "encryptor", "encryptor" ) ), Encryptor.class );
	}

	@Override
	public Builder createJob ( FlowControlCallContext fccc )
	{
		return new ModelFcJobBuilder ( fccc );
	}

	@Override
	public Collection<FlowControlJob> getJobsFor ( FlowControlCallContext fccc ) throws ServiceException
	{
		try
		{
			final ModelRequestContext mrc = buildContext ();

			final Path path = getBaseJobPath ();
			final ModelPathListPage pathList = fModel.listChildrenOfPath ( mrc, path );	// FIXME: does this check READ rights already?

			final LinkedList<FlowControlJob> result = new LinkedList<> ();
			if ( pathList != null )
			{
				for ( Path p : pathList )
				{
					try
					{
						final FlowControlJob job = internalLoadJob ( mrc, p.getItemName ().toString () );
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
		catch ( BuildFailure | ModelServiceException | ModelRequestException e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public FlowControlJob getJob ( FlowControlCallContext fccc, String name ) throws ServiceException, AccessException
	{
		try
		{
			final ModelRequestContext mrc = buildContext ();
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
		try
		{
			final ModelRequestContext mrc = buildContext ();
			return internalLoadJob ( mrc, name );
		}
		catch ( BuildFailure e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public void storeJob ( FlowControlCallContext fccc, String name, FlowControlJob job ) throws ServiceException, AccessException
	{
		try
		{
			final ModelRequestContext mrc = buildContext ();

			final FlowControlJob existing = internalLoadJob ( mrc, name );
			checkAccess ( existing, fccc, AccessControlList.UPDATE );
			internalStoreJob ( mrc, job );
		}
		catch ( BuildFailure e )
		{
			throw new ServiceException ( e );
		}
	}

	@Override
	public void removeJob ( FlowControlCallContext fccc, String name ) throws ServiceException, AccessException
	{
		try
		{
			final ModelRequestContext mrc = buildContext ();
			checkAccess ( internalLoadJob ( mrc, name ), fccc, AccessControlList.UPDATE );
			final Path path = jobNameToPath ( name );
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

	private FlowControlJob internalLoadJob ( ModelRequestContext mrc, String name ) throws ServiceException
	{
		try
		{
			final Path path = jobNameToPath ( name );
			final BasicModelObject mo = fModel.load ( mrc, path );
			return new ModelFcJob ( name, mo );
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

	private FlowControlJob internalStoreJob ( ModelRequestContext mrc, FlowControlJob job ) throws ServiceException
	{
		try
		{
			final String name = job.getName ();
			final Path path = jobNameToPath ( name );

			fModel.createUpdate ( mrc, path )
				.overwriteData ( new JsonModelObject ( ((ModelFcJob)job).toJson() ) )
				.execute ()
			;

			return internalLoadJob ( mrc, name );
		}
		catch ( ModelRequestException | ModelServiceException e )
		{
			throw new ServiceException ( e );
		}
	}

	private class ModelFcJobBuilder implements Builder
	{
		public ModelFcJobBuilder ( FlowControlCallContext fccc )
		{
			fCtx = fccc;
			fAces = new LinkedList<> ();
			
			withAccess ( AccessControlEntry.kOwner, AccessControlList.READ, AccessControlList.UPDATE, AccessControlList.DELETE );
		}

		@Override
		public Builder withName ( String name )
		{
			fName = name;
			return this;
		}

		@Override
		public Builder withOwner ( String owner )
		{
			fOwner = owner;
			return this;
		}

		@Override
		public Builder withAccess ( String user, String... ops )
		{
			fAces.add ( AccessControlEntry.builder ()
				.permit ()
				.forSubject ( user )
				.operations ( ops )
				.build ()
			);
			return this;
		}

		@Override
		public FlowControlJob build () throws RequestException, ServiceException, AccessException
		{
			if ( fName == null || fName.length () == 0 )
			{
				throw new RequestException ( "Name is not set." );
			}

			FlowControlJob existing = getJob ( fCtx, fName );
			if ( existing != null ) 
			{
				throw new RequestException ( "Job " + fName + " already exists." );
			}

			try
			{
				final ModelRequestContext mrc = buildContext ();
				final ModelFcJob job = new ModelFcJob ( this );

				final AccessControlList acl = job.getAccessControlList ();
				acl
					.setOwner ( fOwner )
					.clear ()
				;
				for ( AccessControlEntry ace : fAces )
				{
					acl.addAclEntry ( ace );
				}

				internalStoreJob ( mrc, job );
				return internalLoadJob ( mrc, fName );
			}
			catch ( BuildFailure e )
			{
				throw new ServiceException ( e );
			}
		}

		private final FlowControlCallContext fCtx;
		private String fName;
		private String fOwner;
		private LinkedList<AccessControlEntry> fAces;
	}

	private class ModelFcJob extends JsonJobWas
	{
		public ModelFcJob ( ModelFcJobBuilder builder )
		{
			super ( builder.fName, fEnc );
		}

		public ModelFcJob ( String name, BasicModelObject mo )
		{
			super ( name, fEnc, JsonModelObject.modelObjectToJson ( mo.getData() ) );
		}
	}

	private static Path getBaseJobPath ( )
	{
		return Path.fromString ( "/jobs" );
	}

	private static Path jobNameToPath ( String name )
	{
		return getBaseJobPath().makeChildItem ( Name.fromString ( name ) );
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
