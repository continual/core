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

package io.continual.util.data.json;

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.json.JsonVisitor.ArrayOfObjectVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayOfStringVisitor;

public class JsonUtil
{
	private JsonUtil() {
	}
	
	public static JSONObject readJsonObject ( InputStream is )
	{
		return new JSONObject ( new CommentedJsonTokener( is ) );
	}
	
	public static JSONObject readJsonObject ( String is )
	{
		return new JSONObject ( new CommentedJsonTokener ( is ) );
	}

	public static JSONObject readJsonObject ( Reader is )
	{
		return new JSONObject ( new CommentedJsonTokener ( is ) );
	}

	public static JSONArray readJsonArray ( InputStream is )
	{
		return new JSONArray ( new CommentedJsonTokener ( is ) );
	}

	public static JSONArray readJsonArray ( String is )
	{
		return new JSONArray ( new CommentedJsonTokener ( is ) );
	}

	public static JSONArray readJsonArray ( Reader is )
	{
		return new JSONArray ( new CommentedJsonTokener ( is ) );
	}

	public static Object readJsonValue ( String is )
	{
		return new CommentedJsonTokener ( is ).nextValue ();
	}

	public static JSONObject clone ( JSONObject that )
	{
		if ( that == null ) return null;
		return (JSONObject) cloneJsonValue ( that );
	}

	public static JSONArray clone ( JSONArray that )
	{
		if ( that == null ) return null;
		return (JSONArray) cloneJsonValue ( that );
	}

	public static Object cloneJsonValue ( Object value )
	{
		if ( value == null ) return null;

		if ( value instanceof JSONObject )
		{
			final JSONObject result = new JSONObject ();
			final JSONObject obj = (JSONObject) value;
			for ( Object key : obj.keySet () )
			{
				final String keyStr = key.toString ();
				result.put ( keyStr, cloneJsonValue ( obj.get ( keyStr ) ) );
			}
			return result;
		}
		else if ( value instanceof JSONArray )
		{
			final JSONArray array = (JSONArray) value;
			final JSONArray result = new JSONArray ();
			for ( int i=0; i<array.length(); i++ )
			{
				result.put ( i, cloneJsonValue ( array.get ( i ) ) );
			}
			return result;
		}
		else
		{
			return value;
		}
	}

	/**
	 * For each key in the overlay object, write its value into the same key
	 * in the target object. If the value is itself an object, the overlay process
	 * is performed recursively. If the overlay value is an explicit null value, the
	 * key is removed from the target. Any other value (including array) is simply
	 * overwritten into the target object.
	 * @param target
	 * @param overlay
	 * @return the target argument
	 */
	public static JSONObject overlay ( JSONObject target, JSONObject overlay )
	{
		if ( overlay == null ) return target;
		if ( target == null ) return null;

		for ( Object overlayKey : overlay.keySet () )
		{
			final String overlayKeyStr = overlayKey.toString ();

			final Object currVal = target.opt ( overlayKeyStr );
			final Object newVal = overlay.get ( overlayKeyStr );

			// if the current value is an object and 
			// the overlay value is also an object, then we'll merge the two
			// values into the target.
			if ( currVal instanceof JSONObject && newVal instanceof JSONObject )
			{
				overlay ( (JSONObject) currVal, (JSONObject) newVal );
			}
			else if ( newVal == JSONObject.NULL )
			{
				// remove the key
				target.remove ( overlayKeyStr );
			}
			else
			{
				// any other situation is a straight overwrite
				target.put ( overlayKeyStr, cloneJsonValue ( newVal ) );
			}
		}
		return target;
	}

	public static void copyInto ( JSONObject src, final JSONObject dest )
	{
		if ( src == null || dest == null ) return;
		for ( Object key : src.keySet () )
		{
			final String keyStr = key.toString ();
			dest.put ( keyStr, cloneJsonValue ( src.get ( keyStr ) ) );
		}
	}

	public static JSONObject putDefault ( JSONObject obj, String key, Object val )
	{
		if ( !obj.has ( key ) )
		{
			obj.put ( key, val );
		}
		return obj;
	}

	public static int getIndexOfStringInArray ( String s, JSONArray a )
	{
		if ( a == null || s == null ) return -1;

		int found = -1;
		for ( int i=0; i<a.length (); i++ )
		{
			if ( s.equals ( a.getString ( i ) ) )
			{
				found = i;
				break;
			}
		}

		return found;
	}

	public static boolean ensureStringInArray ( String s, JSONArray a )
	{
		if ( a == null || s == null ) return false;

		final int found = getIndexOfStringInArray ( s, a );
		if ( found < 0 )
		{
			a.put ( s );
			return true;
		}

		return false;
	}

	public static boolean removeStringFromArray ( JSONArray a, String s )
	{
		if ( a == null || s == null ) return false;

		final int found = getIndexOfStringInArray ( s, a );
		if ( found > -1 )
		{
			a.remove ( found );
			return true;
		}

		return false;
	}

	public static void writeDate ( JSONObject base, String key, Date d ) throws JSONException
	{
		final ZonedDateTime zdt = d.toInstant ().atZone ( kLocalZone );
		base.put ( key, zdt.format ( DateTimeFormatter.ISO_LOCAL_DATE ) );
	}

	public static void writeDateTime ( JSONObject base, String key, Date d ) throws JSONException
	{
		base.put ( key, writeDate ( d ) );
	}

	public static String writeDate ( Date d ) throws JSONException
	{
		final ZonedDateTime zdt = d.toInstant ().atZone ( kLocalZone );
		return zdt.format ( DateTimeFormatter.ISO_LOCAL_DATE );
	}

	public static String writeDate ( LocalDate d ) throws JSONException
	{
		return d.format ( DateTimeFormatter.ISO_LOCAL_DATE );
	}

	public static LocalDate readDate ( JSONObject base, String key ) throws JSONException
	{
		final Object o = base.opt ( key );
		if ( o == null ) return null;

		if ( o instanceof Long )
		{
			final Instant i = Instant.ofEpochMilli ( (Long)o );
			return i.atZone ( kLocalZone ).toLocalDate ();	
		}

		if ( o instanceof String )
		{
			// try basic ISO types
			try
			{
				final LocalDateTime ldt = LocalDateTime.parse ( (String)o );
				return ldt.toLocalDate ();
			}
			catch ( DateTimeParseException x )
			{
				// no good; ignore
			}
			try
			{
				return LocalDate.parse ( (String)o );
			}
			catch ( DateTimeParseException x )
			{
				// no good; ignore
			}

			// then other formats
			for ( String format : dateFormats )
			{
				try
				{
					final SimpleDateFormat sdf = new SimpleDateFormat ( format );
					final Date d = sdf.parse ( (String) o );
					d.toInstant ().atZone ( kLocalZone ).toLocalDate ();
				}
				catch ( ParseException e )
				{
					// ignore
				}
			}
		}

		throw new JSONException ( "Unrecognized format for Date read." );
	}

	// EST best guess for USA
	private static final ZoneId kLocalZone = ZoneId.of ( "America/New_York" );

	private static String[] dateFormats =
	{
		"yyyy.MM.dd HH:mm:ss z",
		"yyyyMMdd",
	};

	/**
	 * Load a string or array of strings into a string list.
	 * 
	 * @param base the base json object
	 * @param key the key of the string array
	 * @return a list of 0 or more strings
	 * @throws JSONException if the key doesn't exist, or if it's not a string or array of strings
	 */
	public static List<String> readStringArray ( JSONObject base, String key ) throws JSONException
	{
		return readStringArray ( base, key, null );
	}

	/**
	 * Parse a string value into a list.
	 */
	public interface StringArrayValueParser
	{
		Collection<String> parse ( String rawValue );
	}

	/**
	 * Load a string or array of strings into a string list.
	 * 
	 * @param base the base json object
	 * @param key the key of the string array
	 * @return a list of 0 or more strings
	 * @throws JSONException if the key doesn't exist, or if it's not a string or array of strings
	 */
	public static List<String> readStringArray ( JSONObject base, String key, StringArrayValueParser parser ) throws JSONException
	{
		final LinkedList<String> result = new LinkedList<String> ();

		final Object oo = base.opt ( key );
		if ( oo == null ) throw new JSONException ( key + " does not exist" );

		if ( oo instanceof JSONArray )
		{
			JsonVisitor.forEachStringElement ( (JSONArray)oo, new ArrayOfStringVisitor () 
			{
				@Override
				public boolean visit ( String str ) throws JSONException
				{
					result.add ( str );
					return true;
				}
			} );
		}
		else
		{
			final String val = oo.toString ();
			if ( parser != null )
			{
				result.addAll ( parser.parse ( val ) );
			}
			else
			{
				result.add ( val );
			}
		}

		return result;
	}

	/**
	 * Sort a JSON array of JSON objects using the value retrieved by the value expression.
	 * @param a an array
	 * @param valueExpression an expression
	 */
	public static void sortArrayOfObjects ( final JSONArray a, final String valueExpression )
	{
		sortArrayOfObjects ( a, valueExpression, false );
	}

	/**
	 * Sort a JSON array of JSON objects using the value retrieved by the value expression.
	 * @param a an array
	 * @param valueExpression an expression
	 * @param reverse if true, reverse the array
	 */
	public static void sortArrayOfObjects ( final JSONArray a, final String valueExpression, boolean reverse )
	{
		// build a list of objects
		final LinkedList<JSONObject> list = new LinkedList<JSONObject> ();
		JsonVisitor.forEachObjectIn ( a, new ArrayOfObjectVisitor ()
		{
			@Override
			public boolean visit ( JSONObject t ) throws JSONException
			{
				if ( t != null )
				{
					list.add ( t );
				}
				return true;
			}
		});

		// sort the list
		Collections.sort ( list, new Comparator<JSONObject> () {

			@Override
			public int compare ( JSONObject o1, JSONObject o2 )
			{
				final Object oval1 = JsonEval.eval ( o1, valueExpression );
				final Object oval2 = JsonEval.eval ( o2, valueExpression );
				if (( oval1 instanceof Long ) && ( oval2 instanceof Long ))
				{
					final Long n1 = (Long) oval1;
					final Long n2 = (Long) oval2;
					return n1.compareTo ( n2 );
				}
				if (( oval1 instanceof Integer ) && ( oval2 instanceof Integer ))
				{
					final Integer n1 = (Integer) oval1;
					final Integer n2 = (Integer) oval2;
					return n1.compareTo ( n2 );
				}

				final String e1 = oval1 == null ? "" : oval1.toString ();
				final String e2 = oval2 == null ? "" : oval2.toString ();
				return e1.compareTo ( e2 );
			}
		} );
		if ( reverse )
		{
			Collections.reverse ( list );
		}

		// rewrite the array
		int index = 0;
		for ( JSONObject o : list )
		{
			a.put ( index++, o );
		}
	}

	/**
	 * If the "local" object contains "$ref", return the referenced object, else 
	 * return the original. If the reference is invalid (the target does not exist), return null.
	 * @param topLevel the top-level document
	 * @param local the object which may contain $ref
	 * @return an object or null if the reference is given but does not resolve to an object
	 */
	public static JSONObject resolveRef ( JSONObject topLevel, JSONObject local ) throws JSONException
	{
		if ( local == null ) return null;

		final String ref = local.optString ( "$ref", null );
		if ( ref == null ) return local;

		final String[] parts = ref.split ( "/" );
		if ( !parts[0].equals ( "#" ) ) throw new JSONException ( "Reference must start with #/" );

		JSONObject current = topLevel;
		for ( int i=1; i<parts.length; i++ )
		{
			current = current.optJSONObject ( parts[i] );
			if ( current == null ) return null;
		}

		return current;
	}

	public static String writeConsistently ( Object o )
	{
		if ( o instanceof JSONObject )
		{
			return writeConsistently ( (JSONObject) o );
		}
		else if ( o instanceof JSONArray )
		{
			return writeConsistently ( (JSONArray) o );
		}
		else
		{
			return JSONObject.valueToString ( o );
		}
	}

	public static String writeConsistently ( JSONArray a )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( "[\n" );
		boolean doneOne = false;
		for ( int i=0; i<a.length(); i++ )
		{
			if ( doneOne )
			{
				sb.append ( ",\n" );
			}
			doneOne = true;

			sb.append ( writeConsistently ( a.opt ( i ) ) );
		}
		sb.append ( "\n]\n" );

		return sb.toString();
	}

	/**
	 * Write a string representation of a json object with a consistent format. Two
	 * objects with the same keys and values produce the same string each time.
	 * @param o the object to write
	 * @return a string representation of the object
	 */
	public static String writeConsistently ( JSONObject o )
	{
		final ArrayList<String> keys = new ArrayList<> ();
		keys.addAll ( o.keySet() );
		Collections.sort ( keys );

		final StringBuilder sb = new StringBuilder ();
		sb.append ( "{\n" );
		boolean doneOne = false;
		for ( String key : keys )
		{
			if ( doneOne )
			{
				sb.append ( ",\n" );
			}
			doneOne = true;

			sb
				.append ( key )
				.append ( ":" )
				.append ( writeConsistently ( o.get ( key ) ) );
			;
		}
		sb.append ( "\n}\n" );

		return sb.toString ();
	}

	/**
	 * Hash a json object
	 * @param o a json object 
	 * @return a hash code for this object
	 */
	public static int hash ( JSONObject o )
	{
		// we have to use a predictable key order
		return writeConsistently ( o ).hashCode ();
	}

	/**
	 * Write a JSON document while respecting explicit key ordering... even though such a thing makes no sense.
	 * @param o
	 * @return a JSON string
	 */
	public static String writeWithKeyOrder ( Object o ) { return writeWithKeyOrder ( o, 0 ); }

	private static String writeWithKeyOrder ( Object o, int currentIndent )
	{
		if ( o instanceof JSONObject )
		{
			return writeWithKeyOrder ( (JSONObject) o, currentIndent );
		}
		else if ( o instanceof JSONArray )
		{
			return writeWithKeyOrder ( (JSONArray) o, currentIndent );
		}
		else
		{
			return JSONObject.valueToString ( o );
		}
	}

	/**
	 * Write a JSON document while respecting explicit key ordering... even though such a thing makes no sense.
	 * @param a
	 * @return a JSON string
	 */
	public static String writeWithKeyOrder ( JSONArray a ) { return writeWithKeyOrder ( a, 0 ); }

	private static String writeWithKeyOrder ( JSONArray a, int currentIndent )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( "[\n" );
		boolean doneOne = false;
		for ( int i=0; i<a.length(); i++ )
		{
			if ( doneOne )
			{
				sb.append ( ",\n" );
			}
			doneOne = true;

			for ( int tab=0; tab<currentIndent+1; tab++ ) sb.append ( kTab );
			sb.append ( writeWithKeyOrder ( a.opt ( i ), currentIndent+1 ) );
		}
		sb.append ( "\n" );
		for ( int tab=0; tab<currentIndent; tab++ ) sb.append ( kTab );
		sb.append ( "]" );

		return sb.toString();
	}

	public static final String skKeyOrderArray = "__keyOrder";
	private static final String kTab = "    ";

	/**
	 * Write a JSON document while respecting explicit key ordering... even though such a thing makes no sense.
	 * For JSON objects, we look for an embedded key list and use that. Any keys present that are not in the list
	 * are written after those in the list, and in alpha order.
	 * @param o
	 * @return a JSON string
	 */
	public static String writeWithKeyOrder ( JSONObject o ) { return writeWithKeyOrder ( o, 0 ); }

	private static String writeWithKeyOrder ( JSONObject o, int currentIndent )
	{
		// build the ordered key list

		JSONArray keyOrder = o.optJSONArray ( skKeyOrderArray );
		if ( keyOrder == null ) keyOrder = new JSONArray ();

		final TreeSet<String> unwrittenKeys = new TreeSet<> ();
		unwrittenKeys.addAll ( o.keySet () );
		unwrittenKeys.remove ( skKeyOrderArray );

		final ArrayList<String> keysToWrite = new ArrayList<> ();
		for ( int i=0; i<keyOrder.length (); i++ )
		{
			final String key = keyOrder.getString ( i );
			keysToWrite.add ( key );
			unwrittenKeys.remove ( key );
		}

		final ArrayList<String> remainingKeys = new ArrayList<> ();
		remainingKeys.addAll ( unwrittenKeys );
		Collections.sort ( remainingKeys );
		keysToWrite.addAll ( remainingKeys );

		
		// iterate the key list to write the object
		final StringBuilder sb = new StringBuilder ();
		sb.append ( "{\n" );
		for ( int tab=0; tab<currentIndent; tab++ ) sb.append ( kTab );
		boolean doneOne = false;
		for ( String key : keysToWrite )
		{
			// a bad field name in the key order array...
			if ( !o.has ( key ) ) continue;
			
			if ( doneOne )
			{
				sb.append ( ",\n" );
				for ( int tab=0; tab<currentIndent; tab++ ) sb.append ( kTab );
			}
			doneOne = true;

			sb
				.append ( kTab )
				.append ( JSONObject.quote ( key ) )
				.append ( ": " )
				.append ( writeWithKeyOrder ( o.get ( key ), currentIndent+1 ) );
			;
		}
		sb.append ( "\n" );
		for ( int tab=0; tab<currentIndent; tab++ ) sb.append ( kTab );
		sb.append ( "}" );

		return sb.toString ();
	}
}
