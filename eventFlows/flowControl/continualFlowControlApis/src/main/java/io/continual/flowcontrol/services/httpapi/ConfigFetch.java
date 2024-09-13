package io.continual.flowcontrol.services.httpapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlJobConfig;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;

public class ConfigFetch<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	public ConfigFetch ( ServiceContainer sc, JSONObject config, FlowControlDeploymentService configTransfer ) throws BuildFailure
	{
		super ( sc, config );

		fDeployer = configTransfer;
	}

	public void getConfig ( CHttpRequestContext context, String id ) throws IOException
	{
		try
		{
			// get the deployed job
			final FlowControlDeployment deployment = fDeployer.getDeploymentByConfigKey ( id );
			if ( deployment == null )
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Unknown configuration key." );
				return;
			}
	
			final FlowControlJobConfig fcjc = deployment.getDeploymentSpec ().getJob ().getConfiguration ();
			if ( fcjc == null )
			{
				throw new ServiceException ( "Couldn't retrieve configuration for deployed job." );
			}
	
			try ( final InputStream is = fcjc.readConfiguration () )
			{
				if ( is == null )
				{
					throw new ServiceException ( "Couldn't read configuration for deployed job." );
				}
	
				final CHttpResponse resp = context.response ();
				resp.setStatus ( HttpStatusCodes.k200_ok );
				final OutputStream os = resp.getStreamForBinaryResponse ( fcjc.getDataType () );
				StreamTools.copyStream ( is, os );
				os.close ();
			}
		}
		catch ( RequestException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, x.getMessage() );
		}
		catch ( ServiceException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, x.getMessage() );
		}
	}

	private final FlowControlDeploymentService fDeployer;
}
