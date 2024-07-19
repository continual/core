package io.continual.http.app.servers.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class JsonIoEndpoint
{
	/**
	 * Read a JSON body from the request.
	 * @param context
	 * @return a JSON object
	 * @throws JSONException
	 * @throws IOException
	 */
	protected static JSONObject readBody ( CHttpRequestContext context ) throws JSONException, IOException
	{
		return new JSONObject ( new CommentedJsonTokener ( context.request().getBodyStream () ) );
	}

	protected static JSONObject readJsonBody ( CHttpRequestContext context ) throws JSONException, IOException
	{
		return readBody ( context );
	}

	/**
	 * A checked exception for reading JSON data
	 */
	public static class MissingInputException extends Exception
	{
		public MissingInputException ( String msg ) { super ( msg ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Read a required string value from the given JSON object. This is just "getString" but with a checked
	 * exception.
	 * @param from
	 * @param label
	 * @return a string value
	 * @throws MissingInputException
	 */
	protected static String readJsonString ( JSONObject from, String label ) throws MissingInputException
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

	/**
	 * Read a required object value from the given JSON object. This is just "getJSONObject" but with a checked
	 * exception.
	 * @param from
	 * @param label
	 * @return a string value
	 * @throws MissingInputException
	 */
	protected static JSONObject readJsonObject ( JSONObject from, String label ) throws MissingInputException
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

	/**
	 * Send a status code and JSON body as the response
	 * @param context
	 * @param statusCode
	 * @param payload
	 */
	protected static void sendJson ( CHttpRequestContext context, int statusCode, JSONObject payload )
	{
		// user can send a header for pretty printed JSON. otherwise we send it in dense form.
		final boolean pretty = TypeConvertor.convertToBooleanBroad ( context.request ().getFirstHeader ( "X-CioPrettyJson" ) );

		context.response ().sendStatusAndBody (
			statusCode,
			( pretty ? payload.toString (4) : payload.toString () ),
			MimeTypes.kAppJson
		);
	}

	/**
	 * Send 200 OK with the given payload.
	 * @param context
	 * @param payload
	 */
	protected static void sendJson ( CHttpRequestContext context, JSONObject payload )
	{
		sendJson ( context, HttpStatusCodes.k200_ok, payload );
	}

	private static final String kStatusCode = "statusCode";
	
	/**
	 * Send a status code and message in the response. The response body includes the status code and message.
	 * @param context
	 * @param statusCode
	 * @param msg
	 */
	protected static void sendStatusCodeAndMessage ( CHttpRequestContext context, int statusCode, String msg )
	{
		sendJson ( context, statusCode,
			new JSONObject ()
				.put ( kStatusCode, statusCode )
				.put ( "message",  msg )
		);
	}

	/**
	 * Send 200 OK with the given message.
	 * @param context
	 * @param msg
	 */
	protected static void sendStatusOk ( CHttpRequestContext context, String msg )
	{
		sendStatusCodeAndMessage ( context, HttpStatusCodes.k200_ok, msg );
	}

	/**
	 * Send 200 OK with the given JSON body. The status code field is written into the JSON body.
	 * @param context
	 * @param msg
	 */
	protected static void sendStatusOk ( CHttpRequestContext context, JSONObject msg )
	{
		sendJson ( context, HttpStatusCodes.k200_ok,
			JsonUtil.clone ( msg )
				.put ( kStatusCode, HttpStatusCodes.k200_ok )
		);
	}

	/**
	 * Send 204 No Content
	 * @param context
	 */
	protected static void sendStatusOkNoContent ( CHttpRequestContext context )
	{
		context.response ()
			.setStatus ( HttpStatusCodes.k204_noContent )
		;
	}

	/**
	 * Send 401 Not Authorized
	 * @param context
	 */
	protected static void sendNotAuth ( CHttpRequestContext context )
	{
		sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized. Check your API credentials." );
	}
}
