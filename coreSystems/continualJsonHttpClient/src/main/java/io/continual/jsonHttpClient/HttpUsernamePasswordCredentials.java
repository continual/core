package io.continual.jsonHttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Username/password credentials
 */
public class HttpUsernamePasswordCredentials
{
	/**
	 * Construct a credentials pair with a username and password
	 * @param user
	 * @param pwd
	 */
	public HttpUsernamePasswordCredentials ( String user, String pwd )
	{
		fUser = user;
		fPwd = pwd;
	}

	/**
	 * Get the username
	 * @return the username
	 */
	public String getUser () { return fUser; }

	/**
	 * Get the password
	 * @return the password
	 */
	public String getPassword () { return fPwd; }

	/**
	 * Return the credentials as a base64 encoding suitable for use as HTTP basic auth
	 * @return
	 */
	public String asBasicAuth ()
	{
		final String authText = fUser + ":" + ( fPwd == null ? "" : fPwd );
		return Base64.getEncoder ().encodeToString ( authText.getBytes ( StandardCharsets.UTF_8 ) );
	}

	private final String fUser;
	private final String fPwd;
}
