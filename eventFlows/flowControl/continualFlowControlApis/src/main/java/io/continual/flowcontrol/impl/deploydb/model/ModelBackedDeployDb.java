package io.continual.flowcontrol.impl.deploydb.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.deploydb.common.DeploymentSerde;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.services.deploydb.DeploymentDb;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class ModelBackedDeployDb extends SimpleService implements DeploymentDb
{
	public ModelBackedDeployDb ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fModel = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( kSetting_ModelName, kDefault_ModelName ) ), Model.class );
		fModelUser = sc.getExprEval ().evaluateText ( config.optString ( kSetting_ModelUser ) );

		// make sure we have the indexes we need
		try
		{
			fModel.createIndex ( DeploymentSerde.kField_Owner );
			fModel.createIndex ( DeploymentSerde.kField_JobId );
			fModel.createIndex ( DeploymentSerde.kField_ConfigKey );
		}
		catch ( ModelRequestException | ModelServiceException e )
		{
			throw new BuildFailure ( e );
		}

		fEnc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( "encryptor", "encryptor" ) ), Encryptor.class );
	}

	@Override
	public void storeDeployment ( FlowControlDeployment deployment ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			fModel.createUpdate ( mrc, makeDeployIdPath ( deployment.getId () ) )
				.overwriteData ( new JsonModelObject ( DeploymentSerde.serialize ( deployment, fEnc ) ) )
				.execute ()
			;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	@Override
	public void removeDeployment ( String deployId ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			fModel.remove ( mrc, makeDeployIdPath ( deployId ) );
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	@Override
	public FlowControlDeployment getDeploymentById ( String deployId ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			return deploymentFrom ( fModel.load ( mrc, makeDeployIdPath ( deployId ) ) );
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	@Override
	public List<FlowControlDeployment> getDeploymentsForUser ( Identity userId ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
			for ( ModelObjectAndPath<BasicModelObject> o : fModel.startQuery ()
				.withFieldValue ( DeploymentSerde.kField_Owner, userId.getId () )
				.execute ( mrc )
			)
			{
				result.add ( deploymentFrom ( o.getObject () ) );
			}
			return result;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	@Override
	public List<FlowControlDeployment> getDeploymentsOfJob ( String jobId ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			final LinkedList<FlowControlDeployment> result = new LinkedList<> ();
			for ( ModelObjectAndPath<BasicModelObject> o : fModel.startQuery ()
				.withFieldValue ( DeploymentSerde.kField_JobId, jobId )
				.execute ( mrc )
			)
			{
				result.add ( deploymentFrom ( o.getObject () ) );
			}
			return result;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	@Override
	public FlowControlDeployment getDeploymentByConfigKey ( String configKey ) throws DeployDbException
	{
		try ( final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
			.forSimpleIdentity ( fModelUser )
			.build ()
		)
		{
			final ModelObjectList<BasicModelObject> resultSet = fModel.startQuery ()
				.withFieldValue ( DeploymentSerde.kField_ConfigKey, configKey )
				.execute ( mrc )
			;
			final Iterator<ModelObjectAndPath<BasicModelObject>> it = resultSet.iterator ();
			final BasicModelObject first = it.hasNext () ? it.next ().getObject () : null;
			if ( it.hasNext () )
			{
				log.warn ( "Query by config key {} returned more than one object.", configKey );
			}
			return deploymentFrom ( first );
		}
		catch ( ModelItemDoesNotExistException e )
		{
			return null;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			throw new DeployDbException ( e );
		}
	}

	private final Model fModel;
	private final String fModelUser;

	private final Encryptor fEnc;

	private static final String kSetting_ModelName = "model";
	private static final String kDefault_ModelName = "deployDbModel";

	private static final String kSetting_ModelUser = "modelUser";

	private static final Logger log = LoggerFactory.getLogger ( ModelBackedDeployDb.class );
	
	private Path makeDeployIdPath ( String id )
	{
		return Path.getRootPath ()
			.makeChildItem ( Name.fromString ( "deployments" ) )
			.makeChildItem ( Name.fromString ( id ) )
		;
	}

	private FlowControlDeployment deploymentFrom ( BasicModelObject bmo )
	{
		if ( bmo == null ) return null;
		return DeploymentSerde.deserialize ( JsonModelObject.modelObjectToJson ( bmo.getData () ), fEnc );
	}
}
