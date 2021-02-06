package io.continual.basesvcs.model.user;

import org.json.JSONObject;

import io.continual.iam.identity.Identity;

import io.continual.basesvcs.util.JsonSerialized;

public class UserContext implements JsonSerialized
{
	public static class Builder
	{
		public Builder forUser ( Identity i ) { fForWhom = i; return this; }
		public Builder sponsoredByUser ( Identity i ) { fByWhom = i; return this; }

		public UserContext build ()
		{
			return new UserContext ( fForWhom, fByWhom );
		}

		private Identity fForWhom;
		private Identity fByWhom;
	};

	/**
	 * Get the identity for the user that this transaction is being "executed as".
	 * @return
	 */
	public Identity getUser () { return fIdentity; }

	/**
	 * Get the identity of the user that is actually authenticated, which may be
	 * different from the "executed as" user.
	 * @return
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

	private UserContext ( Identity user, Identity byWhom )
	{
		fIdentity = user;
		fSponsor = ( byWhom != null && !byWhom.getId ().equals ( user.getId () ) ) ? byWhom : null;
	}

	private final Identity fIdentity;
	private final Identity fSponsor;
}
