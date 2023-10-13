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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.resources.ResourceLoader;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class StaticDirHandler implements CHttpPlayishRouteHandler
{
	public static final String kSetting_CacheMaxAge = StaticFileHandler.kSetting_CacheMaxAge;
	public static final int kDefault_CacheMaxAge = StaticFileHandler.kDefault_CacheMaxAge;

	public StaticDirHandler ( String routedPath, String staticDirInfo )
	{
		this ( routedPath, staticDirInfo, kDefault_CacheMaxAge );
	}

	public StaticDirHandler ( String routedPath, String staticDirInfo, int maxCacheAge )
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

		fCacheMaxAge = maxCacheAge;
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> args ) throws IOException
	{
		final String path = context.request ().getPathInContext ();
		if ( path == null || path.length() == 0 )
		{
			log.warn ( "[" + path + "] no path provided" );
			context.response ().sendError ( HttpStatusCodes.k404_notFound, "no path provided" );
			return;
		}

		if ( path.contains ( ".." ) )
		{
			log.warn ( "[" + path + "] contains parent directory accessor" );
			context.response ().sendError ( HttpStatusCodes.k400_badRequest, path + " was not found on this server." );
			return;
		}

		// here, the path should start with the "routed path" and we want to replace
		// that with the local dir
		if ( !path.startsWith ( fRoutedPath ))
		{
			log.warn ( "[" + path + "] does not start with routed path [" + fRoutedPath + "]" );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, path + " is not a matching path" );
			return;
		}

		final String relPath = path.substring ( fRoutedPath.length () );
		final String newPath = 
			( ( relPath.length () == 0 || relPath.equals ( "/" ) ) && fDefaultPage != null ) ?
			fDir + File.separator + fDefaultPage:
			fDir + File.separator + relPath;

		log.info ( "finding stream [" + newPath + "]" );
		final InputStream is = new ResourceLoader()
			.usingStandardSources ( true, this.getClass () )
			.named ( newPath )
			.load ()
		;

		log.info ( "Path [" + path + "] ==> [" + ( is == null ? "<not found>" : newPath ) + "]." );
		if ( is == null )
		{
			context.response ().sendError ( HttpStatusCodes.k404_notFound, path + " was not found on this server." );
		}
		else
		{
			final String contentType = mapToContentType ( newPath );

			// cache expiration
			if ( fCacheMaxAge > 0 )
			{
				context.response().writeHeader ( "Cache-Control", "max-age=" + fCacheMaxAge, true );
			}
			
			try
			{
				StreamTools.copyStream ( is, context.response ().getStreamForBinaryResponse ( contentType ) );
			}
			finally
			{
				is.close ();
			}
		}
	}

	private final String fRoutedPath;
	private final String fDir;
	private final String fDefaultPage;
	private final int fCacheMaxAge;

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

		sfContentTypes.put ( "txt", MimeTypes.kPlainText );
		sfContentTypes.put ( "log", MimeTypes.kPlainText );

		sfContentTypes.put ( "js", MimeTypes.kAppJavascript );
		sfContentTypes.put ( "json", MimeTypes.kAppJson );

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
			result = MimeTypes.kPlainText;
		}
		return result;
	}

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}
}

