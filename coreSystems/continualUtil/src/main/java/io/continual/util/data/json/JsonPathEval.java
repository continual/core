/*
 *	Copyright 2021, Continual.io
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

package io.continual.util.data.json;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonSmartMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

public class JsonPathEval
{
	public static List<String> evaluateJsonPath ( JSONObject root, String jsonPath )
	{
		return JsonPath
			.parse ( root.toString () )	// not sure if this is necessary
			.read ( jsonPath )
		;
	}

	static
	{
		Configuration.setDefaults ( new Configuration.Defaults()
		{
		    @Override
		    public JsonProvider jsonProvider() { return jsonProvider; }

		    @Override
		    public MappingProvider mappingProvider() { return mappingProvider; }
		    
		    @Override
			public Set<Option> options ()
			{
				// return EnumSet.noneOf ( Option.class );
		    	return EnumSet.of ( Option.ALWAYS_RETURN_LIST );
			}

		    private final JsonProvider jsonProvider = new JsonSmartJsonProvider ();
		    private final MappingProvider mappingProvider = new JsonSmartMappingProvider ();
		});
	}
}
