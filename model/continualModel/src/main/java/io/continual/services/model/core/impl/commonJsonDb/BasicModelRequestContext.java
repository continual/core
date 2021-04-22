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

package io.continual.services.model.core.impl.commonJsonDb;

import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelRequestContext;

public class BasicModelRequestContext implements ModelRequestContext
{
	public BasicModelRequestContext ( UserContext user )
	{
		this ( user.getUser (), CacheControl.READ_AND_WRITE );
	}

	public BasicModelRequestContext ( Identity user )
	{
		this ( user, CacheControl.READ_AND_WRITE );	// FIXME: not actually running yet
	}

	public BasicModelRequestContext ( Identity user, CacheControl caching )
	{
		fUser = user;
		fCacheControl = caching;
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
	public ModelObject get ( ModelObjectPath key )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put ( ModelObjectPath key, ModelObject o )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getRawData ( String key )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getRawData ( String key, Class<T> c )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putRawData ( String key, Object value )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doesNotExist ( ModelObjectPath key )
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean knownToNotExist ( ModelObjectPath key )
	{
		// TODO Auto-generated method stub
		return false;
	}

	private final Identity fUser;
	private final CacheControl fCacheControl;
}