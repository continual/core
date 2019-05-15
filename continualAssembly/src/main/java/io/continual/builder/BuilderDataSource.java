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

package io.continual.builder;

import io.continual.builder.Builder.BuildFailure;

public interface BuilderDataSource
{
	/**
	 * Get the string value for the classname
	 * @return the classname or null
	 */
	String getClassNameFromData () throws BuildFailure;

	/**
	 * Get the class used in the object constructor or static initializer call
	 * @return the class to use in initialization
	 */
	Class<?> getIniterClass ();

	/**
	 * Get the name of the static initializer method on the target class
	 * @return
	 */
	String getIniterName ();

	/**
	 * Get the actual data for the init function (constructor, static fromX(), etc.)
	 * @return a data object
	 */
	Object getInitData ();
}
