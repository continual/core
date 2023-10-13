package io.continual.http.app.servers.endpoints;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class FileServer<I extends Identity> extends TypicalUiEndpoint<I>
{
	public FileServer ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final String baseDir = sc.getExprEval ().evaluateText ( config.getString ( "baseDir" ) );
		fBaseDir = new File ( baseDir );
		if ( !fBaseDir.isDirectory () )
		{
			throw new BuildFailure ( baseDir + " is not a directory." );
		}
	}

	public void getFile ( CHttpRequestContext context, String path )
	{
		// only serve paths below our base dir
		if ( path.contains ( ".." ) || path.startsWith ( File.pathSeparator ) )
		{
			context.response ().sendError ( HttpStatusCodes.k404_notFound, "Couldn't find " + path );
			return;
		}

		final File f = new File ( fBaseDir, path );
		if ( !f.exists () )
		{
			context.response ().sendError ( HttpStatusCodes.k404_notFound, "Couldn't find " + path );
			return;
		}

		// transfer the file
		try
		{
			final CHttpResponse r = context.response();
			r.setStatus ( HttpStatusCodes.k200_ok );
			r.setContentType ( MimeTypes.kAppGenericBinary );

			final OutputStream os = context.response ().getStreamForBinaryResponse ();
			StreamTools.copyStream ( new FileInputStream ( f ), os );
			os.close ();
		}
		catch ( FileNotFoundException e )
		{
			context.response ().sendError ( HttpStatusCodes.k404_notFound, "Couldn't find " + path );
		}
		catch ( IOException e )
		{
			context.response ().sendError ( HttpStatusCodes.k503_serviceUnavailable, "Couldn't read " + path );
		}
	}

	private File fBaseDir;
}
