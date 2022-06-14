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

package io.continual.services.processor.engine.model;

import org.json.JSONObject;

import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;

public class Message implements JsonSerialized
{
	public static Message copyJsonToMessage ( JSONObject data )
	{
		return new Message ( data, true );
	}
	
	public static Message adoptJsonAsMessage ( JSONObject data )
	{
		return new Message ( data, false );
	}

	public Message ()
	{
		// an empty message
		this ( new JSONObject (), false );
	}

	protected Message ( JSONObject msgData, boolean clone )
	{
		fData = clone ? JsonUtil.clone ( msgData ) : msgData;
	}

	@Override
	public Message clone ()
	{
		return new Message ( accessRawJson (), true );
	}

	@Override
	public String toString ()
	{
		return toJson().toString ( 4 );
	}

	public String toLine ()
	{
		return toJson().toString ();
	}

	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fData );
	}

	public JSONObject accessRawJson ()
	{
		return fData;
	}
	
	public boolean hasValue ( String fieldName )
	{
		return fData.has ( fieldName );
	}

	public String getValueAsString ( String key )
	{
		return fData.optString ( key, "" );
	}

	/**
	 * Evaluate a ${} expression against this message.
	 * @param expression
	 * @return a string
	 */
	public String evalExpression ( String expression )
	{
		return JsonEval.evalToString ( fData, expression );
	}

	public boolean getBoolean ( String field, boolean def )
	{
		return fData.optBoolean ( field, def );
	}

	public int getInt ( String field, int i )
	{
		return fData.optInt ( field, i );
	}

	public long getLong ( String field, long i )
	{
		return fData.optLong ( field, i );
	}

	public double getDouble ( String field, double i )
	{
		return fData.optDouble ( field, i );
	}

	public Message putValue ( String to, boolean val )
	{
		fData.put ( to, val );
		return this;
	}

	public Message putValue ( String to, double val )
	{
		fData.put ( to, val );
		return this;
	}

	public Message putValue ( String to, long val )
	{
		fData.put ( to, val );
		return this;
	}

	public Message putValue ( String to, String val )
	{
		fData.put ( to, val );
		return this;
	}

	public Message putRawValue ( String to, Object val )
	{
		fData.put ( to, val );
		return this;
	}

	public Message clearValue ( String key )
	{
		fData.remove ( key );
		return this;
	}
	
	private final JSONObject fData;
}
