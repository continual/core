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

package io.continual.services.processor.config.readers;

public class ConfigReadException extends Exception
{
	public ConfigReadException ()
	{
	}

	public ConfigReadException ( String msg )
	{
		super ( msg );
	}

	public ConfigReadException ( Throwable t )
	{
		super ( t );
	}

	public ConfigReadException ( String msg, Throwable t )
	{
		super ( msg, t );
	}

	private static final long serialVersionUID = 1L;
}
