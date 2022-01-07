package io.continual.flowcontrol.endpoints;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.controlapi.ConfigTransferService.ServiceException;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.identity.Identity;
import io.continual.restHttp.ApiContextHelper;
import io.continual.util.data.StreamTools;

public class ConfigFetch<I extends Identity> extends ApiContextHelper<I>
{
	public ConfigFetch ( ConfigTransferService configTransfer )
	{
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
