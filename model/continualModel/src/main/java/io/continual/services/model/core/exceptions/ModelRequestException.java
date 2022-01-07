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

import org.json.JSONObject;

import io.continual.util.data.json.JsonUtil;

/**
 * An exception related to fulfilling a model service request
 */
public class ModelRequestException extends Exception
{
	public ModelRequestException ( String msg ) { super(msg); fDetails = null; }
	public ModelRequestException ( String msg, JSONObject details ) { super(msg); fDetails = JsonUtil.clone ( details ); }
	public ModelRequestException ( Throwable t ) { super(t); fDetails = null; }

	public JSONObject getDetails () { return fDetails; }

	private final JSONObject fDetails;
	
	private static final long serialVersionUID = 1L;
}
