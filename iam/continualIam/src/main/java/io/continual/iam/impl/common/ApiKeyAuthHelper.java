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
package io.continual.iam.impl.common;

import io.continual.iam.IamAuthLog;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.util.nv.NvReadable;

/**
 * Web systems implementing RESTful APIs with API keys can use this helper class to
 * read API key authentication info from the inbound request.
 */
public class ApiKeyAuthHelper
{
	public static final String kSetting_AuthLineHeader = "api.headers.auth";
	public static final String kSetting_DateLineHeader = "api.headers.date";
	public static final String kSetting_MagicLineHeader = "api.headers.magic";

	public static final String kDefault_AuthLineHeader = "X-Auth";
	public static final String kDefault_DateLineHeader = "X-Date";
	public static final String kDefault_MagicLineHeader = "X-Magic";

	/**
	 * Build an ApiKeyCredential from an inbound HTTP header.
	 * 
	 * @param settings a settings source
	 * @param hr a header reader, to isolate web server tech from this class
	 * @param serviceName the name of the service fpr signed content
	 * @return an API key credential, or null if the header is malformed, etc.
	 */
	public static ApiKeyCredential readApiKeyCredential ( NvReadable settings, HeaderReader hr, String serviceName )
	{
		final String authHeader = settings.getString ( kSetting_AuthLineHeader, kDefault_AuthLineHeader );
		final String authLine = hr.getFirstHeader ( authHeader );
		if ( authLine == null )
		{
			IamAuthLog.debug ( "No " + authHeader + " header" );
			return null;
		}

		final String signedContent = SignedContentReader.getSignedContent ( 
			hr.getFirstHeader ( "Date" ),
			hr.getFirstHeader ( settings.getString ( kSetting_DateLineHeader, kDefault_DateLineHeader )), 
			hr.getFirstHeader ( settings.getString ( kSetting_MagicLineHeader, kDefault_MagicLineHeader )),
			serviceName );
		if ( signedContent == null )
		{
			return null;
		}

		//
		// This originally was used to log an auth line with a signature. Now it may be a Basic auth
		// line transmitted safely via https. Logging it here exposes an encoded version of the user's
		// username and password.
		//
		// authLog ( "authLine [" + authLine + "]" );

		final int colon = authLine.indexOf ( ':' );
		if ( colon == -1 )
		{
			IamAuthLog.info ( "" + authHeader + " is malformed" );
			return null;
		}

		final String apiKey = authLine.substring ( 0, colon );
		final String signature = authLine.substring ( colon + 1 );
		IamAuthLog.info ( "key: " + apiKey + "; signature: " + signature );

		return new ApiKeyCredential ( apiKey, signedContent, signature );
	}
}
