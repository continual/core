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

package io.continual.http.util;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

/**
 * Write JSON objects to a response.
 */
public class JsonBodyWriter
{
	/**
	 * Write a list of JSON objects to the response stream in the given context, with a 
	 * 200 status code.
	 * 
	 * @param context
	 * @param objects
	 * @throws IOException
	 */
	public static void writeObjectList ( CHttpRequestContext context, List<JSONObject> objects ) throws IOException
	{
		final JSONArray out = new JSONArray ( objects );
		context.response ().
			setStatus ( HttpStatusCodes.k200_ok ).
			setContentType ( MimeTypes.kAppJson ).
			send ( out.toString () + "\n" );
	}
}
