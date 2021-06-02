package io.continual.onap.services.mrCommon;

import java.io.IOException;

import io.continual.onap.services.subscriber.OnapMrFetchResponse;
import okhttp3.ResponseBody;

/**
 * A parser that receives the MR subscriber response body and returns a list of strings.
 * This class allows this distribution to remain JSON parser neutral without assuming that
 * it's run in a J2EE environment.
 */
public interface JsonResponseParser
{
	/**
	 * Parse the okhttp response body into the fetch response instance.
	 * 
	 * @param httpBody the okhttp response body
	 * @param fetchResponse the fetch response to populate
	 * @throws IOException if reading the body throws an IOException
	 */
	default void parseResponseBody ( ResponseBody httpBody, OnapMrFetchResponse fetchResponse ) throws IOException
	{
		parseResponseBody ( httpBody.string (), fetchResponse );
	}

	/**
	 * Parse the string representation of a response body into the fetch response instance.<br>
	 * <br>
	 * Note: This method is only called by the ResponseBody version above. If you override that,
	 * this method does not need to be implemented beyond meeting the compiler requirement.
	 * 
	 * @param httpBody the http body as a string
	 * @param fetchResponse the fetch response to populate
	 * @throws IOException if reading the body throws an IOException
	 */
	void parseResponseBody ( String httpBody, OnapMrFetchResponse fetchResponse ) throws IOException;
}
