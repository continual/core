/*
/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.service.framework.routing.playish;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.util.data.StreamTools;

import io.continual.http.service.framework.context.CHttpRequestContext;

public class StaticFileHandler implements CHttpPlayishRouteHandler
{
	public static final String kMaxAge = "chttp.staticFile.cache.maxAgeSeconds";

	public StaticFileHandler ( String routedPath, String staticFile )
	{
		String file = staticFile.endsWith ( "/" ) ? ( staticFile + routedPath ) : staticFile;
		file = file.replaceAll ( "//", "/" );

		fFile = file;
		fContentType = StaticDirHandler.mapToContentType ( fFile );
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> args ) throws IOException
	{
		// expiry. currently global.
		final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
		if ( cacheMaxAge > 0 )
		{
			context.response().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
		}

		log.info ( "finding stream [" + fFile + "]" );
		final URL f = context.getServlet ().findStream ( fFile );
		if ( f == null )
		{
			log.warn ( "404 [" + fFile + "] not found" );
			context.response ().sendError ( 404, fFile + " was not found on this server." );
		}
		else
		{
			StreamTools.copyStream (
				f.openStream (),
				context.response ().getStreamForBinaryResponse ( fContentType )
			);
		}
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}

	private final String fFile;
	private final String fContentType;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticFileHandler.class );
}
