package io.continual.onap.services.mrCommon;

/**
 * A response from the ONAP Message Router HTTP API.
 */
public class OnapMrResponse
{
	/**
	 * Construct a base response
	 * @param statusCode the HTTP status code
	 * @param msg the HTTP status message
	 */
	public OnapMrResponse ( int statusCode, String msg )
	{
		fStatusCode = statusCode;
		fMsg = msg;
	}

	/**
	 * Is this response successful?
	 * @return true if the response status code is in success range
	 */
	public boolean isSuccess ()
	{
		return HttpHelper.isSuccess ( fStatusCode );
	}

	/**
	 * Get the HTTP status code
	 * @return the HTTP status code
	 */
	public int getStatusCode ()
	{
		return fStatusCode;
	}

	/**
	 * Get the HTTP status text
	 * @return the HTTP status text
	 */
	public String getStatusText ()
	{
		return fMsg;
	}

	private final int fStatusCode;
	private final String fMsg;
}

