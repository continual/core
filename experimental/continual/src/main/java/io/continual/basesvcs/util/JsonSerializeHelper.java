package io.continual.basesvcs.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class JsonSerializeHelper
{
	/**
	 * Read a JSON string into a JSON object
	 * @param json
	 * @return a JSONObject
	 */
	public static JSONObject readObject ( String json )
	{
		final JSONObject o = new JSONObject ( new JSONTokener ( json ) );
		return o;
	}

	public interface ValueSerializer<T>
	{
		public Object serialize ( T value );
		public T deserialize ( Object jsonValue );
	};

	/**
	 * Serialize a map into a JSONObject
	 * @param m
	 * @param vs
	 * @return
	 */
	public static <T> JSONObject serializeMapToObject ( Map<?,T> m, ValueSerializer<T> vs )
	{
		final JSONObject o = new JSONObject ();
		for ( Entry<?, T> e : m.entrySet () )
		{
			final String key = e.getKey ().toString ();
			o.put ( key, vs.serialize ( e.getValue() ) );
		}
		return o;
	}

	/**
	 * Deserialize from a JSON object into a map
	 * @param o
	 * @param vs
	 * @return
	 */
	public static <T> Map<String,T> deserializeMapFromObject ( JSONObject o, final ValueSerializer<T> vs )
	{
		final HashMap<String,T> m = new HashMap<String,T> ();
		JsonVisitor.forEachElement ( o, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String key, Object t ) throws JSONException
			{
				m.put ( key, vs.deserialize ( t ) );
				return true;
			}
		} );
		return m;
	}

	/**
	 * Serialize a map into a JSONObject and return the string form
	 * @param m
	 * @param vs
	 * @return
	 */
	public static <T> String serialize ( Map<?,T> m, ValueSerializer<T> vs )
	{
		return serializeMapToObject(m,vs).toString ();
	}

	/**
	 * Serialize a list into a JSONArray
	 * @param l
	 * @param vs
	 * @return
	 */
	public static <T> JSONArray serializeListToArray ( List<T> l, ValueSerializer<T> vs )
	{
		final JSONArray a = new JSONArray ();
		for ( T t : l )
		{
			a.put ( vs.serialize ( t ) );
		}
		return a;
	}

	/**
	 * Serialize a list of strings into a JSONArray
	 * @param l
	 * @return
	 */
	public static JSONArray serialize ( List<String> l )
	{
		return serializeListToArray(l,new StringSerializer());
	}

	/**
	 * A serializer for string values
	 * @author peter
	 *
	 */
	public static class StringSerializer implements ValueSerializer<String>
	{
		@Override
		public Object serialize ( String value )
		{
			return value;
		}

		@Override
		public String deserialize ( Object jsonValue )
		{
			return (String) jsonValue;
		}
	}

	public static <T> T deserializePojo ( JSONObject o, Class<T> clazz )
	{
		try
		{
			final String className = o.getString ( "class" );
			final Class<? extends T> cc = Class.forName ( className ).asSubclass ( clazz );

			// look for a static method that will instantiate this class from a JSON object
			T result = tryStaticFromJson ( cc, o );
			if ( result == null )
			{
				result = tryJsonConstruct ( cc, o );
			}
		}
		catch ( ClassNotFoundException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( NoSuchMethodException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( SecurityException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( IllegalAccessException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( IllegalArgumentException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( InvocationTargetException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		catch ( InstantiationException e )
		{
			log.warn ( "Couldn't load an object: ", e );
		}
		throw new IllegalArgumentException ( "Couldn't create object." );
	}

	private static <T> T tryStaticFromJson ( Class<? extends T> cc, JSONObject o ) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		// look for a static method that will instantiate this class from a JSON object
		final Method m = cc.getMethod ( "fromJson", JSONObject.class );
		if ( Modifier.isStatic ( m.getModifiers ()  ))
		{
			@SuppressWarnings("unchecked")
			final T t = (T) m.invoke ( null, o );
			return t;
		}
		return null;
	}
	
	private static <T> T tryJsonConstruct ( Class<? extends T> cc, JSONObject o ) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException 
	{
		// look for a static method that will instantiate this class from a JSON object
		final Constructor<? extends T> m = cc.getConstructor ( JSONObject.class );
		final T t = m.newInstance ( o );
		return t;
	}
	
	private static final Logger log = LoggerFactory.getLogger ( JsonSerializeHelper.class );
}
