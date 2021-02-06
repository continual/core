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

package io.continual.builder.sources;

import java.io.InputStream;
import java.io.Reader;

import org.json.JSONObject;

import io.continual.util.data.json.CommentedJsonTokener;

import io.continual.builder.BuilderDataSource;
import io.continual.builder.common.CommonDataSource;

/**
 * A builder data source backed by the json.org JSON object implementation.
 */
public class BuilderJsonDataSource extends CommonDataSource implements BuilderDataSource
{
	public BuilderJsonDataSource ( JSONObject data )
	{
		super ( JSONObject.class, "fromJson", data );
		fData = data;
	}

	public BuilderJsonDataSource ( InputStream data )
	{
		this ( new JSONObject ( new CommentedJsonTokener ( data ) ) );
	}

	public BuilderJsonDataSource ( Reader data )
	{
		this ( new JSONObject ( new CommentedJsonTokener ( data ) ) );
	}

	public BuilderJsonDataSource ( String data )
	{
		this ( new JSONObject ( new CommentedJsonTokener ( data ) ) );
	}

	@Override
	public String getClassNameFromData ()
	{
		// the service code always used "classname" but the builder code used
		// "class."  For compatibility, we'll allow both but prefer "class".

		String cn = fData.optString ( "class", null );
		if ( cn == null )
		{
			cn = fData.optString ( "classname", null );
		}
		return cn;
	}

	private final JSONObject fData;
}
