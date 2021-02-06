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
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.util.data.TypeConvertor;
import io.continual.util.nv.NvReadable;

/**
 * Web systems implementing RESTful APIs with basic auth can use this helper class to
 * get credentials from the inbound request.
 */
public class BasicAuthHelper
{
	public static final String kSetting_AuthHeader = "Authorization";

	/**
	 * Build an UsernamePasswordCredential from an inbound HTTP header.
	 * 
	 * @param settings a settings source
	 * @param hr a header reader to isolate this code from the web technology
	 * @return username password credential or null if the header is malformed, etc.
	 */
	public static UsernamePasswordCredential readUsernamePasswordCredential ( NvReadable settings, HeaderReader hr )
	{
		final String authLine = hr.getFirstHeader ( kSetting_AuthHeader );
		if ( authLine == null )
		{
			IamAuthLog.debug ( "No " + kSetting_AuthHeader + " header" );
			return null;
		}

		final String[] parts = authLine.split ( " " );
		if ( parts.length != 2 || !parts[0].equals ( "Basic" ) )
		{
			IamAuthLog.info ( kSetting_AuthHeader + " value is illegal" );
			return null;
		}

		final byte[] decoded = TypeConvertor.base64Decode ( parts[1] );
		final String creds = new String ( decoded );	// FIXME: charset concerns here?

		final int colon = creds.indexOf ( ':' );
		if ( colon == -1 )
		{
			IamAuthLog.info ( kSetting_AuthHeader + " was provided but is malformed" );
			return null;
		}

		final String username = creds.substring ( 0, colon );
		final String password = creds.substring ( colon + 1 );
		return new UsernamePasswordCredential ( username, password );
	}
}
