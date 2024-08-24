package io.continual.flowcontrol.impl.deploydb.model;

import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlJob;
import io.continual.flowcontrol.impl.enc.Enc;
import io.continual.flowcontrol.impl.enc.Encryptor;
import io.continual.flowcontrol.impl.jobdb.common.JsonJob;
import io.continual.flowcontrol.services.deploydb.DeploymentDb;
import io.continual.flowcontrol.services.deployer.FlowControlDeployedProcess;
import io.continual.flowcontrol.services.deployer.FlowControlDeployment;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.DeploymentSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ResourceSpecs;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.Toleration;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.impl.noop.NoopMetricsCatalog;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
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
			fModel.createIndex ( kField_Owner );
			fModel.createIndex ( kField_JobId );
			fModel.createIndex ( kField_ConfigKey );
		}
		catch ( ModelRequestException | ModelServiceException e )
		{
			throw new BuildFailure ( e );
		}

		try
		{
			fEnc = new Enc ( config.getString ( "secretEncryptKey" ) );
		}
		catch ( GeneralSecurityException e )
		{
			throw new BuildFailure ( e );
		}
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
				.overwriteData ( new JsonModelObject ( serialize ( deployment ) ) )
			;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException | RequestException | ServiceException | io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException e )
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
				.withFieldValue ( kField_Owner, userId.getId () )
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
				.withFieldValue ( kField_JobId, jobId )
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
				.withFieldValue ( kField_ConfigKey, configKey )
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

	private static final String kField_Owner = "deployer";
	private static final String kField_JobId = "jobId";
	private static final String kField_ConfigKey = "configKey";
	
	private static final Logger log = LoggerFactory.getLogger ( ModelBackedDeployDb.class );
	
	private Path makeDeployIdPath ( String id )
	{
		return Path.getRootPath ()
			.makeChildItem ( Name.fromString ( "deployments" ) )
			.makeChildItem ( Name.fromString ( id ) )
		;
	}

	private JSONObject serialize ( FlowControlDeployment d ) throws RequestException, ServiceException, io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException, JSONException, DeployDbException
	{
		final JSONObject instances = new JSONObject ();
		for ( String instance : d.instances () )
		{
			final FlowControlDeployedProcess fcdp = d.getProcessById ( instance );
			instances.put ( instance, serialize ( fcdp ) );
		}

		return new JSONObject ()
			.put ( "id", d.getId () )
			.put ( kField_Owner, d.getDeployer ().getId () )
			.put ( kField_ConfigKey, d.getConfigKey () )
			.put ( kField_JobId, d.getJobId () )
			.put ( "spec", serialize ( d.getDeploymentSpec () ) )
			.put ( "instanceCount", d.instanceCount () )
			.put ( "instances", instances )
		;
	}

	private JSONObject serialize ( DeploymentSpec ds ) throws io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException, JSONException, DeployDbException
	{
		return new JSONObject ()
			.put ( "job", serialize ( ds.getJob () ) )
			.put ( "instanceCount", ds.getInstanceCount () )
			.put ( "env", JsonVisitor.mapOfStringsToObject ( ds.getEnv () ) )
			.put ( "resources", serialize ( ds.getResourceSpecs () ) )
		;
	}

	private JSONObject serialize ( FlowControlJob job ) throws io.continual.flowcontrol.services.jobdb.FlowControlJobDb.ServiceException, JSONException, DeployDbException
	{
		if ( job instanceof JsonJob ) return ((JsonJob) job).toJson ();
		throw new DeployDbException ( "The ModelBackedDeployDb only works with JsonJob FlowControlJobs." );
	}

	private JSONObject serialize ( ResourceSpecs rs )
	{
		return new JSONObject ()
			.put ( "cpuReq", rs.cpuRequest () )
			.put ( "cpuLim", rs.cpuLimit () )
			.put ( "memLim", rs.memLimit () )
			.put ( "persistDiskSize", rs.persistDiskSize () )
			.put ( "logDiskSize", rs.logDiskSize () )
			.put ( "tolerations", JsonVisitor.listToArray ( rs.tolerations (), new ItemRenderer<Toleration,JSONObject> ()
			{
				@Override
				public JSONObject render ( Toleration t ) throws IllegalArgumentException
				{
					return new JSONObject ()
						.put ( "effect", t.effect () )
						.put ( "key", t.key () )
						.put ( "operator", t.operator () )
						.put ( "seconds", t.seconds () )
						.put ( "value", t.value () )
					;
				}
			}  ) )
		;
	}

	private JSONObject serialize ( FlowControlDeployedProcess dp )
	{
		return new JSONObject ()
			.put ( "pid", dp.getProcessId () )
			// note that we're not writing the log or metrics streams here
		;
	}

	private FlowControlDeployment deploymentFrom ( BasicModelObject bmo )
	{
		if ( bmo == null ) return null;
		return new LocalDeployment ( JsonModelObject.modelObjectToJson ( bmo.getData () ), fEnc );
	}

	private static final class LocalDeployment implements FlowControlDeployment
	{
		public LocalDeployment ( JSONObject top, Encryptor enc )
		{
			fJson = top;
			fLdEnc = enc;
		}

		@Override
		public String getId ()
		{
			return fJson.optString ( "id", null );
		}

		@Override
		public Identity getDeployer ()
		{
			return new SimpleIdentityReference ( fJson.optString ( kField_Owner, null ) );
		}

		@Override
		public String getConfigKey ()
		{
			return fJson.optString ( kField_ConfigKey, null );
		}

		@Override
		public String getJobId ()
		{
			return fJson.optString ( kField_JobId, null );
		}

		@Override
		public DeploymentSpec getDeploymentSpec ()
		{
			final JSONObject specData = fJson.optJSONObject ( "spec" );
			if ( specData != null )
			{
				return new DeploymentSpec ()
				{
					@Override
					public FlowControlJob getJob ()
					{
						final JSONObject job = fJson.optJSONObject ( "job" );
						return new JsonJob ( job.getString ( "name" ), fLdEnc, job );
					}

					@Override
					public int getInstanceCount ()
					{
						return specData.optInt ( "instanceCount", 1 );
					}

					@Override
					public Map<String, String> getEnv ()
					{
						return JsonVisitor.objectToMap ( specData.optJSONObject ( "env" ) );
					}

					@Override
					public ResourceSpecs getResourceSpecs ()
					{
						final JSONObject rs = specData.optJSONObject ( "resources" );
						if ( rs == null ) return null;

						return new ResourceSpecs ()
						{
							public String cpuRequest () { return rs.optString ( "cpuReq", null ); }
							public String cpuLimit () { return rs.optString ( "cpuLim", null ); }
							public String memLimit () { return rs.optString ( "memLim", null ); }
							public String persistDiskSize () { return rs.optString ( "persistDiskSize", null ); }
							public String logDiskSize () { return rs.optString ( "logDiskSize", null ); }
							public List<Toleration> tolerations ()
							{
								final LinkedList<Toleration> result = new LinkedList<> ();
								JsonVisitor.forEachElement ( rs.optJSONArray ( "tolerations" ), new ArrayVisitor<JSONObject,JSONException> ()
								{
									@Override
									public boolean visit ( JSONObject tol ) throws JSONException
									{
										result.add ( new Toleration ()
										{
											public String effect () { return tol.optString ( "effect", null ); }
											public String key () { return tol.optString ( "key", null ); }
											public String operator () { return tol.optString ( "operator", null ); }
											public Long seconds () { return tol.optLongObject ( "seconds", null ); }
											public String value () { return tol.optString ( "value", null ); }
										} );
										return true;
									}
								} );
								return result;
							}
						};
					}
				};
			}
			return null;
		}

		@Override
		public Status getStatus () throws ServiceException
		{
			// we can't know the status in this context
			return Status.UNKNOWN;
		}

		@Override
		public int instanceCount ()
		{
			return fJson.optInt ( "instanceCount", 1 );
		}

		@Override
		public Set<String> instances ()
		{
			final TreeSet<String> instanceIds = new TreeSet<> ();
			final JSONObject i = fJson.optJSONObject ( "instances" );
			if ( i != null )
			{
				instanceIds.addAll ( i.keySet () );
			}
			return instanceIds;
		}

		@Override
		public FlowControlDeployedProcess getProcessById ( String instanceName ) throws RequestException, ServiceException
		{
			final JSONObject i = fJson.optJSONObject ( "instances" );
			if ( i != null )
			{
				final JSONObject instData = i.optJSONObject ( instanceName );
				if ( instData != null )
				{
					return new LocalDeployedProcess ( instData );
				}
			}
			return null;
		}

		private final JSONObject fJson;
		private final Encryptor fLdEnc;
	}

	private static final class LocalDeployedProcess implements FlowControlDeployedProcess
	{
		public LocalDeployedProcess ( JSONObject data )
		{
			fJson = data;
		}

		@Override
		public String getProcessId ()
		{
			return fJson.optString ( "pid", null );
		}

		@Override
		public List<String> getLog ( String sinceRfc3339Time ) throws ServiceException, RequestException
		{
			return new LinkedList<> ();
		}

		@Override
		public MetricsCatalog getMetrics ()
		{
			return new NoopMetricsCatalog ();
		}

		private final JSONObject fJson;
	}
}
