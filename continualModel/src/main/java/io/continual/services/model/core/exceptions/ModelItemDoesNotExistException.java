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

package io.continual.services.model.core.exceptions;

import io.continual.services.model.core.ModelObjectPath;

/**
 * An exception thrown when a referenced object does not exist.
 */
public class ModelItemDoesNotExistException extends ModelServiceRequestException
{
	public ModelItemDoesNotExistException ( ModelObjectPath p ) { super(p.toString()); }
	public ModelItemDoesNotExistException ( String msg ) { super(msg); }
	public ModelItemDoesNotExistException ( Throwable t ) { super(t); }
	private static final long serialVersionUID = 1L;
}
