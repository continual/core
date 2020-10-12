/*
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

package io.continual.http.service.framework;

import java.util.Collection;
import java.util.Map;

import io.continual.util.nv.impl.nvBaseReadable;
import io.continual.util.nv.impl.nvWriteableTable;

/**
 * The runtime controls are read like regular settings in the system, but are not
 * expected to be cached.
 */
public class CHttpRuntimeControls extends nvBaseReadable
{
	public static final String kSetting_LogHeaders = "chttp.logging.requestHeaders";

	public CHttpRuntimeControls ()
	{
		fTable = new nvWriteableTable ();
	}

	public void setLogHeaders ( boolean b )
	{
		fTable.set ( kSetting_LogHeaders, b );
	}
	
	@Override
	public int size ()
	{
		return fTable.size();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		return fTable.getAllKeys();
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		return fTable.getCopyAsMap();
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return fTable.hasValueFor( key );
	}

	@Override
	public String getString ( String key )
		throws MissingReqdSettingException
	{
		return fTable.getString ( key );
	}

	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		final String fullset = getString ( key );
		return fullset.split ( ",", -1 );
	}

	private final nvWriteableTable fTable;
}
