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
package io.continual.iam.credentials;

import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.time.Clock;

/**
 * A JWT credential.
 */
public class JwtCredential
{
	public static class InvalidJwtToken extends Exception
	{
		public InvalidJwtToken () { super(); }	// we don't normally say much for failed authentication
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Build a JWT credential from a header value (normally Authorization)
	 * @param authHeader the authentication header value
	 * @return a JwtCredential
	 * @throws InvalidJwtToken when the token is invalid
	 */
	public static JwtCredential fromHeader ( String authHeader ) throws InvalidJwtToken
	{
		if ( authHeader != null && authHeader.startsWith ( "Bearer " ) )
		{
			final String[] parts = authHeader.split ( " " );
			if ( parts.length == 2 )
			{
				return new JwtCredential ( parts[1] );
			}
		}
		throw new InvalidJwtToken ();
	}

	/**
	 * Build a JWT credential from a token with an sha256 key used for signing
	 * @param jwtToken a token
	 * @throws InvalidJwtToken when the token is invalid
	 */
	public JwtCredential ( String jwtToken ) throws InvalidJwtToken
	{
		this ( jwtToken, true );
	}

	public JwtCredential ( String jwtToken, boolean checkExpired ) throws InvalidJwtToken
	{
		fOrigToken = jwtToken;

		final String[] parts = jwtToken.split ( "\\." );
		if ( parts.length != 3 ) throw new InvalidJwtToken ();

		fSignedContent = parts[0] + "." + parts[1];
		
		log.info ( "signed data: {}", fSignedContent );
		
		fSignature = parts[2];

		try
		{

			final String headerStr = new String ( TypeConvertor.base64UrlDecode ( parts[0] ) );
			final JSONObject header = new JSONObject ( headerStr );
			final String type = header.getString ( "typ" );
			fAlgo = header.getString ( "alg" );
			if ( !type.equals ( "JWT" ) || !skSignatureAlgos.contains ( fAlgo ) )
			{
				// all we know so far...
				log.info ( "Unrecognized type or algo on JWT: " + type + " / " + fAlgo );
				throw new InvalidJwtToken ();
			}
			

			final String payloadStr = new String ( TypeConvertor.base64UrlDecode ( parts[1] ) );
			final JSONObject payload = new JSONObject ( payloadStr );

			log.debug ( "Unpacking JWT: {}", payloadStr );

			fIssuer = payload.getString ( "iss" );
			
			fAudience = new TreeSet<> ();
			final JSONArray aud = payload.optJSONArray ( "aud" );
			if ( aud != null )
			{
				fAudience.addAll ( JsonVisitor.arrayToList ( aud ) );
			}
			else
			{
				fAudience.add ( payload.getString ( "aud" ) );
			}

			final long expiresSec = payload.getLong ( "exp" );
			final long nowSec = Clock.now () / 1000L;
			if ( checkExpired && expiresSec < nowSec )
			{
				log.info ( "Expired token. exp=" + expiresSec + "; currently " + nowSec );
				throw new InvalidJwtToken ();
			}

			// at this point, we have a token that's valid in terms of time and audience (but hasn't been
			// checked for authenticity)
			fSubject = payload.getString ( "sub" );
			if ( fSubject.length () == 0 )
			{
				log.info ( "No subject on token" );
				throw new InvalidJwtToken ();
			}
		}
		catch ( JSONException e )
		{
			log.info ( "Couldn't parse token." );
			throw new InvalidJwtToken ();
		}
	}

	public String toBearerString ()
	{
		return fOrigToken;
	}

	public String getSignedContent ()
	{
		return fSignedContent;
	}

	public String getSignature ()
	{
		return fSignature;
	}

	@Override
	public String toString () { return "JWT for " + fSubject; }

	public String getSubject ()
	{
		return fSubject;
	}

	public String getIssuer ()
	{
		return fIssuer;
	}

	public boolean isForAudience ( String aud )
	{
		return fAudience.contains ( aud );
	}

	public String getSigningAlgorithm ()
	{
		return fAlgo;
	}
	
	private final String fOrigToken;
	private final String fSubject;
	private final String fIssuer;
	private final TreeSet<String> fAudience;
	private final String fSignedContent;
	private final String fSignature;
	private final String fAlgo;

	private static TreeSet<String> skSignatureAlgos = new TreeSet<String> ();
	static
	{
		skSignatureAlgos.add ( "HS256" );
		skSignatureAlgos.add ( "RS256" );
	}
	
	private static final Logger log = LoggerFactory.getLogger ( JwtCredential.class );
}
