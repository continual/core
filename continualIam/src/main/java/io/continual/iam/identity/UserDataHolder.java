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
package io.continual.iam.identity;

import java.util.Map;

import io.continual.iam.exceptions.IamSvcException;

/**
 * A container for user-oriented data. In addition to being extended by Identity,
 * the Group interface can also hold user-oriented data.
 */
public interface UserDataHolder
{
	/**
	 * reload this object from the server
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	void reload () throws IamSvcException;

	/**
	 * Get a named data value.
	 * @param key a key for a user data entry
	 * @return a value or null
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	String getUserData ( String key ) throws IamSvcException;

	/**
	 * Put a named data value.
	 * @param key a key for the user data entry
	 * @param val a value to store
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	void putUserData ( String key, String val ) throws IamSvcException;

	/**
	 * Remove a named data value.
	 * @param key a key for a user data entry
	 */
	void removeUserData ( String key ) throws IamSvcException;

	/**
	 * Get all of the user data
	 * @return a map of user data
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	Map<String,String> getAllUserData () throws IamSvcException;
}
