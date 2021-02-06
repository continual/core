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

package io.continual.http.service.framework.routing.playish;

import java.util.List;

import io.continual.http.service.framework.context.CHttpRequestContext;

public class RedirectHandler implements CHttpPlayishRouteHandler
{
	public static final String kMaxAge = "chttp.staticDir.cache.maxAgeSeconds";
	
	public RedirectHandler ( String loc )
	{
		fTargetLocation = loc;
	}

	@Override
	public void handle ( CHttpRequestContext context, List<String> args )
	{
		context.response ().redirect ( fTargetLocation );
	}

	private final String fTargetLocation;

	@Override
	public boolean actionMatches(String fullPath)
	{
		return false;
	}
}
