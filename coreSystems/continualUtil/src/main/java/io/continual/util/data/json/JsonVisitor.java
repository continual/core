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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonVisitor
{
	private JsonVisitor() {
	}
	
	public interface ArrayVisitor<T,E extends Exception>
	{
		/**
		 * Visit an array entry. Return false to stop iteration.
		 * @param t an item of type T
		 * @return if true, iteration continues
		 * @throws JSONException when an underlying JSON operation throws
		 * @throws E an additional exception type that your processing may throw
		 */
		boolean visit ( T t ) throws JSONException, E;
	}

	public interface ArrayOfObjectVisitor extends ArrayVisitor<JSONObject,JSONException>
	{
	}

	public interface ArrayOfStringVisitor extends ArrayVisitor<String,JSONException>
	{
	}

	@SuppressWarnings("unchecked")
	public static <T,E extends Exception> void forEachElement ( JSONArray a, ArrayVisitor<T,E> v ) throws JSONException, E
	{
		if ( a == null ) return;
		
		final int len = a.length ();
		for ( int i=0; i<len; i++ )
		{
			final Object o = a.get ( i );
			if ( o != null )
			{
				if ( !( v.visit ( (T) o ) ) )
				{
					break;
				}
			}
		}
	}

	public static void forEachStringElement ( JSONArray a, ArrayOfStringVisitor v ) throws JSONException
	{
		forEachElement ( a, v );
	}

	public static void forEachObjectIn ( JSONArray a, ArrayOfObjectVisitor v ) throws JSONException
	{
		forEachElement ( a, v );
	}

	/**
	 * Returns a map of string values from a JOSNObject. If the argument is null, an empty map is returned.
	 * @param obj
	 * @return a map
	 */
	public static HashMap<String,String> objectToMap ( JSONObject obj )
	{
		final HashMap<String,String> map = new HashMap<String,String> ();
		if ( obj != null )
		{
			for ( Object oo : obj.keySet () )
			{
				final String key = oo.toString ();
				final String val = obj.getString ( key );
				map.put ( key, val );
			}
		}
		return map;
	}

	public static<T> HashMap<String,T> objectToMap ( JSONObject obj, ItemRenderer<Object,T> ir )
	{
		return objectToMap ( obj, new KeyRenderer<String> ()
		{
			@Override
			public String render ( String key ) { return key; }
		}, ir );
	}

	public static<K,T> HashMap<K,T> objectToMap ( JSONObject obj, KeyRenderer<K> kr, ItemRenderer<Object,T> ir )
	{
		final HashMap<K,T> map = new HashMap<> ();
		if ( obj != null )
		{
			for ( Object oo : obj.keySet () )
			{
				final String keyStr = oo.toString ();
				final K key = kr.render ( keyStr );
				final T val = ir.render ( obj.get ( keyStr ) );
				map.put ( key, val );
			}
		}
		return map;
	}

	public interface ObjectFilter
	{
		boolean matches ( JSONObject item );
	}
	
	public static List<JSONObject> findMatchingObjects ( JSONArray a, ObjectFilter of )
	{
		final LinkedList<JSONObject> result = new LinkedList<> ();
		forEachObjectIn ( a, new ArrayOfObjectVisitor ()
		{
			@Override
			public boolean visit ( JSONObject item ) throws JSONException
			{
				if ( of.matches ( item ) )
				{
					result.add ( item );
				}
				return true;
			}
		} );
		return result;
	}

	public static boolean listContains ( JSONArray a, String t )
	{
		if ( a == null ) return false;

		final int len = a.length ();
		for ( int i=0; i<len; i++ )
		{
			final Object o = a.get ( i );
			if ( o != null )
			{
				final String s = o.toString ();
				if ( s.equals ( t  ) ) return true;
			}
		}
		return false;
	}

	public interface ValueReader<F,T> 
	{
		T read ( F val );
	};

	public static <T,F> List<T> arrayToList ( JSONArray a, final ValueReader<F,T> vr )
	{
		final LinkedList<T> list = new LinkedList<T> ();
		if ( a != null )
		{
			forEachElement ( a, new ArrayVisitor<F,JSONException> ()
			{
				@Override
				public boolean visit ( F t ) throws JSONException
				{
					list.add ( vr.read ( t ) );
					return true;
				}
			});
		}
		return list;
	}

	public static List<String> arrayToList ( JSONArray a )
	{
		final LinkedList<String> list = new LinkedList<String> ();
		if ( a != null )
		{
			forEachElement ( a, new ArrayVisitor<Object,JSONException> ()
			{
				@Override
				public boolean visit ( Object t ) throws JSONException
				{
					if ( t != null )
					{
						list.add ( t.toString () );
					}
					return true;
				}
			});
		}
		return list;
	}
	
	public static List<Integer> arrayToIntList ( JSONArray a )
	{
		final LinkedList<Integer> list = new LinkedList<Integer> ();
		if ( a != null )
		{
			forEachElement ( a, new ArrayVisitor<Integer,JSONException> ()
			{
				@Override
				public boolean visit ( Integer t ) throws JSONException
				{
					list.add ( t );
					return true;
				}
			});
		}
		return list;
	}

	public interface ItemRenderer<T,O>
	{
		O render ( T t ) throws IllegalArgumentException;
	}

	public interface KeyRenderer<K>
	{
		K render ( String key );
	}

	public static class StdItemRenderer<T> implements ItemRenderer<T,Object>
	{
		@Override
		public Object render ( T t )
		{
			if ( t instanceof JsonSerialized )
			{
				return ((JsonSerialized)t).toJson ();
			}
			return t;
		}
	}

	public static <T> JSONArray listToArray ( String[] list )
	{
		return listToArray ( Arrays.asList ( list ) );
	}

	public static <T> JSONArray listToArray ( Collection<T> list )
	{
		return listToArray ( list, new StdItemRenderer<T> () );
	}

	public static <T,O> JSONArray listToArray ( Collection<T> list, ItemRenderer<T,O> renderer )
	{
		return collectionToArray ( list, renderer );
	}

	public static <T> JSONArray collectionToArray ( Collection<T> list )
	{
		return collectionToArray ( list, new StdItemRenderer<T> () );
	}
	
	public static <T,O> JSONArray collectionToArray ( Collection<T> list, ItemRenderer<T,O> renderer )
	{
		if ( list == null ) return null;

		final JSONArray a = new JSONArray ();
		for ( T o : list )
		{
			a.put ( renderer.render ( o ) );
		}
		return a;
	}

	public static JSONObject mapOfStringsToObject ( Map<String,String> list )
	{
		final JSONObject obj = new JSONObject ();
		for ( Map.Entry<String,String> e : list.entrySet () )
		{
			obj.put ( e.getKey (), e.getValue () );
		}
		return obj;
	}

	public static JSONObject mapToObject ( Map<String, Integer> list )
	{
		return mapToObject ( list, i -> i );
	}

	public static JSONObject mapOfJsonToObject ( Map<String, ? extends JsonSerialized> list )
	{
		return mapToObject ( list, i -> i.toJson () );
	}

	public static <T> JSONObject mapToObject ( Map<String, T> list, ItemRenderer<T,?> renderer )
	{
		return mapToObject ( list, k -> k, renderer );
	}

	public static <K,T> JSONObject mapToObject ( Map<K, T> list, ItemRenderer<K,String> keyRender, ItemRenderer<T,?> valRender )
	{
		final JSONObject obj = new JSONObject ();
		for ( Map.Entry<K,T> e : list.entrySet () )
		{
			obj.put ( keyRender.render ( e.getKey () ), valRender.render ( e.getValue () ) );
		}
		return obj;
	}

	public interface ObjectVisitor<T,E extends Exception>
	{
		/**
		 * Visit an entry.
		 * @param key the string key of the entry
		 * @param t the value of type T of the entry
		 * @return true to continue, false to exit the loop
		 * @throws JSONException when an underlying JSON operation throws
		 * @throws E an additional exception type your code may throw
		 */
		boolean visit ( String key, T t ) throws JSONException, E;
	}

	public static <T,E extends Exception> void forEachElement ( JSONObject object, ObjectVisitor<T,E> v ) throws JSONException, E
	{
		forEachElement ( object, v, false );
	}

	@SuppressWarnings("unchecked")
	public static <T,E extends Exception> void forEachElement ( JSONObject object, ObjectVisitor<T,E> v, boolean allowNulls ) throws JSONException, E
	{
		if ( object == null ) return;
		for ( Object keyObj : object.keySet () )
		{
			final String key = keyObj.toString ();
			final Object val = object.get ( key );

			if ( val == JSONObject.NULL && allowNulls )
			{
				if ( ! ( v.visit ( key, null ) ) )
				{
					break;
				}
			}
			else if ( ! ( v.visit ( key, (T)(val) ) ) )
			{
				break;
			}
		}
	}
}
