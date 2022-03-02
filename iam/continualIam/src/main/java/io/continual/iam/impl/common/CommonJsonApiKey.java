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

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.iam.identity.ApiKey;

public class CommonJsonApiKey implements ApiKey
{
	public static JSONObject initialize ( String secret, String userId )
	{
		return new JSONObject ()
			.put ( CommonJsonDb.kUserId, userId )
			.put ( CommonJsonDb.kSecret, secret )
		;
	}

	public CommonJsonApiKey ( String id, JSONObject record )
	{
		fId = id;
		fUserId = record.optString ( CommonJsonDb.kUserId, record.optString ( "user", null ) );
		fSecret = record.getString ( CommonJsonDb.kSecret );
		fCreateTsMs = record.optLong ( CommonJsonDb.kCreateTsMs, kDefaultTs );

		if ( fUserId == null )
		{
			throw new JSONException ( "Expected either " + CommonJsonDb.kUserId + " or user in API key record." );
		}
	}

	@Override
	public String getKey () { return fId; }

	@Override
	public String getSecret () { return fSecret; }

	@Override
	public String getUserId () { return fUserId; }

	@Override
	public long getCreationTimestamp () { return fCreateTsMs; }

	private final String fId;
	private final String fSecret;
	private final String fUserId;
	private final long fCreateTsMs;

	private static final long kDefaultTs = 0L;
}
