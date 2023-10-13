package io.continual.flowcontrol.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.controlapi.ConfigTransferService.ServiceException;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class ConfigFetch<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	public ConfigFetch ( ServiceContainer sc, JSONObject config, ConfigTransferService configTransfer ) throws BuildFailure
	{
		super ( sc, config );

		fConfigTransfer = configTransfer;
	}

	public void getConfig ( CHttpRequestContext context, String id ) throws IOException, ServiceException
	{
		try ( final InputStream is = fConfigTransfer.fetch ( id ) )
		{
			if ( is == null )
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Unknown configuration key." );
				return;
			}

			final CHttpResponse resp = context.response ();
			resp.setStatus ( HttpStatusCodes.k200_ok );
			final OutputStream os = resp.getStreamForBinaryResponse ( MimeTypes.kAppGenericBinary );
			StreamTools.copyStream ( is, os );
			os.close ();
		}
	}

	private final ConfigTransferService fConfigTransfer;
}
