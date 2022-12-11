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

import org.json.JSONObject;

import io.continual.util.data.json.JsonSerialized;

public class UserContext<I extends Identity> implements JsonSerialized
{
	public static class Builder<I extends Identity>
	{
		public Builder<I> forUser ( I i ) { fForWhom = i; return this; }
		public Builder<I> sponsoredByUser ( I i ) { fByWhom = i; return this; }

		public UserContext<I> build ()
		{
			return new UserContext<I> ( fForWhom, fByWhom );
		}

		private I fForWhom;
		private I fByWhom;
	};

	/**
	 * Create a builder for an UserContext
	 * @return a new builder
	 */
	public static Builder<Identity> builder () { return new Builder<Identity> (); }

	/**
	 * Get the identity for the user that this transaction is being "executed as".
	 * @return an identity
	 */
	public I getUser () { return fIdentity; }

	/**
	 * Get the identity of the user that is actually authenticated, which may be
	 * different from the "executed as" user.
	 * @return an identity
	 */
	public Identity getSponsor () { return fSponsor != null ? fSponsor : fIdentity; }

	/**
	 * Get the ID of the effective user. Equivalent to getUser().getId()
	 * @return the effective user ID
	 */
	public String getEffectiveUserId () { return getUser().getId (); }

	/**
	 * Get the ID of the actual authenticated user. Equivalent to getSponsor().getId()
	 * @return the actual user ID
	 */
	public String getActualUserId () { return getSponsor().getId (); }

	@Override
	public String toString ()
	{
		if ( fSponsor != null )
		{
			return fIdentity.getId () + " (" + fSponsor.getId () + ")";
		}
		else
		{
			return fIdentity.getId ();
		}
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "identity", fIdentity.getId () )
		;
		if ( fSponsor != null )
		{
			result.put ( "sponsor", fSponsor.getId () );
		}
		return result;
	}

	private UserContext ( I user, I byWhom )
	{
		fIdentity = user;
		fSponsor = ( byWhom != null && !byWhom.getId ().equals ( user.getId () ) ) ? byWhom : null;
	}

	private final I fIdentity;
	private final I fSponsor;
}
