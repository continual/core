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
import java.io.InputStream;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.resources.ResourceLoader;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;

public class StaticFileHandler implements CHttpPlayishRouteHandler
{
	public static final String kSetting_CacheMaxAge = "chttp.staticFile.cache.maxAgeSeconds";
	public static final int kDefault_CacheMaxAge = -1;

	public StaticFileHandler ( String routedPath, String staticFile )
	{
		this ( routedPath, staticFile, kDefault_CacheMaxAge );
	}

	public StaticFileHandler ( String routedPath, String staticFile, int cacheMaxAge )
	{
		String file = staticFile.endsWith ( "/" ) ? ( staticFile + routedPath ) : staticFile;
		file = file.replaceAll ( "//", "/" );

		fFile = file;
		fContentType = StaticDirHandler.mapToContentType ( fFile );
		fCacheMaxAge = cacheMaxAge;
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> args ) throws IOException
	{
		// cache expiration
		if ( fCacheMaxAge > 0 )
		{
			context.response().writeHeader ( "Cache-Control", "max-age=" + fCacheMaxAge, true );
		}

		log.info ( "finding stream [" + fFile + "]" );
		final InputStream is = new ResourceLoader()
			.usingStandardSources ( true, this.getClass () )
			.named ( fFile )
			.load ()
		;
		if ( is == null )
		{
			log.warn ( "404 [" + fFile + "] not found" );
			context.response ().sendError ( HttpStatusCodes.k404_notFound, fFile + " was not found on this server." );
		}
		else
		{
			try
			{
				StreamTools.copyStream ( is, context.response ().getStreamForBinaryResponse ( fContentType ) );
			}
			finally
			{
				is.close ();
			}
		}
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}

	private final String fFile;
	private final String fContentType;
	private final int fCacheMaxAge;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticFileHandler.class );
}
