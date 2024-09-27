package io.continual.flowcontrol.services.httpapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlService;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlJob;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlJobConfig;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.model.FlowControlJobBuilder;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.flowcontrol.model.FlowControlJobDb.RequestException;
import io.continual.flowcontrol.model.FlowControlJobDb.ServiceException;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AccessException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.services.ServiceContainer;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class FlowControlRoutes<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	public FlowControlRoutes ( ServiceContainer sc, JSONObject config, FlowControlService fcs ) throws BuildFailure
	{
		super ( sc, config );

		fFlowControl = fcs;
	}

	public void getJobs ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final Collection<FlowControlJob> jobs = jobDb.getJobsFor ( fccc );

					final LinkedList<FlowControlJob> jobList = new LinkedList<> ( jobs );
					Collections.sort ( jobList, new Comparator<FlowControlJob> ()
					{
						@Override
						public int compare ( FlowControlJob o1, FlowControlJob o2 )
						{
							return o1.getName ().compareTo ( o2.getName () );
						}
					} );
					sendJson ( context, new JSONObject().put ( "jobs", JsonVisitor.listToArray ( jobList, new ItemRenderer<FlowControlJob,JSONObject> ()
					{
						@Override
						public JSONObject render ( FlowControlJob job ) throws IllegalArgumentException
						{
							return new JSONObject ()
								.put ( "id", job.getId () )
								.put ( "name", job.getName () )
							;
						}
					} ) ) );
				}
				catch ( FlowControlJobDb.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
			}
		} );
	}

	public void getJob ( CHttpRequestContext context, String id ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJob job = jobDb.getJob ( fccc, id );

					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "no such job" );
					}
					else
					{
						sendJson ( context, render ( job ) );
					}
				}
				catch ( FlowControlJobDb.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	public void createJob ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final JSONObject payload = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );

					final String id = UniqueStringGenerator.createUlid ();
					final String name = payload.getString ( "name" );

					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJobBuilder jobBuilder = jobDb.createJobBuilder ( fccc )
						.withId ( id )
						.withName ( name )
						.withOwner ( uc.getUser ().getId () )
						.withAccess ( AccessControlEntry.kOwner, AccessControlList.READ, AccessControlList.UPDATE, AccessControlList.DELETE )
					;

					readJobPayloadInto ( payload, jobBuilder );

					final FlowControlJob job = jobBuilder.build ();
					jobDb.storeJob ( fccc, job.getId (), job );
					sendJson ( context, render ( job ) );
				}
				catch ( FlowControlJobDb.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( FlowControlJobDb.RequestException | BuildFailure | JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	public void patchJob ( CHttpRequestContext context, String name ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final JSONObject payload = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );

					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJob job = jobDb.getJob ( fccc, name );

					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "no such job" );
					}
					else
					{
						final FlowControlJobBuilder jobBuilder = jobDb.createJobBuilder ( fccc )
							.clone ( job )
						;
						final boolean changesMade = readJobPayloadInto ( payload, jobBuilder );
						final FlowControlJob updatedJob = jobBuilder.build ();

						if ( changesMade )
						{
							jobDb.storeJob ( fccc, name, updatedJob );
						}

						sendJson ( context, render ( updatedJob ) );
					}
				}
				catch ( FlowControlJobDb.ServiceException | GeneralSecurityException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( FlowControlJobDb.RequestException | BuildFailure | JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	public void putSecret ( CHttpRequestContext context, String name, String secretId ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJob job = jobDb.getJob ( fccc, name );

					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "no such job" );
					}
					else
					{
						final JSONObject payload = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
						final Object value = payload.opt ( "value" );
						if ( value != null )
						{
							final FlowControlJob jobUpdate = jobDb.createJobBuilder ( fccc )
								.clone ( job )
								.registerSecret ( secretId, value.toString () )
								.build ()
							;
							jobDb.storeJob (
								fccc,
								name,
								jobUpdate
							);
							sendJson ( context, render ( jobUpdate ) );
						}
						else
						{
							sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "A value must be provided." );
							return;
						}
					}
				}
				catch ( FlowControlJobDb.ServiceException | GeneralSecurityException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( FlowControlJobDb.RequestException | BuildFailure  e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process your request: " + e.getMessage () );
				}
				catch ( JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}


	public void deleteSecret ( CHttpRequestContext context, String name, String secretId ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJob job = jobDb.getJob ( fccc, name );

					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "no such job" );
					}
					else
					{
						final FlowControlJob jobUpdate = jobDb.createJobBuilder ( fccc )
							.clone ( job )
							.removeSecretRef ( secretId )
							.build ()
						;
						jobDb.storeJob (
							fccc,
							name,
							jobUpdate
						);
						sendJson ( context, render ( jobUpdate ) );
					}
				}
				catch ( FlowControlJobDb.ServiceException | GeneralSecurityException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( FlowControlJobDb.RequestException | BuildFailure  e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process your request: " + e.getMessage () );
				}
				catch ( JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	private boolean readJobPayloadInto ( JSONObject payload, FlowControlJobBuilder job ) throws RequestException, IOException, ServiceException, JSONException
	{
		boolean changesMade = false;

		// patch the job and store it
		final JSONObject config = payload.optJSONObject ( "config" );
		if ( config != null )
		{
			// the API has the ability to support any mime type for the config but for now just support JSON
			final String mimeType = config.getString ( "type" );
			if ( !mimeType.equalsIgnoreCase ( MimeTypes.kAppJson ) )
			{
				throw new FlowControlJobDb.RequestException ( "Unsupported config type " + mimeType + "." );
			}

			job.setConfiguration ( new FlowControlJobConfig ()
			{
				@Override
				public String getDataType () { return mimeType; }

				@Override
				public InputStream readConfiguration () { return new ByteArrayInputStream ( config.getJSONObject ( "data" ).toString ().getBytes ( StandardCharsets.UTF_8 ) ); }
			} );

			changesMade = true;
		}

		final JSONObject runtime = payload.optJSONObject ( "runtime" );
		if ( runtime != null )
		{
			job.setRuntimeSpec ( new FlowControlRuntimeSpec ()
			{
				@Override
				public String getName () { return runtime.getString ( "name" ); }

				@Override
				public String getVersion () { return runtime.getString ( "version" ); }
			} );
			changesMade = true;
		}

		final JSONObject secrets = payload.optJSONObject ( "secrets" );
		if ( secrets != null )
		{
			JsonVisitor.forEachElement ( secrets, new ObjectVisitor<String,ServiceException> ()
			{
				@Override
				public boolean visit ( String key, String t ) throws JSONException, ServiceException
				{
					try
					{
						job.registerSecret ( key, t );
					}
					catch ( GeneralSecurityException x )
					{
						throw new ServiceException ( x );
					}
					return true;
				}
			} );
			changesMade = true;
		}

		return changesMade;
	}

	public void deleteJob ( CHttpRequestContext context, String id ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					jobDb.removeJob ( fccc, id );

					sendStatusOk ( context, "removed " + id );
				}
				catch ( FlowControlJobDb.RequestException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "Couldn't process your request: " + e.getMessage () );
				}
				catch ( FlowControlJobDb.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	public void createDeployment ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final JSONObject payload = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
					final String jobId = payload.getString ( "job" );
					final int instanceCount = payload.optInt ( "instanceCount", 1 );
					final JSONObject env =  payload.optJSONObject ( "env" );

					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();

					final FlowControlJobDb jobDb = fFlowControl.getJobDb ( fccc );
					final FlowControlJob job = jobDb.getJob ( fccc, jobId );
					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Couldn't load job: " + jobId );
						return;
					}

					final FlowControlDeploymentService api = fFlowControl.getDeployer ( fccc );
					final FlowControlDeployment deploy = api.deploy ( fccc, api.deploymentBuilder()
						.forJob ( job )
						.withInstances ( instanceCount )
						.withEnv ( JsonVisitor.objectToMap ( env ) )
						.build ()
					);

					sendJson ( context, new JSONObject().put ( "deployment", render ( deploy, true ) ) );
				}
				catch ( JSONException | BuildFailure e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( FlowControlJobDb.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control job database." );
				}
				catch ( FlowControlDeploymentService.RequestException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( FlowControlDeploymentService.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't deploy the job." );
				}
				catch ( AccessException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, x.getMessage () );
				}
			}
		} );
	}

	public void getDeployments ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
					final FlowControlDeploymentService deployApi = fFlowControl.getDeployer ( fccc );

					final String jobSpecific = context.request ().getParameter ( "job", null );
					
					final Collection<FlowControlDeployment> deployments =
						jobSpecific == null ?
							deployApi.getDeployments ( fccc ) :
							deployApi.getDeploymentsForJob ( fccc, jobSpecific )
					;

					final JSONObject deploymentContainer = new JSONObject ();
					for ( FlowControlDeployment dd : deployments )
					{
						deploymentContainer.put ( dd.getId (), render ( dd, false ) );
					}
					sendJson ( context, new JSONObject().put ( "deploys", deploymentContainer ) );
				}
				catch ( FlowControlDeploymentService.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control controller." );
				}
				finally
				{
				}
			}
		} );
	}

	public void getDeployment ( CHttpRequestContext context, String deployId ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
					final FlowControlDeploymentService deployApi = fFlowControl.getDeployer ( fccc );

					final FlowControlDeployment deployment = deployApi.getDeployment ( fccc, deployId );

					if ( deployment != null )
					{
						sendJson ( context, new JSONObject().put ( "deployment", render ( deployment, true ) ) );
					}
					else
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Deployment not found." );
					}
				}
				catch ( FlowControlDeploymentService.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't complete your request." );
				}
				finally
				{
				}
			}
		} );
	}

	public void getLogs ( CHttpRequestContext context, String deployId, String instanceId ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k200_ok, "fixme" );

				try
				{
//					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
//					final FlowControlDeploymentService deployApi = fFlowControl.getDeployer ( fccc );

//					final FlowControlDeployment deployment = deployApi.getDeployment ( fccc, deployId );

//					if ( deployment != null )
//					{
//						final String since = context.request ().getParameter ( "since", null );
//						final List<String> lines = deployment.getProcessById ( instanceId ).getLog ( since );
//
//						try ( final PrintWriter pw = context.response ().getStreamForTextResponse ( MimeTypes.kPlainText ) )
//						{
//							for ( String line : lines )
//							{
//								pw.println ( line );
//							}
//						}
//					}
//					else
//					{
//						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Deployment not found." );
//					}
				}
//				catch ( FlowControlDeploymentService.RequestException e )
//				{
//					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
//				}
//				catch ( FlowControlDeploymentService.ServiceException e )
//				{
//					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't complete your request." );
//				}
				finally
				{
				}
			}
		} );
	}

	public void undeploy ( CHttpRequestContext context, String deployId ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
					fFlowControl.getDeployer ( fccc ).undeploy ( fccc, deployId );

					sendStatusOk ( context, "undeployed " + deployId );
				}
				catch ( JSONException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There was a problem with your request: " + e.getMessage () );
				}
				catch ( FlowControlDeploymentService.ServiceException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't deploy the job." );
				}
				finally
				{
				}
			}
		} );
	}

	private static JSONObject render ( FlowControlJob job ) throws JSONException, IOException, ServiceException
	{
		final FlowControlRuntimeSpec runtime = job.getRuntimeSpec ();
		
		return new JSONObject ()
			.put ( "name", job.getName () )
			.put ( "config", render ( job.getConfiguration () ) )
			.put ( "runtime", runtime == null ? null : new JSONObject ()
				.put ( "name", runtime.getName () )
				.put ( "version", runtime.getVersion () ) )
			.put ( "secrets", JsonVisitor.listToArray ( job.getSecretRefs () ) )
		;
	}

	private static JSONObject render ( FlowControlJobConfig config ) throws IOException
	{
		if ( config == null ) return null;

		final JSONObject result = new JSONObject ();

		final String type = config.getDataType ();
		result.put ( "type", type );

		try ( final InputStream is = config.readConfiguration () )
		{
			if ( type.equalsIgnoreCase ( MimeTypes.kAppJson ) )
			{
				result.put ( "data", new JSONObject ( new CommentedJsonTokener ( is ) ) );
			}
			else
			{
				throw new IllegalArgumentException ( "Unsupported config type: " + type );
			}
		}
		return result;
	}

	private static JSONObject render ( FlowControlDeployment deploy, boolean withId ) throws JSONException, IOException
	{
		if ( deploy == null ) return null;

		final JSONObject result = new JSONObject ()
			.put ( "jobId", deploy.getDeploymentSpec ().getJob ().getId () )
		;
		if ( withId )
		{
			result.put ( "id", deploy.getId () );
		}
		return result;
	}

	private final FlowControlService fFlowControl;
}
