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

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.StringUtils;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;

public class Message implements JsonSerialized
{
	/**
	 * Clone the given data into a message. The original data is not
	 * used after this call and the caller can continue to update it.
	 * @param data
	 * @return a message
	 */
	public static Message copyJsonToMessage ( JSONObject data )
	{
		return new Message ( data, true );
	}

	/**
	 * Adopt the given data into a message. The original data should
	 * not be updated again outside of the message class. That is, the 
	 * JSON is now owned by the message, not the caller.
	 * @param data
	 * @return a message
	 */
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

	/**
	 * Generate a single line representation of the message 
	 * @return a string
	 */
	public String toLine ()
	{
		return toJson().toString ();
	}

	/**
	 * Produce a JSON object of this message
	 * @return a json object
	 */
	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fData );
	}

	/**
	 * Get access to the raw JSON object data for this message. 
	 * @return the internal JSON object
	 */
	public JSONObject accessRawJson ()
	{
		return fData;
	}

	public static class FieldSpec
	{
		/**
		 * Convert a dot-separated key string to a field spec
		 * @param key
		 * @return a field spec
		 */
		public static FieldSpec fromString ( String key )
		{
			return new FieldSpec ( key, true );
		};

		/**
		 * Convert a string into a field spec without interpreting dots as container
		 * separators.
		 * @param key
		 * @return a field spec
		 */
		public static FieldSpec fromSimpleString ( String key )
		{
			return new FieldSpec ( key, false );
		};

		private FieldSpec ( String key, boolean useDotSeps )
		{
			fContainers = new LinkedList<> ();
			if ( useDotSeps )
			{
				final String[] parts = key.split ( "\\." );
				for ( int i = 0; i < parts.length - 1; i++ )
				{
					fContainers.add ( parts[i] );
				}
				fField = parts [ parts.length - 1 ];
			}
			else
			{
				fField = key;
			}			
		}

		private JSONObject getContainer ( JSONObject topLevel, boolean createIntermediates )
		{
			if ( topLevel == null ) return null;
			return JsonEval.getContainer ( topLevel, fContainers, createIntermediates );
		}

		private final LinkedList<String> fContainers;
		private final String fField;
	}

	/**
	 * Return true if the message has the given field. 
	 * @param fieldName
	 * @return true if the field exists on this message
	 */
	public boolean hasValue ( String fieldName )
	{
		return hasValue ( FieldSpec.fromString ( fieldName ) );
	}

	/**
	 * Return true if the message has the given field. 
	 * @param fs
	 * @return true if the field exists on this message
	 */
	public boolean hasValue ( FieldSpec fs )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? false : container.has ( fs.fField );
	}

	/**
	 * 
	 * Evaluate a ${} expression against this message.
	 * @param expression
	 * @return a string
	 */
	public String evalExpression ( String expression )
	{
		return JsonEval.evalToString ( fData, expression );
	}

	public String getString ( String field )
	{
		return getString ( field, "" );
	}

	public String getString ( String field, String def )
	{
		return getString ( FieldSpec.fromString ( field ), def );
	}

	public String getString ( FieldSpec fs, String def )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? def : container.optString ( fs.fField, def );
	}

	public boolean getBoolean ( String field, boolean def )
	{
		return getBoolean ( FieldSpec.fromString ( field ), def );
	}

	public boolean getBoolean ( FieldSpec fs, boolean def )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? def : container.optBoolean ( fs.fField, def );
	}

	public int getInt ( String field, int i )
	{
		return getInt ( FieldSpec.fromString ( field ), i );
	}

	public int getInt ( FieldSpec fs, int def )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? def : container.optInt ( fs.fField, def );
	}

	public long getLong ( String field, long i )
	{
		return getLong ( FieldSpec.fromString ( field ), i );
	}

	public long getLong ( FieldSpec fs, long def )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? def : container.optLong ( fs.fField, def );
	}

	public double getDouble ( String field, double def )
	{
		return getDouble ( FieldSpec.fromString ( field ), def );
	}

	public double getDouble ( FieldSpec fs, double def )
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? def : container.optDouble ( fs.fField, def );
	}

	public Message putValue ( String to, boolean val )
	{
		return putValue ( FieldSpec.fromString ( to ), val );
	}

	public Message putValue ( String to, double val )
	{
		return putValue ( FieldSpec.fromString ( to ), val );
	}

	public Message putValue ( String to, long val )
	{
		return putValue ( FieldSpec.fromString ( to ), val );
	}

	public Message putValue ( String to, String val )
	{
		return putValue ( FieldSpec.fromString ( to ), val );
	}

	public Message putRawValue ( String to, Object val )
	{
		return putValue ( FieldSpec.fromString ( to ), val );
	}

	public <T> Message putValue ( FieldSpec fs, T val )
	{
		final JSONObject container = fs.getContainer ( fData, true );
		if ( container != null )
		{
			container.put ( fs.fField, val );
		}
		return this;
	}

	/**
	 * Append a value to the given array. If the array does not exist, it's created.
	 * If a non-array value exists with the key, it's replaced with an empty array and then
	 * the new value is appended.
	 * @param arrayKey
	 * @param val
	 * @return this message
	 */
	public Message appendRawValue ( String arrayKey, Object val )
	{
		final JSONObject container = FieldSpec.fromString ( arrayKey ).getContainer ( fData, true );

		JSONArray a = container.optJSONArray ( arrayKey );
		if ( a == null )
		{
			a = new JSONArray ();
			container.put ( arrayKey, a );
		}
		a.put ( val );
		return this;
	}

	public Object getRawValue ( String key ) 
	{
		return getRawValue ( FieldSpec.fromString ( key ) );
	}

	public Object getRawValue ( FieldSpec fs ) 
	{
		final JSONObject container = fs.getContainer ( fData, false );
		return container == null ? null : container.opt ( fs.fField );
	}

	public Message clearValue ( String key )
	{
		if ( StringUtils.isEmpty ( key ) ) return this;

		final JSONObject data = JsonEval.getContainerOf ( fData, key );
		final List<String> pathParts = JsonEval.splitPath ( key );
		if ( data != null )
		{
			data.remove ( pathParts.get ( pathParts.size () - 1 ) );
		}
		return this;
	}
	
	private final JSONObject fData;
}
