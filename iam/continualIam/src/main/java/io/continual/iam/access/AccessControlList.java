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
package io.continual.iam.access;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

/**
 * An access control list, which has an owner and an ordered list of ACL entries. 
 */
public class AccessControlList
{
	// common operations. These are here for convenience are are not required to be used
	// in ACL entries.
	public static final String CREATE = "create";
	public static final String READ = "read";
	public static final String UPDATE = "update";
	public static final String DELETE = "delete";

	public static class Builder 
	{
		public Builder ownedBy ( String userOrGroupId ) { fOwner = userOrGroupId; return this; }
		public Builder withEntry ( AccessControlEntry ace ) { fAces.add ( ace ); return this; }
		public Builder withListener ( AclUpdateListener l ) { fListener = l; return this; }

		public AccessControlList build () { return new AccessControlList ( this ); }

		private String fOwner = null;
		private LinkedList<AccessControlEntry> fAces = new LinkedList<> ();
		private AclUpdateListener fListener = null;
	}

	/**
	 * Create a builder for an ACL
	 * @return a new builder
	 */
	public static Builder builder () { return new Builder (); }
	
	/**
	 * Construct an empty ACL
	 */
	public AccessControlList ()
	{
		this ( (AclUpdateListener) null );
	}

	/**
	 * Construct an ACL with the given update listener
	 * @param listener a listener, which may be null
	 */
	public AccessControlList ( AclUpdateListener listener )
	{
		fOwner = null;
		fEntries = new LinkedList<AccessControlEntry> ();
		fListener = listener;
	}

	/**
	 * Get the owner ID for this ACL
	 * @return the owner, which may be null
	 */
	public String getOwner ()
	{
		return fOwner;
	}

	/**
	 * Set the owner ID for this ACL. The listener is updated if provided.
	 * @param userOrGroupId The ID to use as owner. This may be null.
	 * @return this ACL
	 */
	public AccessControlList setOwner ( String userOrGroupId )
	{
		fOwner = userOrGroupId;
		if ( fListener != null )
		{
			fListener.onAclUpdate ( this );
		}
		return this;
	}

	/**
	 * Permit the given ID to perform the given operations by adding a new entry to the end of the ACL entry list.
	 * Note that a conflicting entry earlier in the list will take precedence.
	 * @param userOrGroupId the user or group ID
	 * @param ops one or more operations
	 * @return this ACL
	 */
	public AccessControlList permit ( String userOrGroupId, String... ops )
	{
		return addAclEntry ( new AccessControlEntry ( userOrGroupId, Access.PERMIT, ops ) );
	}

	/**
	 * Deny the given ID from performing the given operations by adding a new entry to the end of the ACL entry list.
	 * Note that a conflicting entry earlier in the list will take precedence.
	 * @param userOrGroupId the user or group ID
	 * @param ops one or more operations
	 * @return this ACL
	 */
	public AccessControlList deny ( String userOrGroupId, String... ops )
	{
		return addAclEntry ( new AccessControlEntry ( userOrGroupId, Access.DENY, ops ) );
	}

	/**
	 * Clear matching entries. This method removes a matching entry and is different
	 * from deny(), which adds an entry that explicitly denies access. 
	 *  
	 * @param userOrGroupId the user or group to clear entries for
	 * @param op the operation to clear entries for
	 * @return this ACL
	 */
	public AccessControlList clear ( String userOrGroupId, String op )
	{
		return clear ( userOrGroupId, new String[] { op } );
	}

	/**
	 * Clear matching entries. This method removes a matching entry and is different
	 * from deny(), which adds an entry that explicitly denies access. 
	 *  
	 * @param userOrGroupId the user or group to clear entries for
	 * @param ops the operations to clear entries for
	 * @return this ACL
	 */
	public AccessControlList clear ( String userOrGroupId, String... ops )
	{
		boolean changed = false;

		final LinkedList<AccessControlEntry> removals = new LinkedList<AccessControlEntry> ();

		// look for matching entries
		for ( AccessControlEntry e : fEntries )
		{
			if ( e.getSubject ().equals ( userOrGroupId ) )
			{
				for ( String op : ops )
				{
					final boolean change = e.removeOperation ( op );
					changed = changed || change;
				}
				if ( e.getOperationCount() == 0 )
				{
					removals.add ( e );
				}
			}
			// else: unrelated entry
		}

		// remove empty entries
		for ( AccessControlEntry e : removals )
		{
			boolean change = fEntries.remove ( e );
			changed = changed || change;
		}

		// updates
		if ( changed && fListener != null )
		{
			fListener.onAclUpdate ( this );
		}

		return this;
	}

	/**
	 * Clear all entries from this ACL and notify the listener if present
	 * @return this ACL
	 */
	public AccessControlList clear ()
	{
		fEntries.clear ();
		if ( fListener != null )
		{
			fListener.onAclUpdate ( this );
		}
		return this;
	}

	/**
	 * Get the list of ACL entries on this ACL
	 * @return a list of 0 or more entries
	 */
	public List<AccessControlEntry> getEntries ()
	{
		final LinkedList<AccessControlEntry> result = new LinkedList<> ();
		for ( AccessControlEntry e : fEntries )
		{
			// clone the entries because the operation list can be updated on the entry (i.e. they're not quite
			// immutable classes)
			result.add ( e.clone () );
		}
		return result;
	}

	/**
	 * Can the given user perform the given operation based on this ACL?
	 * @param user a user
	 * @param op an operation
	 * @return true if the user can perform the given operation
	 * @throws IamSvcException if there's an error during processing
	 */
	public boolean canUser ( Identity user, String op ) throws IamSvcException
	{
		if ( user == null )
		{
			return canUser ( null, new TreeSet<String> (), op );
		}
		else
		{
			return canUser ( user.getId (), user.getGroupIds (), op );
		}
	}

	/**
	 * Can the given user ID or group set perform the given operation based on this ACL? 
	 * @param userId a user ID
	 * @param groups a set of 0 or more groups
	 * @param op an operation
	 * @return true if the user or group set can perform the given operation
	 */
	public boolean canUser ( String userId, Set<String> groups, String op )
	{
		final boolean isOwner = userId != null && userId.equals ( getOwner() );

		for ( AccessControlEntry e : getEntries() )
		{
			final AccessControlEntry.Access p = e.check ( userId, groups, isOwner, op );
			if ( p != null )
			{
				if ( p.equals ( AccessControlEntry.Access.DENY ) ) return false;
				if ( p.equals ( AccessControlEntry.Access.PERMIT ) ) return true;
			}
		}
		return false;
	}

	/**
	 * Add the given ACL entry to this ACL's list of entries.
	 * @param acle an ACL entry
	 * @return this ACL
	 */
	public AccessControlList addAclEntry ( AccessControlEntry acle )
	{
		fEntries.add ( acle );
		if ( fListener != null )
		{
			fListener.onAclUpdate ( this );
		}
		return this;
	}

	@Override
	public String toString ()
	{
		return serialize ();
	}

	/**
	 * Serialize to JSON
	 * @return a JSON object
	 */
	public JSONObject asJson ()
	{
		final JSONArray entries = new JSONArray ();
		for ( AccessControlEntry e : getEntries() )
		{
			entries.put ( e.serialize () );
		}
		return new JSONObject ()
			.put ( "owner", fOwner )
			.put ( "entries", entries )
		;
	}

	/**
	 * Serialize to a JSON String
	 * @return a string
	 */
	public String serialize ()
	{
		return asJson().toString ();
	}

	public static AccessControlList initialize ( AclUpdateListener listener )
	{
		return new AccessControlList ( listener );
	}

	/**
	 * Deserialize a string created by serialize()
	 * @param s a string serialized ACL
	 * @param listener an optional listener
	 * @return an ACL
	 */
	public static AccessControlList deserialize ( String s, AclUpdateListener listener )
	{
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( s ) );
		return deserialize ( o, listener );
	}

	/**
	 * Deserialize a JSON object created by serialize() or asJson()
	 * @param o a JSON object serialized ACL
	 * @param listener an optional listener
	 * @return an ACL
	 */
	public static AccessControlList deserialize ( JSONObject o, AclUpdateListener listener )
	{
		if ( o == null )
		{
			return initialize ( listener );
		}

		final AccessControlList acl = new AccessControlList ( listener );
		acl.fOwner = o.optString ( "owner", null );
		JsonVisitor.forEachElement ( o.optJSONArray ( "entries" ), new ArrayVisitor<JSONObject,JSONException>()
		{
			@Override
			public boolean visit ( JSONObject t ) throws JSONException
			{
				final AccessControlEntry e = AccessControlEntry.deserialize ( t );
				acl.fEntries.add ( e );
				return true;
			}
		} );
		return acl;
	}

	/**
	 * Get the listener on this ACL if present
	 * @return a listener or null
	 */
	public AclUpdateListener getListener ()
	{
		return fListener;
	}

	private String fOwner;
	private final LinkedList<AccessControlEntry> fEntries;
	private final AclUpdateListener fListener;

	private AccessControlList ( Builder b )
	{
		fOwner = b.fOwner;
		fListener = b.fListener;

		fEntries = new LinkedList<> ();
		fEntries.addAll ( b.fAces );
	}
}
