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

import io.continual.builder.BuilderDataSource;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.common.CommonDataSource;

/**
 * A builder data source backed by the json.org JSON object implementation.
 */
public class BuilderStringDataSource extends CommonDataSource implements BuilderDataSource
{
	public BuilderStringDataSource ( String data )
	{
		super ( String.class, "fromString", data );
	}

	@Override
	public String getClassNameFromData () throws BuildFailure 
	{
		throw new BuildFailure ( "String data source doesn't contain a classname." );
	}
}
