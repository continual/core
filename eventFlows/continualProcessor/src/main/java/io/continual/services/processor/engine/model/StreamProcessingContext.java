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

import io.continual.metrics.MetricsCatalog;
import io.continual.iam.identity.Identity;

/**
 * A stream processing context is provided to all processing components in a program
 * and is consistent across messages (but not across streams). It's a good place to
 * store stream-specific data.
 */
public interface StreamProcessingContext
{
	/**
	 * Get this stream's source
	 * @return a source instance
	 */
	Source getSource ();

	/**
	 * report a warning about processing the stream
	 * @param warningText
	 */
	void warn ( String warningText );

	/**
	 * Report that the stream processing has failed.
	 * @param warningText
	 */
	void fail ( String warningText );

	/**
	 * Returns true if fail() has been called
	 * @return
	 */
	boolean failed ();

	/**
	 * Get the identity of the operator running this process.
	 * @return an identity, or null
	 */
	Identity getOperator ();

	/**
	 * Add a named object of any type.
	 * @param name
	 * @param o
	 * @return this context
	 */
	StreamProcessingContext addNamedObject ( String name, Object o );

	/**
	 * Get a raw object reference for the instance with the given name
	 * @param name
	 * @return an object, or null of none is known to this context
	 */
	Object getNamedObject ( String name );

	/**
	 * Get a typed object with the given name.
	 * @param name
	 * @param clazz
	 * @return the named object cast to T, null of none exists.
	 * @throws ClassCastException
	 */
	<T> T getNamedObject ( String name, Class<T> clazz ) throws ClassCastException;

	/**
	 * No suitable object found.
	 */
	public class NoSuitableObjectException extends Exception
	{
		public NoSuitableObjectException () { super (); }
		public NoSuitableObjectException ( String msg ) { super ( msg ); }
		public NoSuitableObjectException ( Throwable t ) { super ( t ); }
		public NoSuitableObjectException ( String msg, Throwable t ) { super ( msg, t ); }
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * Get a typed object with the given name and throw an exception if it's not available.
	 * @param name
	 * @param clazz
	 * @return the named object cast to T
	 * @throws NoSuitableObjectException
	 */
	<T> T getReqdNamedObject ( String name, Class<T> clazz ) throws NoSuitableObjectException;

	/**
	 * Remove a named object from this context, if it exists
	 * @param name
	 * @return this context
	 */
	StreamProcessingContext removeNamedObject ( String name );

	/**
	 * Set a boolean flag
	 * @param flagName
	 * @return value before this call
	 */
	boolean setFlag ( String flagName );

	/**
	 * Get the value of a given flag.
	 * @param flagName
	 * @return true if set, false if clear or unknown
	 */
	boolean checkFlag ( String flagName );

	/**
	 * Clear a flag
	 * @param flagName
	 * @return the value before this call
	 */
	boolean clearFlag ( String flagName );

	/**
	 * Evaluate a ${} expression in the context of this stream processor
	 * @param expression
	 * @return a value, which may be an empty string
	 */
	String evalExpression ( String expression );

	/**
	 * Requeue a message
	 * @param mr
	 */
	void requeue ( MessageAndRouting mr );

	/**
	 * Get the metrics catalog into which this processing context reports. Processors may 
	 * create/use metrics objects at the top-level, which is scoped properly during the call. 
	 * @return a metrics catalog
	 */
	MetricsCatalog getMetrics ();
}
