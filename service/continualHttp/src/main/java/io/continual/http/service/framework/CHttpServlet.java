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
package io.continual.http.service.framework;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.StdRequest;
import io.continual.http.service.framework.inspection.CHttpObserverMgr;
import io.continual.http.service.framework.inspection.impl.ObserveNoneMgr;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRequestRouter.noMatchingRoute;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.impl.nvInstallTypeWrapper;
import io.continual.util.nv.impl.nvPropertiesFile;
import io.continual.util.nv.impl.nvReadableStack;
import io.continual.util.time.Clock;

/**
 * The base servlet associates a connection object with an HTTP connection. Even
 * session-less servers like a RESTful API have connections -- they're just not
 * stored across calls.
 */
public class CHttpServlet extends HttpServlet
{
	/**
	 * Session life cycle is determined at servlet creation time.
	 */
	public enum SessionLifeCycle
	{
		/**
		 * No session data is stored on the server for the client.
		 */
		NO_SESSION,

		/**
		 * The server stores a full session for the client. The session eventually
		 * expires if not explicitly removed.
		 */
		FULL_SESSION
	}

	/**
	 * Construct a servlet with default settings and the "no session" session life cycle.
	 */
	public CHttpServlet ()
	{
		this ( SessionLifeCycle.NO_SESSION );
	}

	/**
	 * Construct a servlet with default settings, and the specified session life cycle.
	 */
	public CHttpServlet ( String prefsFileName )
	{
		this ( prefsFileName, SessionLifeCycle.NO_SESSION );
	}

	/**
	 * Construct a servlet with default settings, and the specified session life cycle.
	 */
	public CHttpServlet ( SessionLifeCycle slc )
	{
		this ( null, slc );
	}

	/**
	 * Construct a servlet with settings from a named file, and the specified
	 * session life cycle.
	 * 
	 * @param prefsFileName a settings (preferences) file name
	 * @param slc a session life cycle
	 */
	public CHttpServlet ( String prefsFileName, SessionLifeCycle slc )
	{
		this ( null, prefsFileName, slc, null, null );
	}

	/**
	 * Construct a servlet with settings from a named file, and the specified
	 * session life cycle.
	 * 
	 * @param settings settings
     * @param addlSettingsFileName an additional settings file
	 * @param slc the session life cycle
	 */
	public CHttpServlet ( NvReadable settings, String addlSettingsFileName, SessionLifeCycle slc, MetricsCatalog metrics, CHttpObserverMgr inspector )
	{
		fWebInfDir = null;
		fProvidedPrefs = settings;
		fPrefsConfigFilename = addlSettingsFileName;
		fSessionLifeCycle = slc;
		fRouter = null;
		fObjects = new HashMap<>();
		fSearchDirs = new LinkedList<>();
		fRuntimeControls = new CHttpRuntimeControls ();
		fMetrics = metrics;
		fInspector = inspector != null ? inspector : new ObserveNoneMgr();
	}

	/**
	 * Initialize the servlet (called by the servlet container).
	 */
	@Override
	public final void init ( ServletConfig sc ) throws ServletException
	{
		super.init ( sc );

		for ( String msg : CHttpVersionInfo.getTitleAndCopyright () )
		{
			log.info ( msg );
		}

		// find the WEB-INF dir
		final String basePath = sc.getServletContext ().getRealPath ( "/" );
		if ( basePath == null )
		{
			throw new ServletException ( "Couldn't get the base path from '/'. Container returned null." );
		}

		log.info ( "working dir = " + System.getProperty("user.dir") );
		log.info ( "servlet class: " + this.getClass ().getName() );
		log.info ( "real path of '/' = " + basePath );

		// make the settings
		final nvReadableStack settingsStack = new nvReadableStack ();
//		settingsStack.push ( new nvJvmProperties () );
		settingsStack.push ( new CHttpServletSettings ( sc ) );
		if ( fProvidedPrefs != null )
		{
			settingsStack.push ( fProvidedPrefs );
		}

		// find the base webapp directory, normally "WEB-INF" in classic servlets
		final String webappDirName = settingsStack.getString ( kSetting_BaseWebAppDir,
			new File ( basePath, "WEB-INF" ).getAbsolutePath () );
		final File checkDir = new File ( webappDirName );
		if ( checkDir.exists () )
		{
			fWebInfDir = checkDir;
		}
		else
		{
			log.info ( "Can't find the webapp's base directory. Used '" + webappDirName + "'." );
		}

		// get additional search dirs
		final String searchDirString = settingsStack.getString ( kSetting_SearchDirs, null );
		if ( searchDirString != null )
		{
			log.info ( "config search dirs: " + searchDirString );
			final String searchDirs[] = searchDirString.split ( ":" );
			for ( String searchDir : searchDirs )
			{
				addToFileSearchDirs ( new File ( searchDir ) );
			}
		}
		else
		{
			log.info ( kSetting_SearchDirs + " is not set. (Typically set in web.xml)" );
		}

		if ( fPrefsConfigFilename != null && fPrefsConfigFilename.length() > 0 )
		{
			try
			{
				log.info ( "finding config stream named [" + fPrefsConfigFilename + "]." );
				final URL configFile = findStream ( fPrefsConfigFilename );
				if ( configFile != null )
				{
					log.info ( "chose stream [" + configFile.toString () + "]." );
					final NvReadable filePrefs = new nvPropertiesFile ( configFile );
					settingsStack.push ( filePrefs );
				}
				else
				{
					log.warn ( "could not find config stream." );
				}
			}
			catch ( NvReadable.LoadException e )
			{
				log.warn (  "Couldn't load settings from [" + fPrefsConfigFilename + "]." );
			}
		}
		else
		{
			log.info ( "no preferences file specified to " + getClass().getSimpleName() + "'s constructor." );
		}

		// add the runtime control settings to the settings stack
		settingsStack.push ( fRuntimeControls );
		
		// put a wrapper on the top-level settings object to allow for
		// installation-type specific settings.
		final NvReadable appLevelSettings = makeSettings ( settingsStack );
		fSettings = new nvInstallTypeWrapper ( appLevelSettings );

		// routing setup
		fRouter = new CHttpRequestRouter ();
		
		// app-level setup
		try
		{
			log.info ( "Calling app servlet setup." );
			servletSetup ();
		}
		catch ( NvReadable.MissingReqdSettingException e )
		{
			log.error ( "Shutting down due to missing setting. " + e.getMessage () );
			throw new ServletException ( e );
		}
		catch ( NvReadable.InvalidSettingValueException e )
		{
			log.error ( "Shutting down due to invalid setting. " + e.getMessage () );
			throw new ServletException ( e );
		}

		log.info ( "Servlet is ready." );
	}

	@Override
	public final void destroy ()
	{
		super.destroy ();
		try
		{
			servletShutdown ();
		}
		catch ( Exception x )
		{
			log.error ( "During tear-down: " + x.getMessage () );
		}
	}

	/**
	 * Find the named resource and return an InputStream for it. This is related to findFile(), but
	 * is built to be more general. Use this if you don't actually require a file on disk.<br>
	 * <br>
	 * 1. If the JVM system properties include a setting with the key specified by kJvmSetting_FileRoot, look
	 * for the file relative to that path.<br>
	 * 2. Try the system's findResource() call.
	 * 3. Try findFile()
	 * <br>
	 * @param resourceName
	 * @return an InputStream, or null
	 */
	public URL findStream ( String resourceName )
	{
		try
		{
			// first try it as an absolute file name
			File file = new File ( resourceName );
			if ( file.isAbsolute () && file.exists () )
			{
				return file.toURI().toURL();
			}

			// next try the file root setting, which takes precedence
			final String filesRoot = System.getProperty ( kJvmSetting_FileRoot, null );
			if ( filesRoot != null )
			{
				final String fullPath = filesRoot + "/" + resourceName;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
				if ( file.exists () )
				{
					return file.toURI().toURL();
				}
			}

			// next try the class's resource finder
			URL res = getClass().getClassLoader().getResource ( resourceName );
			if ( res != null )
			{
				return res;
			}

			// now try the system class loaders' resource finder
			res = ClassLoader.getSystemResource ( resourceName );
			if ( res != null )
			{
				return res;
			}

			// finally, do the regular file search
			final File f = findFile ( resourceName );
			if ( f.exists () )
			{
				final URI u = f.toURI ();
				return u.toURL ();
			}
		}
		catch ( MalformedURLException e )
		{
			log.warn ( "Unexpected failure to convert a local filename into a URL: " + e.getMessage () );
		}

		return null;
	}
	
	/**
	 * Find a file given a file name. If the name is absolute, the file is returned. Otherwise,
	 * the file is located using this search path:<br>
	 * <br>
	 * 1. If the JVM system properties include a setting with the key specified by kJvmSetting_FileRoot, look
	 * for the file relative to that path.<br>
	 * 2. If not yet found, look for the file relative to the servlet's WEB-INF directory, if that exists.<br>
	 * 3. If not yet found, look for the file relative to the servlet's real path for "/". (Normally inside the war.)<br>
	 * 4. If not yet found, check each app-provided search directory. <br>
	 * 4. If not yet found, return a File with the relative path as-is. (This does not mean the file exists!) 
	 *  
	 * @param appRelativePath
	 * @return a File
	 */
	public File findFile ( String appRelativePath )
	{
		File file = new File ( appRelativePath );
		if ( !file.isAbsolute () )
		{
			final String filesRoot = System.getProperty ( kJvmSetting_FileRoot, null );
			if ( filesRoot != null )
			{
				final String fullPath = filesRoot + "/" + appRelativePath;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
			}

			// check in WEB-INF	(FIXME: using a member variable; think about thread synchronization)
			if ( !file.exists () && fWebInfDir != null )
			{
				file = new File ( fWebInfDir, appRelativePath );
				log.debug ( "Looking for [" + file.getAbsolutePath() + "]." );
			}

			// check in webapp's "/"
			if ( !file.exists () )
			{
				final String basePath = super.getServletContext ().getRealPath ( "/" );
				final String fullPath = basePath + ( basePath.endsWith ( "/" ) ? "" : "/" ) + appRelativePath;
				log.debug ( "Looking for [" + fullPath + "]." );
				file = new File ( fullPath );
			}

			// check search dirs specified by app
			if ( !file.exists () )
			{
				for ( File dir : fSearchDirs )
				{
					final File candidate = new File ( dir, appRelativePath );
					log.debug ( "Looking for [" + candidate.getAbsolutePath () + "]." );
					if ( candidate.exists () )
					{
						file = candidate;
						break;
					}
				}
			}
			
			if ( !file.exists () )
			{
				file = new File ( appRelativePath );
			}
		}
		log.debug ( "Given [" + appRelativePath + "], using file [" + file.getAbsolutePath () + "]." );
		return file;
	}

	/**
	 * Get settings in use by this servlet. They can come from the servlet container, from an
	 * optional config file named by the string provided to the constructor, and anything else
	 * the servlet init code (in the concrete class) decides to add.
	 * 
	 * @return settings
	 */
	public NvReadable getSettings ()
	{
		return fSettings;
	}

	/**
	 * Get servlet controls. (These are settings that don't get cached.)
	 * @return
	 */
	public CHttpRuntimeControls getControls ()
	{
		return fRuntimeControls;
	}

	/**
	 * Put an object into the servlet's directory by name.
	 * @param key
	 * @param o
	 */
	public void putObject ( String key, Object o )
	{
		fObjects.put ( key, o );
	}

	/**
	 * Get an object from the servlet's directory by name. If none is found, null is returned.
	 * @param key
	 * @return a previously stored object, or null.
	 */
	public Object getObject ( String key )
	{
		return fObjects.get ( key );
	}

	/**
	 * Create a session.
	 * @return a session.
	 * @throws NvReadable.MissingReqdSettingException
	 */
	public CHttpConnection createSession () throws NvReadable.MissingReqdSettingException
	{
		return null;
	}

	/**
	 * Get the servlet's request router.
	 * @return a request router.
	 */
	public CHttpRequestRouter getRequestRouter ()
	{
		return fRouter;
	}

	/**
	 * Override this to take the settings built by the base servlet and return
	 * something wrapping them (or different, even)
	 * @param fromBase
	 * @return a settings object
	 */
	protected NvReadable makeSettings ( NvReadable fromBase )
	{
		return fromBase;
	}

	/**
	 * Add a directory to the file search directory path.
	 * @param dir
	 */
	protected synchronized void addToFileSearchDirs ( File dir )
	{
		if ( dir.exists() && dir.isDirectory () )
		{
			fSearchDirs.add ( dir );
		}
		else
		{
			log.warn ( "File [" + dir.toString () + "] is not a directory. Ignored." );
		}
	}

	/**
	 * Called at the end of servlet initialization. Override servletSetup to do custom
	 * init work in your servlet.
	 * @throws NvReadable.MissingReqdSettingException
	 * @throws NvReadable.InvalidSettingValueException
	 * @throws ServletException
	 */
	protected void servletSetup () throws NvReadable.MissingReqdSettingException, NvReadable.InvalidSettingValueException, ServletException {}

	/**
	 * override servletShutdown to do custom shutdown work in your servlet. Note that this isn't always called,
	 * depending on the servlet container.
	 */
	protected void servletShutdown () {}

	@Override
	protected final void service ( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, java.io.IOException
	{
		final long startMs = Clock.now ();
		final String clientIp = StdRequest.getBestRemoteAddress ( req );
		final String reqId = clientIp + " " + req.getMethod () + " " + req.getRequestURI ();
		log.debug ( "start " + reqId );

		if ( getSettings().getBoolean ( CHttpRuntimeControls.kSetting_LogHeaders, false ) )
		{
			log.info ( "--" );
			log.info ( "REQUEST from " + req.getRemoteHost () + " (" + req.getRemoteAddr () + "):" );
			log.info ( "    " + req.getMethod () + " " + req.getPathInfo () + " " + req.getQueryString () );
			log.info ( "" );

			final Enumeration<?> e = req.getHeaderNames ();
			while ( e.hasMoreElements () )
			{
				final String name = e.nextElement ().toString ();
				final String val = req.getHeader ( name );
				log.info ( "    " + name + ": " + val );
			}
			log.info ( "--" );
		}

		final CHttpConnection session = getSession ( req );

		final CHttpRequestContext ctx = createHandlingContext ( req, resp, session, fObjects, fRouter );
		fInspector.consider ( ctx );

		final CHttpRequest reqObj = ctx.request ();
		Path pathAsMetricName = getMetricNamer().getMetricNameFor ( reqObj );

		try
		{
			final CHttpRouteInvocation handler = fRouter.route ( reqObj, session );
			pathAsMetricName = handler.getRouteNameForMetrics ();

			final Timer.Context timer = fMetrics == null ? null : fMetrics.timer ( pathAsMetricName.makeChildItem ( Name.fromString ( "executionTime" ) ) ).time ();
			try
			{
				handler.run ( ctx );
			}
			finally
			{
				if ( timer != null ) timer.stop ();
			}
		}
		catch ( noMatchingRoute e )
		{
			onError ( ctx, e, new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
				{
					sendStdJsonError ( ctx, HttpStatusCodes.k404_notFound, "Not found." );
				}
			} );

			// record the mismatched route but not the actual route and status code so that we're not polluting
			// the metrics catalog with garbage input paths
			pathAsMetricName = null;
			if ( fMetrics != null )
			{
				fMetrics
					.meter ( Path.fromString ( "/noMatchForMethodAndPath" ) )
					.mark ()
				;
			}
		}
		catch ( InvocationTargetException x )
		{
			final Throwable t = x.getCause ();
			if ( t != null )
			{
				onError ( ctx, t, null );
			}
			else
			{
				onError ( ctx, x, null );
			}
		}
		catch ( Throwable t )
		{
			onError ( ctx, t, null );
		}

		final long endMs = Clock.now ();
		final long durationMs = endMs - startMs;
		final int returnedStatusCode = ctx.response ().getStatusCode ();
		log.info ( "{} {} {} ms", reqId, returnedStatusCode, durationMs );

		ctx.close ();
		
		if ( fMetrics != null && pathAsMetricName != null )
		{
			fMetrics
				.meter ( pathAsMetricName
					.makeChildItem ( Name.fromString ( "statusCode" ) )
					.makeChildItem ( Name.fromString ( "" + returnedStatusCode ) )
				)
				.mark ()
			;
		}
	}

	/**
	 * Override this to create a custom handling context for your request handlers.
	 * @param req
	 * @param resp
	 * @param dc
	 * @param objects
	 * @param rr
	 * @return
	 */
	protected CHttpRequestContext createHandlingContext ( HttpServletRequest req, HttpServletResponse resp,
		CHttpConnection dc, HashMap<String,Object> objects, CHttpRequestRouter rr )
	{
		return new CHttpRequestContext ( this, req, resp, dc, objects, rr );
	}

	private void sendStdJsonError ( CHttpRequestContext ctx, int err, String msg )
	{
		ctx.response ().sendErrorAndBody ( err,
			new JSONObject()
				.put ( "statusCode", err )
				.put ( "status", msg )
				.toString (4),
			MimeTypes.kAppJson );
	}

	private void onError ( CHttpRequestContext ctx, Throwable t, CHttpErrorHandler defHandler )
	{
		CHttpErrorHandler eh = fRouter.route ( t );
		if ( eh == null && defHandler != null )
		{
			eh = defHandler;
		}

		if ( eh != null )
		{
			try
			{
				eh.handle ( ctx, t );
			}
			catch ( Throwable tt )
			{
				log.warn ( "Error handler failed, handling a " + t.getClass().getName() + ", with " + tt.getMessage () );
				sendStdJsonError ( ctx, HttpStatusCodes.k500_internalServerError, t.getMessage () );
			}
		}
		else
		{
			log.warn ( "No handler defined for " + t.getClass().getName() + ". Sending 500." );
			sendStdJsonError ( ctx, HttpStatusCodes.k500_internalServerError, t.getMessage () );

			final StringWriter sw = new StringWriter ();
			final PrintWriter pw = new PrintWriter ( sw );
			t.printStackTrace ( pw );
			pw.close ();
			log.warn ( sw.toString () );
		}
	}

	private String getSessionIdFromCookie ( final HttpServletRequest req )
	{
		final Cookie[] cookies = req.getCookies ();
		if ( cookies != null )
		{
			for ( Cookie c : req.getCookies () )
			{
				if ( c.getName ().equals ( "JSESSIONID" ) )
				{
					return c.getValue ();
				}
			}
		}
		return null;
	}
	
	private CHttpConnection getSession ( final HttpServletRequest req ) throws ServletException
	{
		CHttpConnection result = null;
		if ( !fSessionLifeCycle.equals ( SessionLifeCycle.NO_SESSION ) )
		{
			try
			{
				final String servletSessionName = getSessionObjectName ( this.getClass () );

				// FIXME: on a server change (or restart), the browser will ask for a bunch of static files
				// using the old session cookie. This code creates a new session for each. We don't necessarily
				// even need a session for these. The session setup should include a path regex to say
				// whether a session should be created on the response.
				
				final String sessionCookieWas = getSessionIdFromCookie ( req );
				log.debug ( "Session ID from request cookie: " + sessionCookieWas );

				final HttpSession session = req.getSession ( true );
				log.debug ( "Session ID on response session: " + session.getId () );

				// locate the last session
				result = (CHttpConnection) session.getAttribute ( servletSessionName );

				if ( result == null )
				{
					result = createSession ();
					if ( result != null )
					{
						session.setAttribute ( servletSessionName, result );
						result.onSessionCreate ( this, new CHttpConnectionContext ()
						{
							@Override
							public void setInactiveExpiration ( long units, TimeUnit tu )
							{
								final long timeInSeconds = TimeUnit.SECONDS.convert ( units, tu );
								if ( timeInSeconds < 0 || timeInSeconds > Integer.MAX_VALUE )
								{
									throw new IllegalArgumentException ( "Invalid time specification." );
								}
								final int timeInSecondsInt = (int) timeInSeconds;
								session.setMaxInactiveInterval ( timeInSecondsInt );
							}

							@Override
							public String getRemoteAddress ( boolean actual )
							{
								return actual ? StdRequest.getActualRemoteAddress ( req ): StdRequest.getBestRemoteAddress ( req );
							}
						} );
					}
				}
	
				if ( result != null )
				{
					result.noteActivity ();
				}
			}
			catch ( NvReadable.MissingReqdSettingException e )
			{
				throw new ServletException ( e );
			}
		}
		return result;
	}

	private static String getSessionObjectName ( Class<?> c )
	{
		return kWebSessionObject + c.getName ();
	}

	protected CHttpMetricNamer getMetricNamer ()
	{
		return new CHttpMetricNamer ()
		{
			@Override
			public Path getMetricNameFor ( CHttpRequest req )
			{
				return Path.fromString ( "/" + req.getMethod () + " " + req.getPathInContext ().replaceAll ( "\\.", "%2E" ) );
			}
		};
	}

	private NvReadable fSettings;
	private final CHttpRuntimeControls fRuntimeControls;
	private File fWebInfDir;
	private final NvReadable fProvidedPrefs;
	private final String fPrefsConfigFilename;
	private final LinkedList<File> fSearchDirs;
	private final SessionLifeCycle fSessionLifeCycle;
	private CHttpRequestRouter fRouter;
	private final HashMap<String,Object> fObjects;
	private final MetricsCatalog fMetrics;
	private final CHttpObserverMgr fInspector;

	public static final String kJvmSetting_FileRoot = "CHTTP_FILES";
	public static final String kSetting_BaseWebAppDir = "chttp.webapp.base"; 
	private static final String kWebSessionObject = "chttp.session.";
	public static final String kSetting_BaseTemplateDir = "chttp.templates.path";
	private static final String kSetting_SearchDirs = "chttp.config.search.dirs";
	private static final long serialVersionUID = 1L;

	private static Logger log = LoggerFactory.getLogger ( CHttpServlet.class );
}
