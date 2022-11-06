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

package io.continual.builder.sources;

import java.util.prefs.Preferences;

import io.continual.builder.common.CommonDataSource;

/**
 * A builder data source backed by a Preferences instance.
 */
public class BuilderPrefsDataSource extends CommonDataSource
{
	public BuilderPrefsDataSource ( Preferences data )
	{
		super ( Preferences.class, "fromSettings", data );
		fData = data;
	}

	@Override
	public String getClassNameFromData ()
	{
		return fData.get ( "class", null );
	}

	private final Preferences fData;
}
