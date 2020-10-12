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

import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletConfig;

import io.continual.util.nv.impl.nvReadableTable;


/**
 * Wraps a ServletConfig in the settings class used throughout the framework.
 */
public class CHttpServletSettings extends nvReadableTable
{
	public CHttpServletSettings ( ServletConfig sc )
	{
		super ();

		final HashMap<String,String> loaded = new HashMap<>();

		final Enumeration<String> e = sc.getInitParameterNames ();
		while ( e.hasMoreElements () )
		{
			final String name = e.nextElement ();
			final String val = sc.getInitParameter ( name );
			loaded.put ( name, val );
		}

		set ( loaded );
	}
}
