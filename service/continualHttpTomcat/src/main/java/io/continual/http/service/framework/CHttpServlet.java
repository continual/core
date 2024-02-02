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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

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

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpFilter.Disposition;
import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.ServletRequestContext;
import io.continual.http.service.framework.context.ServletRequestTools;
import io.continual.http.service.framework.inspection.CHttpObserverMgr;
import io.continual.http.service.framework.inspection.impl.ObserveNoneMgr;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRequestRouter.noMatchingRoute;
import io.continual.http.service.framework.sessions.CHttpUserSession;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.iam.IamService;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.data.HumanReadableHelper;
import io.continual.util.legal.CopyrightGenerator;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.impl.nvInstallTypeWrapper;
import io.continual.util.nv.impl.nvJsonObject;
import io.continual.util.nv.impl.nvReadableStack;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;
import io.continual.util.time.Clock;

/**
 * The base servlet associates a connection object with an HTTP connection. Even
 * session-less servers like a RESTful API have connections -- they're just not
 * stored across calls.
 */
public class CHttpServlet extends HttpServlet
{
	private static final String kSetting_SessionTimeout = "sessionDuration";
	private static final String kDefault_SessionTimeout = "14d";
	
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
	 * @throws BuildFailure 
	 */
	public CHttpServlet () throws BuildFailure
	{
		this ( SessionLifeCycle.NO_SESSION );
	}

	/**
	 * Construct a servlet with default settings, and the specified session life cycle.
	 * @throws BuildFailure 
	 */
	public CHttpServlet ( SessionLifeCycle slc ) throws BuildFailure
	{
		this ( new JSONObject (), slc, null, null, null );
	}

	/**
	 * Construct a servlet with settings from a named file, and the specified
	 * session life cycle.
	 * 
	 * @param settings settings
	 * @param slc the session life cycle
	 * @param metrics 
	 * @param inspector
	 * @param accounts
	 * @throws BuildFailure 
	 */
	public CHttpServlet ( JSONObject settings, SessionLifeCycle slc, MetricsCatalog metrics, CHttpObserverMgr inspector, IamService<?,?> accounts ) throws BuildFailure
	{
		fProvidedPrefs = settings;
		fRouter = null;
		fMetrics = metrics;
		fInspector = inspector != null ? inspector : new ObserveNoneMgr();
		fRouters = new LinkedList<> ();
		fFilters = new LinkedList<> ();

		final JSONObject namer = settings != null ? settings.optJSONObject ( "metricsNamer" ) : null;
		if ( namer != null )
		{
			fMetricNamer = Builder.withBaseClass ( CHttpMetricNamer.class )
				.withClassNameInData ()
				.usingData ( namer )
				.build ()
			;
		}
		else
		{
			fMetricNamer = null;
		}

		fAccounts = accounts;

		// session lifecycle and timeout
		fSessionLifeCycle = slc;

		final int sessionDuration = (int) HumanReadableHelper.parseDuration ( settings.optString ( kSetting_SessionTimeout, kDefault_SessionTimeout ) );
		if ( sessionDuration < 0 || sessionDuration > Integer.MAX_VALUE )
		{
			throw new IllegalArgumentException ( "Invalid time specification." );
		}
		fSessionTimeInSeconds = sessionDuration * 1000;
	}

	/**
	 * Initialize the servlet (called by the servlet container).
	 */
	@Override
	public final void init ( ServletConfig sc ) throws ServletException
	{
		super.init ( sc );

		// produce some identifying information
		for ( String msg : CopyrightGenerator.getStandardNotice ().getCopyrightNotices () )
		{
			log.info ( msg );
		}

		// make the settings
		final nvReadableStack settingsStack = new nvReadableStack ();
//		settingsStack.push ( new nvJvmProperties () );
		settingsStack.push ( new CHttpServletSettings ( sc ) );
		if ( fProvidedPrefs != null )
		{
			settingsStack.push ( new nvJsonObject ( fProvidedPrefs ) );
		}

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
		catch ( NvReadable.MissingReqdSettingException | NvReadable.InvalidSettingValueException e )
		{
			log.error ( "Shutting down due to missing setting. " + e.getMessage () );
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
	 * Add a route installer
	 * @param value
	 */
	public CHttpServlet addRouter ( CHttpRouteInstaller value )
	{
		fRouters.add ( value );
		return this;
	}

	/**
	 * Add a filter
	 * @param value
	 */
	public CHttpServlet addFilter ( CHttpFilter value )
	{
		fFilters.add ( value );
		return this;
	}

	/**
	 * Get settings in use by this servlet. They can come from the servlet container, from an
	 * optional config file named by the string provided to the constructor, and anything else
	 * the servlet init code (in the concrete class) decides to add.
	 * 
	 * @return settings
	 * @deprecated Make settings directly in handling components
	 */
	@Deprecated
	public NvReadable getSettings ()
	{
		return fSettings;
	}

	/**
	 * Create a session.
	 * @return a session.
	 * @throws NvReadable.MissingReqdSettingException
	 */
	public CHttpUserSession createSession () throws NvReadable.MissingReqdSettingException
	{
		return new CHttpUserSession ( fAccounts );
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
	 * Get the accounts service, which can be null
	 * @return the accounts service
	 */
	public IamService<?,?> getAccounts ()
	{
		return fAccounts;
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
	 * Called at the end of servlet initialization. Override servletSetup to do custom
	 * init work in your servlet.
	 * @throws NvReadable.MissingReqdSettingException
	 * @throws NvReadable.InvalidSettingValueException
	 * @throws ServletException
	 */
	protected void servletSetup () throws NvReadable.MissingReqdSettingException, NvReadable.InvalidSettingValueException, ServletException
	{
		try
		{
			final NvReadable p = getSettings ();

			// setup request routing
			final CHttpRequestRouter rr = getRequestRouter ();
			for ( CHttpRouteInstaller router : fRouters )
			{
				router.setupRouter ( rr, p );
			}

			log.info ( "The server is ready." );
		}
		catch ( IOException | BuildFailure e )
		{
			throw new ServletException ( e );
		}
	}

	/**
	 * override servletShutdown to do custom shutdown work in your servlet. Note that this isn't always called,
	 * depending on the servlet container.
	 */
	protected void servletShutdown () {}

	@Override
	protected final void service ( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, java.io.IOException
	{
		final long startMs = Clock.now ();
		final String clientIp = ServletRequestTools.getBestRemoteAddress ( req );
		final String reqId = clientIp + " " + req.getMethod () + " " + req.getRequestURI ();
		log.debug ( "start " + reqId );

		final CHttpSession session = getSession ( req );

		final ServletRequestContext ctx = createHandlingContext ( req, resp, session, fRouter );
		fInspector.consider ( ctx );

		final CHttpRequest reqObj = ctx.request ();
		Path pathAsMetricName = getMetricNamer().getMetricNameFor ( reqObj );

		try
		{
			final CHttpFilter.Disposition preRouteOk;
			final Timer.Context prt = fMetrics == null ? null : fMetrics.timer ( pathAsMetricName.makeChildItem ( Name.fromString ( "executionTime" ) ) ).time ();
			try
			{
				preRouteOk = preRouteHandling ( ctx );
			}
			finally
			{
				if ( prt != null ) prt.stop ();
			}

			if ( preRouteOk == Disposition.PASS )
			{
				final CHttpRouteInvocation handler = fRouter.route ( reqObj );
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
	 * Run through installed pre-route handling filters
	 * @param ctx
	 * @return a disposition
	 */
	protected Disposition preRouteHandling ( ServletRequestContext ctx )
	{
		for ( CHttpFilter filter : fFilters )
		{
			final Disposition disp = filter.runFilter ( ctx );
			if ( disp == Disposition.RESPONDED )
			{
				return disp;
			}
		}
		return Disposition.PASS;
	}

	/**
	 * Override this to create a custom handling context for your request handlers.
	 * @param req
	 * @param resp
	 * @param dc
	 * @param rr
	 * @return
	 */
	protected ServletRequestContext createHandlingContext ( HttpServletRequest req, HttpServletResponse resp, CHttpSession dc, CHttpRequestRouter rr )
	{
		return new ServletRequestContext ( req, resp, dc, rr );
	}

	private void sendStdJsonError ( CHttpRequestContext ctx, int err, String msg )
	{
		ctx.response ().sendStatusAndBody ( err,
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
	
	private CHttpSession getSession ( final HttpServletRequest req ) throws ServletException
	{
		CHttpSession result = null;
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
				result = (CHttpSession) session.getAttribute ( servletSessionName );

				if ( result == null )
				{
					result = createSession ();
					if ( result != null )
					{
						session.setAttribute ( servletSessionName, result );
						session.setMaxInactiveInterval ( fSessionTimeInSeconds );
					}
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
		if ( fMetricNamer != null ) return fMetricNamer;

		return new CHttpMetricNamer ()
		{
			@Override
			public Path getMetricNameFor ( CHttpRequest req )
			{
				// handle a null request without throwing
				if ( req == null ) return Path.fromString ( "/null" );

				// get the url path
				String urlPathPart = req.getPathInContext ();

				// replace any dots with %2e, because our metrics library doesn't like them (FIXME: why? it's just a Path?)
				urlPathPart = urlPathPart.replaceAll ( "\\.", "%2E" );

				// we can't end a path in "/", so truncate that, or for the root path, replace it
				if ( urlPathPart.equals ( Path.getPathSeparatorString () ) )
				{
					urlPathPart = "(root)";
				}
				else if ( urlPathPart.endsWith ( Path.getPathSeparatorString () ))
				{
					urlPathPart = urlPathPart.substring ( 0, urlPathPart.length () - 1 );
				}

				return Path.fromString ( "/" + req.getMethod () + " " + urlPathPart );
			}
		};
	}

	private NvReadable fSettings;
	private final JSONObject fProvidedPrefs;
	private final SessionLifeCycle fSessionLifeCycle;
	private final int fSessionTimeInSeconds;

	private final LinkedList<CHttpRouteInstaller> fRouters;
	private final LinkedList<CHttpFilter> fFilters;
	private CHttpRequestRouter fRouter;

	private final IamService<?,?> fAccounts;
	private final MetricsCatalog fMetrics;
	private final CHttpObserverMgr fInspector;

	private static final String kWebSessionObject = "chttp.session.";
	private static final long serialVersionUID = 1L;

	private final CHttpMetricNamer fMetricNamer;

	private static Logger log = LoggerFactory.getLogger ( CHttpServlet.class );
}
