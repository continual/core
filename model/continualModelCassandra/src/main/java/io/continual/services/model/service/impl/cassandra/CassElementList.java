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

package io.continual.services.model.service.impl.cassandra;

import java.util.Set;
import java.util.TreeSet;

import io.continual.services.model.service.ModelElementList;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

class CassElementList implements ModelElementList
{
	public CassElementList ( Path base, Set<String> names )
	{
		fSet = new TreeSet<> ();
		for ( String name : names )
		{
			fSet.add ( base.makeChildItem ( Name.fromString ( name ) ) );
		}
	}

	public CassElementList ( Set<Path> elements )
	{
		fSet = elements;
	}

	@Override
	public ResponseType getResponseType ()
	{
		return ResponseType.LISTING;
	}

	@Override
	public Iterable<Path> getElements ()
	{
		return fSet;
	}

	private final Set<Path> fSet;
}
