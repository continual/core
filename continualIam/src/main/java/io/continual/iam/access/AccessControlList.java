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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.iam.access.AccessControlEntry.Access;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class AccessControlList
{
	// common operations
	public static final String CREATE = "create";
	public static final String READ = "read";
	public static final String UPDATE = "update";
	public static final String DELETE = "delete";

	public AccessControlList ( AclUpdateListener listener )
	{
		fOwner = null;
		fEntries = new LinkedList<AccessControlEntry> ();
		fListener = listener;
	}

	public String getOwner ()
	{
		return fOwner;
	}

	public AccessControlList setOwner ( String userOrGroupId )
	{
		fOwner = userOrGroupId;
		if ( fListener != null )
		{
			fListener.onAclUpdate ( this );
		}
		return this;
	}

	public AccessControlList permit ( String userOrGroupId, String op )
	{
		return addAclEntry ( new AccessControlEntry ( userOrGroupId, Access.PERMIT, op ) );
	}

	public AccessControlList permit ( String userOrGroupId, String... ops )
	{
		return addAclEntry ( new AccessControlEntry ( userOrGroupId, Access.PERMIT, ops ) );
	}

	public AccessControlList deny ( String userOrGroupId, String op )
	{
		return addAclEntry ( new AccessControlEntry ( userOrGroupId, Access.DENY, op ) );
	}

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
	 * @throws AclUpdateException 
	 */
	public AccessControlList clear ( String userOrGroupId, String[] ops )
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

	public AccessControlList clear ()
	{
		fEntries.clear ();
		if ( fListener != null )
		{
			fListener.onAclUpdate ( this );
		}
		return this;
	}

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

	public boolean canUser ( Identity user, String op ) throws IamSvcException
	{
		return canUser ( user.getId (), user.getGroupIds (), op );
	}

	public boolean canUser ( String userId, Set<String> groups, String op )
	{
		final boolean isOwner = userId.equals ( getOwner() );

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
	
	public String serialize ()
	{
		return asJson().toString ();
	}

	public static AccessControlList initialize ( AclUpdateListener listener )
	{
		return new AccessControlList ( listener );
	}

	public static AccessControlList deserialize ( String s, AclUpdateListener listener )
	{
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( s ) );
		return deserialize ( o, listener );
	}

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

	public AclUpdateListener getListener () { return fListener; }

	private String fOwner;
	private final LinkedList<AccessControlEntry> fEntries;
	private final AclUpdateListener fListener;
}
