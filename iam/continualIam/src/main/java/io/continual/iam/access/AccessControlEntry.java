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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.json.JsonVisitor;

/**
 * An access control entry, which has an identity, a permit/deny access
 * flag, and a set of operation labels that are specific to the service
 * that owns the ACL.
 */
public class AccessControlEntry
{
	/**
	 * Special user setting "any user"
	 */
	public static final String kAnyUser = "*";

	/**
	 * Special setting "any operation"
	 */
	public static final String kAnyOperation = "*";

	/**
	 * Special user setting "owner"
	 */
	public static final String kOwner = "~owner~";

	/**
	 * ACL entry permission; permit or deny
	 */
	public enum Access
	{
		PERMIT,
		DENY;

		public static Access deserialize ( String val )
		{
			val = val.trim ().toUpperCase ();
			if ( val.equals ( "P" ) ) val = Access.PERMIT.toString ();
			if ( val.equals ( "D" ) ) val = Access.DENY.toString ();
			return Access.valueOf ( val );
		}
	}

	public static class Builder
	{
		public Builder forSubject ( String userOrGroupId ) { fUserOrGroupId = userOrGroupId; return this; }
		public Builder forAllUsers () { return forSubject ( kAnyUser ); }
		public Builder forOwner () { return forSubject ( kOwner ); }

		public Builder permit () { return withAccess ( Access.PERMIT ); }
		public Builder deny () { return withAccess ( Access.DENY ); }
		public Builder withAccess ( Access a ) { fAccess = a; return this; }

		public Builder operation ( String op ) { fOps.add ( op ); return this; }
		public Builder operations ( String... ops )
		{
			for ( String op : ops )
			{
				fOps.add ( op );
			}
			return this;
		}
		public Builder operations ( Collection<String> ops ) { fOps.addAll ( ops ); return this; }
		public Builder forAnyOperation () { return operation ( kAnyOperation ); }

		public AccessControlEntry build () { return new AccessControlEntry ( this ); }

		private String fUserOrGroupId = null;
		private Access fAccess = Access.PERMIT;
		private LinkedList<String> fOps = new LinkedList<> ();
	}

	/**
	 * Create a builder for an ACL entry
	 * @return a new builder
	 */
	public static Builder builder() { return new Builder (); }

	public AccessControlEntry ( AccessControlEntry that )
	{
		fWho = that.fWho;
		fPermission = that.fPermission;

		fOperations = new TreeSet<String> ();
		for ( String op : that.fOperations )
		{
			fOperations.add ( op );
		}
	}

	public AccessControlEntry ( String userOrGroupId, Access p, String operation )
	{
		this ( userOrGroupId, p, new String [] { operation } );
	}

	public AccessControlEntry ( String userOrGroupId, Access p, String[] operations )
	{
		fWho = userOrGroupId;
		fOperations = new TreeSet<String> ();
		fPermission = p;

		for ( String op : operations )
		{
			fOperations.add ( op );
		}
	}

	public AccessControlEntry ( String userOrGroupId, Access p, Collection<String> a )
	{
		fWho = userOrGroupId;
		fPermission = p;
		fOperations = new TreeSet<String> ( a );

		if ( fWho == null ) throw new IllegalArgumentException ( "ACL entry requires a subject." );
		if ( fPermission == null ) throw new IllegalArgumentException ( "ACL entry requires a permission." );
		if ( fOperations.size () == 0 ) throw new IllegalArgumentException ( "ACL entry requires at least one operation." );
	}

	@Override
	public AccessControlEntry clone ()
	{
		return new AccessControlEntry ( fWho, fPermission, fOperations );
	}

	/**
	 * Get an access permission for a given user ID or group set on a given operation. If the entry
	 * doesn't match the user/groups, then null is returned.
	 * 
	 * @param userId a user ID 
	 * @param groups a group set, presumably associated with the user
	 * @param isOwner true if the user Id is the ACL owner (which is not visible to entries)
	 * @param op the operation, which is checked as a case-insensitive string
	 * @return an access permission or null if no match
	 */
	public Access check ( String userId, Set<String> groups, boolean isOwner, String op )
	{
		// does this entry apply to the given user?
		if (
			fWho.equals ( kAnyUser ) ||							// the entry applies to any user
			fWho.equals ( userId ) ||							// the entry applies to the given user explicitly
			( groups != null && groups.contains ( fWho ) ) ||	// the entry applies to a group in the group set
			( isOwner && (fWho.equals(kOwner) ) )				// the user is the ACL owner and this entry is for the owner
		)
		{
			for ( String listedOp : fOperations )
			{
				if ( listedOp.equalsIgnoreCase ( op ) || listedOp.equals ( kAnyOperation ) )
				{
					return fPermission;
				}
			}
			// here, the operation isn't covered in this ACL entry
		}
		// else: not relevant for this user

		return null;
	}

	/**
	 * Get the subject of this ACL entry
	 * @return the subject
	 */
	public String getSubject () { return fWho; }

	/**
	 * Get the permission for this ACL entry
	 * @return PERMIT or DENY
	 */
	public Access getPermission () { return fPermission; }

	/**
	 * Get the operation set in this ACL entry
	 * @return a set of operations
	 */
	public Set<String> getOperationSet ()
	{
		return new TreeSet<String> ( fOperations );
	}

	/**
	 * Get the operation set in this ACL entry
	 * @return an array of operations
	 */
	public String[] getOperations ()
	{
		return fOperations.toArray ( new String[ fOperations.size () ] );
	}

	/**
	 * Get the number of operations in this ACL entry
	 * @return the count of operations
	 */
	public int getOperationCount ()
	{
		return fOperations.size ();
	}

	/**
	 * Remove the operation.
	 * @param op the operation to remove
	 * @return true if there was a change
	 */
	public boolean removeOperation ( String op )
	{
		return fOperations.remove ( op );
	}
	
	@Override
	public String toString ()
	{
		return serialize ().toString ();
	}

	/**
	 * Serialize this ACL entry to a JSON object
	 * @return a json object
	 */
	public JSONObject serialize ()
	{
		final JSONArray ops = new JSONArray ();
		for ( String op : fOperations )
		{
			ops.put ( op );
		}
		
		return new JSONObject ()
			.put ( "w", fWho )
			.put ( "a", fPermission == Access.PERMIT ? "p" : "d" )
			.put ( "o", ops )
		;
	}

	/**
	 * Deserialize a JSON object created by serialize()
	 * @param o a JSON object
	 * @return an ACL entry
	 */
	public static AccessControlEntry deserialize ( JSONObject o )
	{
		return builder()
			.forSubject ( readString ( o, "w", "who" ) )
			.withAccess ( Access.deserialize ( readString ( o, "a", "access" ) ) )
			.operations ( JsonVisitor.arrayToList ( readArray ( o, "o", "operations" ) ) )
			.build ()
		;
	}

	@Override
	public int hashCode ()
	{
		return Objects.hash ( fOperations, fPermission, fWho );
	}

	@Override
	public boolean equals ( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass () != obj.getClass () )
			return false;
		AccessControlEntry other = (AccessControlEntry) obj;
		return Objects.equals ( fOperations, other.fOperations )
			&& fPermission == other.fPermission
			&& Objects.equals ( fWho, other.fWho );
	}

	private final String fWho;
	private final TreeSet<String> fOperations;
	private final Access fPermission;

	private AccessControlEntry ( Builder b )
	{
		this ( b.fUserOrGroupId, b.fAccess, b.fOps );
	}

	private static String readString ( JSONObject o, String... keys )
	{
		for ( String key : keys )
		{
			final String v = o.optString ( key, null );
			if ( v != null ) return v;
		}
		return null;
	}
	
	private static JSONArray readArray ( JSONObject o, String... keys )
	{
		for ( String key : keys )
		{
			final JSONArray v = o.optJSONArray ( key );
			if ( v != null ) return v;
		}
		return null;
	}
}
