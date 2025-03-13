/*
 *  Copyright (c) 2006-2025 Continual.io. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.continual.http.service.framework;

import java.io.IOException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.util.nv.NvReadable;

public interface CHttpRouteInstaller
{
	/**
	 * Build the router for this service
	 * @param rr
	 * @param p
	 * @throws IOException
	 * @throws BuildFailure 
	 */
	void setupRouter ( CHttpRequestRouter rr, NvReadable p ) throws IOException, BuildFailure;
}
