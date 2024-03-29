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

/**
 * An API key has a key string, a secret string, and an associated user.
 */
public interface ApiKey
{
	/**
	 * Get the API key part.
	 * @return a string
	 */
	String getKey ();

	/**
	 * Get the API key's secret part.
	 * @return a string
	 */
	String getSecret ();

	/**
	 * Get the user associated with this key.
	 * @return the user id
	 */
	String getUserId ();

	/**
	 * Get the creation timestamp
	 * @return the API creation timestamp
	 */
	long getCreationTimestamp ();
}
