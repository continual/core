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
package io.continual.util.nv;

import java.util.Map;

/**
 * Write interface for a name/value pair container.
 */
public interface NvWriteable extends NvReadable
{
	void clear ();
	void unset ( String key );

	void set ( String key, String value );
	void set ( String key, char value );
	void set ( String key, boolean value );
	void set ( String key, int value );
	void set ( String key, long value );
	void set ( String key, double value );
	void set ( String key, byte[] value );
	void set ( String key, byte[] value, int offset, int length );
	void set ( String key, String[] value );
	void set ( Map<String,String> map );
}
