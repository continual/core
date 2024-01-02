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

package io.continual.services.model.impl.common;

import java.util.HashMap;
import java.util.TreeSet;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.util.naming.Path;

public class BasicModelRequestContext implements ModelRequestContext
{
	public BasicModelRequestContext ( BasicModelRequestContextBuilder mrcBuilder ) throws BuildFailure
	{
		fUser = mrcBuilder.fUser;
		fCacheControl = CacheControl.READ_AND_WRITE;
		fObjects = new HashMap<> ();
		fKnownNotToExist = new TreeSet<> ();
		fSchemaReg = mrcBuilder.fSchemaReg;
		fNotificationService = mrcBuilder.fNotificationSvc;
	}

	@Override
	public void close ()
	{
	}
	
	@Override
	public Identity getOperator ()
	{
		return fUser;
	}

	@Override
	public CacheControl getCacheControl ()
	{
		return fCacheControl;
	}

	@Override
	public ModelObject get ( Path key )
	{
		return fObjects.get ( key );
	}

	@Override
	public void put ( Path key, ModelObject o )
	{
		fObjects.put ( key, o );
		fKnownNotToExist.remove ( key );
	}

	@Override
	public void remove ( Path objectPath )
	{
		fObjects.remove ( objectPath );
		fKnownNotToExist.add ( objectPath );
	}

	@Override
	public void doesNotExist ( Path key )
	{
		fKnownNotToExist.add ( key );
		fObjects.remove ( key );
	}

	@Override
	public boolean knownToNotExist ( Path key )
	{
		return fKnownNotToExist.contains ( key );
	}

	@Override
	public ModelSchemaRegistry getSchemaRegistry ()
	{
		return fSchemaReg;
	}

	@Override
	public ModelNotificationService getNotificationService ()
	{
		return fNotificationService;
	}
	
	private final Identity fUser;
	private final CacheControl fCacheControl;
	private final ModelSchemaRegistry fSchemaReg;
	private final ModelNotificationService fNotificationService;

	private final HashMap<Path,ModelObject> fObjects;
	private final TreeSet<Path> fKnownNotToExist;
}
