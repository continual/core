package io.continual.flowcontrol.endpoints;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.json.JSONObject;

import io.continual.flowcontrol.FlowControlApi;
import io.continual.flowcontrol.FlowControlApi.FlowControlApiException;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlJob;
import io.continual.flowcontrol.FlowControlService;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.ApiContextHelper;
import io.continual.restHttp.HttpServlet;
import io.continual.util.data.json.JsonVisitor;

public class FlowControlRoutes<I extends Identity> extends ApiContextHelper<I>
{
	public FlowControlRoutes ( IamServiceManager<I, ?> accts, FlowControlService fcs )
	{
		super ( accts );

		fFlowControl = fcs;
	}

	public void getJobs ( CHttpRequestContext context ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, HttpServlet servlet, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
					final FlowControlApi api = fFlowControl.getApiFor ( fccc );
					final Collection<FlowControlJob> jobs = api.getAllJobs ();

					final LinkedList<String> jobNames = new LinkedList<> ();
					for ( FlowControlJob job : jobs )
					{
						jobNames.add ( job.getName () );
					}
					Collections.sort ( jobNames );
					sendJson ( context, new JSONObject().put ( "jobs", JsonVisitor.listToArray ( jobNames ) ) );
				}
				catch ( FlowControlApiException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control service." );
				}
			}
		} );
	}

	public void getJob ( CHttpRequestContext context, String id ) throws IamSvcException
	{
		handleWithApiAuthAndAccess ( context, new ApiHandler<I> ()
		{
			@Override
			public void handle ( CHttpRequestContext context, HttpServlet servlet, UserContext<I> uc )  throws IOException
			{
				try
				{
					final FlowControlCallContext fccc = fFlowControl.createtContextBuilder ().asUser ( uc.getUser () ).build ();
					final FlowControlApi api = fFlowControl.getApiFor ( fccc );
					final FlowControlJob job = api.getJob ( id );
					if ( job == null )
					{
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "no such job" );
					}
					else
					{
						sendJson ( context, render ( job ) );
					}
				}
				catch ( FlowControlApiException e )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "Couldn't access the flow control service." );
				}
			}
		} );
	}

	private static JSONObject render ( FlowControlJob job )
	{
		return new JSONObject ()
			.put ( "name", job.getName () )
		;
	}
	
	private final FlowControlService fFlowControl;
}
