
package io.continual.iam.apiserver.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.ApiContextHelper;
import io.continual.restHttp.HttpServlet;

public class BaseEndpoint extends ApiContextHelper
{
	public interface ApiUserContext
	{
		UserContext getUser ();
//		Group getAccount ();
		boolean isUserAccountAdmin ();
	}

	public static Resource makeResource ( String named )
	{
		return new Resource () {
			@Override
			public String getId () { return named; }
		};
	}
	
	public static boolean checkAccess ( CHttpRequestContext context, UserContext user, String resId, String op ) throws IamSvcException
	{
		final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
		final boolean result = am.getAccessDb ().canUser ( user.getEffectiveUserId (), makeResource ( resId ), op );
		if ( !result )
		{
			log.info ( user.toString () + " cannot " + op + " object " + resId );
		}
		return result;
	}
	
	public interface ContextApiHandler
	{
		String handle ( CHttpRequestContext context, HttpServlet servlet, ApiUserContext luc ) throws IOException, JSONException;
	}

	/**
	 * Read a JSON object body
	 * @param ctx
	 * @return a JSON object 
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject readJsonBody ( CHttpRequestContext ctx ) throws JSONException, IOException
	{
		return readBody ( ctx );
	}

	public static class MissingInputException extends Exception
	{
		public MissingInputException ( String msg ) { super ( msg ); }
		private static final long serialVersionUID = 1L;
	}

	public static String readJsonString ( JSONObject from, String label ) throws MissingInputException
	{
		try
		{
			return from.getString ( label );
		}
		catch ( JSONException x )
		{
			throw new MissingInputException ( "Missing required field '" + label + "' in input." );
		}
	}
	
	public static JSONObject readJsonObject ( JSONObject from, String label ) throws MissingInputException
	{
		try
		{
			return from.getJSONObject ( label );
		}
		catch ( JSONException x )
		{
			throw new MissingInputException ( "Missing required field '" + label + "' in input." );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( BaseEndpoint.class );
}



//
//
//
//{
//	public enum ApiVersion
//	{
//		V1
//	}
//	
//	/**
//	 * Get the API version this caller is using.
//	 * @param ctx
//	 * @return an API version
//	 */
//	public static ApiVersion getApiVersion ( CHttpRequestContext ctx )
//	{
//		return ApiVersion.V1;
//	}
//
//	public BaseEndpoint ( IamService<Identity,Group> iam )
//	{
//		super ();
//
//		fIam = iam;
//	}
//
//	public interface StreamWriter
//	{
//		void write ( OutputStream os ) throws IOException;
//	}
//
//	/**
//	 * Send a success response with content.
//	 * @param ctx
//	 * @param result
//	 * @throws JSONException
//	 * @throws IOException
//	 */
//	public static void respondOk ( CHttpRequestContext ctx, JSONObject result ) throws IOException
//	{
//		respondOkWithStream ( ctx, MimeTypes.kAppJson, new ByteArrayInputStream(result.toString(4).getBytes ()) );
//	}
//
//	/**
//	 * Send a success response without content.
//	 * @param ctx
//	 * @param result
//	 * @throws JSONException
//	 * @throws IOException
//	 */
//	public static void respondOkNoContent ( CHttpRequestContext ctx ) throws IOException
//	{
//		sendStatusOkNoContent ( ctx );
//	}
//
//	/**
//	 * Send a success response with content.
//	 * @param ctx
//	 * @param result
//	 * @throws JSONException
//	 * @throws IOException
//	 */
//	public static void respondOkWithHtml ( CHttpRequestContext ctx, String html ) throws IOException
//	{
//		respondOkWithStream ( ctx, "text/html", new ByteArrayInputStream(html.toString().getBytes ()) );
//	}
//
//	/**
//	 * Send a success response with a content stream.
//	 * @param ctx
//	 * @param mediaType
//	 * @param is
//	 * @throws IOException
//	 */
//	public static void respondOkWithStream ( CHttpRequestContext ctx, String mediaType, final InputStream is ) throws IOException
//	{
//		respondOkWithStream ( ctx, mediaType, new StreamWriter ()
//		{
//			@Override
//			public void write ( OutputStream os ) throws IOException
//			{
//				copyStream ( is, os );
//			}
//		});
//	}
//
//	/**
//	 * Send a success response with a content stream.
//	 * @param ctx
//	 * @param mediaType
//	 * @param is
//	 * @throws IOException
//	 */
//	public static void respondOkWithStream ( CHttpRequestContext ctx, String mediaType, final StreamWriter writer ) throws IOException
//	{
//		ctx.response ().setStatus ( HttpServletResponse.SC_OK );
//		final OutputStream os = ctx.response ().getStreamForBinaryResponse ( mediaType );
//		writer.write ( os );
//	}
//
//	/**
//	 * Respond to the client with the given error code and status message
//	 * @param ctx
//	 * @param errCode
//	 * @param msg
//	 * @throws IOException
//	 */
//	public static void respondWithError ( CHttpRequestContext ctx, int errCode, String msg ) throws IOException
//	{
//		sendStatusCodeAndMessage ( ctx, errCode, msg );
//	}
//
//	/**
//	 * Respond to the client with the given error code and status message and a JSON body
//	 * @param ctx
//	 * @param errCode
//	 * @param msg
//	 * @throws IOException
//	 */
//	public static void respondWithErrorInJson ( CHttpRequestContext ctx, int errCode, String msg ) throws IOException
//	{
//		final JSONObject o = new JSONObject ();
//		o.put ( "status", errCode );
//		o.put ( "message", msg );
//		respondWithError ( ctx, errCode, o );
//	}
//
//	/**
//	 *AccessException Respond to the client with the given error code and status message
//	 * @param ctx
//	 * @param errCode
//	 * @param msg
//	 * @throws IOException
//	 */
//	public static void respondWithError ( CHttpRequestContext ctx, int errCode, JSONObject body ) throws IOException
//	{
//		ctx.response ().sendErrorAndBody ( errCode, body.toString ( 4 ), MimeTypes.kAppJson );
//	}
//
//	/**
//	 * Copy from the input stream to the output stream, then close the output stream.
//	 * @param in
//	 * @param out
//	 * @throws IOException
//	 */
//	public static void copyStream ( InputStream in, OutputStream out ) throws IOException
//	{
//		copyStream ( in, out, kBufferLength );
//	}
//
//	/**
//	 * Copy from the input stream to the output stream, then close the output stream.
//	 * @param in
//	 * @param out
//	 * @param bufferSize
//	 * @throws IOException
//	 */
//	public static void copyStream ( InputStream in, OutputStream out, int bufferSize ) throws IOException
//	{
//		final byte[] buffer = new byte [bufferSize];
//		int len;
//		while ( ( len = in.read ( buffer ) ) != -1 )
//		{
//			out.write ( buffer, 0, len );
//		}
//		out.close ();
//	}
//
//
//	public static void sendJson ( CHttpResponse r, JSONObject json ) throws IOException
//	{
//		sendJson ( r, json.toString (4) );
//	}
//
//	public static void sendJson ( CHttpResponse r, String json ) throws IOException
//	{
//		final PrintWriter pw = r.getStreamForTextResponse ( MimeTypes.kAppJson );
//		pw.println ( json );
//	}
//
//	public static void sendError ( CHttpResponse r, int statusCode, String msg ) throws IOException
//	{
//		final JSONObject o = new JSONObject ()
//			.put ( "error", msg )
//			.put ( "status", statusCode )
//		;
//		r.sendErrorAndBody ( statusCode, o.toString (), MimeTypes.kAppJson );
//	}
//
//	public static void withUser ( CHttpRequestContext ctx, ApiHandler handler )
//	{
//		ApiContextHelper.handleWithApiAuth ( ctx, handler );
//	}
//	
//	protected IamService<Identity,Group> getIamInterface () { return fIam; }
//
//	private final IamService<Identity,Group> fIam;
//
//	protected static final int kBufferLength = 4096;
//}
