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
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

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
	 * Special user setting "owner"
	 */
	public static final String kOwner = "~owner~";

	/**
	 * ACL entry permission; permit or deny
	 */
	public enum Access
	{
		PERMIT,
		DENY
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
	}

	@Override
	public AccessControlEntry clone ()
	{
		return new AccessControlEntry ( fWho, fPermission, fOperations );
	}
	
	public Access check ( String userId, Set<String> groups, boolean isOwner, String op )
	{
		if ( fWho.equals ( kAnyUser ) || fWho.equals ( userId ) || groups.contains ( fWho ) ||
			( isOwner && (fWho.equals(kOwner) ) ) )
		{
			for ( String a : fOperations )
			{
				if ( a.equalsIgnoreCase ( op ) )
				{
					return fPermission;
				}
			}
			// here, the operation isn't covered in this ACL entry
		}
		// else: not relevant for this user

		return null;
	}

	public String getSubject () { return fWho; }
	public Access getPermission () { return fPermission; }

	public Set<String> getOperationSet ()
	{
		return new TreeSet<String> ( fOperations );
	}

	public String[] getOperations ()
	{
		int i=0;
		final String[] result = new String [ fOperations.size () ];
		for ( String op : getOperationSet () )
		{
			result[i++] = op;
		}
		return result;
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

	public int getOperationCount ()
	{
		return fOperations.size ();
	}
	
	@Override
	public String toString ()
	{
		return serialize ().toString ();
	}

	public JSONObject serialize ()
	{
		final JSONArray ops = new JSONArray ();
		for ( String op : fOperations )
		{
			ops.put ( op );
		}
		
		return new JSONObject ()
			.put ( "who", fWho )
			.put ( "access", fPermission.toString () )
			.put ( "operations", ops )
		;
	}

	public static AccessControlEntry deserialize ( JSONObject o )
	{
		final LinkedList<String> ops = new LinkedList<> ();
		JsonVisitor.forEachElement ( o.getJSONArray ( "operations" ), new ArrayVisitor<String,JSONException>()
		{
			@Override
			public boolean visit ( String op ) throws JSONException
			{
				ops.add ( op );
				return true;
			}
		} );
		
		return new AccessControlEntry (
			o.getString ( "who" ),
			Access.valueOf ( o.getString ( "access" ) ),
			ops.toArray ( new String[ops.size()] )
		);
	}

	private final String fWho;
	private final TreeSet<String> fOperations;
	private final Access fPermission;
}
