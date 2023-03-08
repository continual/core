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

package io.continual.services.model.service;

import io.continual.services.Service;
import io.continual.services.model.session.ModelSessionBuilder;

/**
 * The model service, a global view over all models everywhere.
 * By "all models" we really mean all of them, including models that belong
 * to other users or are shared data.
 */
public interface ModelService extends Service
{
	/**
	 * Build a session with the service
	 * @return a session builder
	 */
	ModelSessionBuilder sessionBuilder ();
}
