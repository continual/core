package io.continual.jsonHttpClient;

import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;

/**
 * An abstracted cache for HTTP response data
 */
public interface ResponseCache
{
	/**
	 * Close this cache
	 */
	default void close () {}

	/**
	 * Return a cached response if it exists, or null
	 * @param path
	 * @return a response or null
	 */
	HttpResponse get ( String path );

	/**
	 * Put the given request into the cache at the given path.
	 * @param path
	 * @param response
	 */
	void put ( String path, HttpResponse response );

	/**
	 * Remove a cached response with the given path if it exists
	 * @param path
	 */
	void remove ( String path );
}
