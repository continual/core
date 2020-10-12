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
package io.continual.iam.tags;

import java.util.concurrent.TimeUnit;

import io.continual.iam.exceptions.IamSvcException;

/**
 * Tags are randomized strings that are used to generate text that would be improbable
 * for attackers to guess. They're used for activities like user confirmation via email
 * or password reset request handling.
 *
 */
public interface TagManager
{
	/**
	 * Create a tag for a given user id with a particular type and duration. If a tag for 
	 * the same user with the same type exists, it's replaced with the new tag.
	 * 
	 * @param userId
	 * @param appTagType
	 * @param duration
	 * @param durationTimeUnit
	 * @param nonce used to seed random number generator
	 * @return a tag
	 * @throws IamSvcException 
	 */
	String createTag ( String userId, String appTagType, long duration, TimeUnit durationTimeUnit, String nonce ) throws IamSvcException;

	/**
	 * Retrieves the userId associated with a tag. If the tag has expired, null is returned.
	 *  
	 * @param tag
	 * @return a user ID or null if no entry exists (or an entry existed but expired)
	 * @throws IamSvcException 
	 */
	String getUserIdForTag ( String tag ) throws IamSvcException;

	/**
	 * Remove any matching tag for the given user and type.
	 * @param userId
	 * @param appTagType
	 * @throws IamSvcException 
	 */
	void removeMatchingTag ( String userId, String appTagType ) throws IamSvcException;

	/**
	 * Sweep any expired tags. The tag manager implementation may not actually 
	 * require this operation. In that case, make it a no-op.
	 * @throws IamSvcException 
	 */
	void sweepExpiredTags () throws IamSvcException;
}
