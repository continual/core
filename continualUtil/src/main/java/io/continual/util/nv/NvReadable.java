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
package io.continual.util.nv;

import java.util.Collection;
import java.util.Map;

/**
 * A data supplier
 */
public interface NvReadable
{
	class LoadException extends Exception
	{
		public LoadException ( String reason ) { super(reason); }
		public LoadException ( Throwable cause ) { super(cause); }
		private static final long serialVersionUID = 1L;
	}

	class MissingReqdSettingException extends Exception
	{
		public MissingReqdSettingException ( String key ) { super("Missing required setting \"" + key + "\"" ); fKey = key; }
		public MissingReqdSettingException ( String key, Throwable cause ) { super("Missing required setting \"" + key + "\" because " + cause.getMessage (), cause ); fKey=key; }
		private static final long serialVersionUID = 1L;
		public final String fKey;
	}

	class InvalidSettingValueException extends Exception
	{
		public InvalidSettingValueException ( String key ) { super("Invalid setting for \"" + key + "\"" ); fKey=key; }
		public InvalidSettingValueException ( String key, Throwable cause ) { super("Invalid setting for \"" + key + "\" because " + cause.getMessage (), cause ); fKey=key; }
		public InvalidSettingValueException ( String key, String why ) { super("Invalid setting for \"" + key + "\" because " + why ); fKey=key; }
		public InvalidSettingValueException ( String key, Throwable cause, String why ) { super("Invalid setting for \"" + key + "\" because " + why, cause ); fKey=key; }
		private static final long serialVersionUID = 1L;
		public final String fKey;
	}

	/**
	 * For use with systems like Velocity, that introspect for "get(key)" and expect a value or null
	 * @param key
	 * @return a value, or null
	 */
	String get ( String key );

	String getString ( String key ) throws MissingReqdSettingException;
	String getString ( String key, String defValue );

	char getCharacter ( String key ) throws MissingReqdSettingException;
	char getCharacter ( String key, char defValue );

	boolean getBoolean ( String key ) throws MissingReqdSettingException;
	boolean getBoolean ( String key, boolean defValue );

	int getInt ( String key ) throws MissingReqdSettingException;
	int getInt ( String key, int defValue );

	long getLong ( String key ) throws MissingReqdSettingException;
	long getLong ( String key, long defValue );

	double getDouble ( String key ) throws MissingReqdSettingException;
	double getDouble ( String key, double defValue );

	byte[] getBytes ( String key ) throws MissingReqdSettingException, InvalidSettingValueException;
	byte[] getBytes ( String key, byte[] defValue );

	/**
	 * Get a set of strings given a key. Most implementations expect to use "getString()" and then
	 * split the value by commas.
	 *  
	 * @param key
	 * @return a string array
	 * @throws MissingReqdSettingException
	 */
	String[] getStrings ( String key ) throws MissingReqdSettingException;
	String[] getStrings ( String key, String[] defValue );

	int size ();
	boolean hasValueFor ( String key );
	Collection<String> getAllKeys ();
	Map<String, String> getCopyAsMap ();

	void copyInto ( NvWriteable writeable );
	void copyInto ( Map<String,String> writeable );

	void rescan () throws LoadException;
}
