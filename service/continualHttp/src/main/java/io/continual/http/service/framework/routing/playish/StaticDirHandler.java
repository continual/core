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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.util.data.StreamTools;

public class StaticDirHandler implements CHttpPlayishRouteHandler
{
	public static final String kMaxAge = StaticFileHandler.kMaxAge;

	public StaticDirHandler ( String routedPath, String staticDirInfo )
	{
		// the format of staticDirInfo is "dir;defaultpage"
		final String[] parts = staticDirInfo.split ( ";" );
		if ( parts.length < 1 ) throw new IllegalArgumentException ( "dir[;defaultpage]" );

		fRoutedPath = routedPath;
		fDir = parts[0];
		if ( parts.length > 1 )
		{
			fDefaultPage = parts[1];
		}
		else
		{
			fDefaultPage = null;
		}
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> args )
	{
		final String path = context.request ().getPathInContext ();
		if ( path == null || path.length() == 0 )
		{
			log.warn ( "404 [" + path + "] no path provided" );
			context.response ().sendError ( 404, "no path provided" );
			return;
		}

		if ( path.contains ( ".." ) )
		{
			log.warn ( "404 [" + path + "] contains parent directory accessor" );
			context.response ().sendError ( 404, path + " was not found on this server." );
			return;
		}

		// here, the path should start with the "routed path" and we want to replace
		// that with the local dir
		if ( !path.startsWith ( fRoutedPath ))
		{
			log.warn ( "404 [" + path + "] does not start with routed path [" + fRoutedPath + "]" );
			context.response ().sendError ( 404, path + " is not a matching path" );
			return;
		}

		final String relPath = path.substring ( fRoutedPath.length () );
		final String newPath = 
			( ( relPath.length () == 0 || relPath.equals ( "/" ) ) && fDefaultPage != null ) ?
			fDir + File.separator + fDefaultPage:
			fDir + File.separator + relPath;

		final URL in = context.getServlet ().findStream ( newPath );

		log.info ( "Path [" + path + "] ==> [" + ( in == null ? "<not found>" : in.toString () ) + "]." );
		if ( in == null )
		{
			context.response ().sendError ( 404, path + " was not found on this server." );
		}
		else
		{
			final String contentType = mapToContentType ( in.toString () );

			// expiry. currently global.
			final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
			if ( cacheMaxAge > 0 )
			{
				context.response().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
			}
			
			try
			{
				final InputStream is = in.openStream ();
				final OutputStream os = context.response ().getStreamForBinaryResponse ( contentType );
				StreamTools.copyStream ( is, os );
			}
			catch ( FileNotFoundException e )
			{
				log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.toString () + ")" );
				context.response ().sendError ( 404, path + " was not found on this server." );
			}
			catch ( IOException e )
			{
				log.warn ( "500 [" + in.toString () + "]: " + e.getMessage () );
				context.response ().sendError ( 500, e.getMessage () );
			}
		}
	}

	private final String fRoutedPath;
	private final String fDir;
	private final String fDefaultPage;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( StaticDirHandler.class );

	static final HashMap<String,String> sfContentTypes = new HashMap<>();
	static
	{
		sfContentTypes.put ( "css", MimeTypes.kCss );

		sfContentTypes.put ( "jpg", MimeTypes.kImageJpg );
		sfContentTypes.put ( "gif", MimeTypes.kImageGif );
		sfContentTypes.put ( "png", MimeTypes.kImagePng );
		sfContentTypes.put ( "ico", MimeTypes.kImageIco );

		sfContentTypes.put ( "htm", MimeTypes.kHtml );
		sfContentTypes.put ( "html", MimeTypes.kHtml );

		sfContentTypes.put ( "js", MimeTypes.kAppJavascript );

		sfContentTypes.put ( "eot", MimeTypes.kFontEot );
		sfContentTypes.put ( "woff", MimeTypes.kFontWoff );
		sfContentTypes.put ( "woff2", MimeTypes.kFontWoff2 );
		sfContentTypes.put ( "otf", MimeTypes.kFontOtf );
		sfContentTypes.put ( "ttf", MimeTypes.kFontTtf );
		sfContentTypes.put ( "svg", MimeTypes.kSvg );

		sfContentTypes.put ( "xml", MimeTypes.kXml );

		sfContentTypes.put ( "sh", MimeTypes.kAppGenericBinary );
	}

	public static String mapToContentType ( String name )
	{
		final int dot = name.lastIndexOf ( "." );
		if ( dot != -1 )
		{
			name = name.substring ( dot + 1 );
		}
		String result = sfContentTypes.get ( name );
		if ( result == null )
		{
			log.warn ( "Unknown content type [" + name + "]. Sending text/plain. (See " + StaticDirHandler.class.getSimpleName () + "::sfContentTypes)" );
			result = "text/plain";
		}
		return result;
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}
}

