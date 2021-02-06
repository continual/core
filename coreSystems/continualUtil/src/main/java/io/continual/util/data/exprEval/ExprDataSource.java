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

package io.continual.util.data.exprEval;

public interface ExprDataSource
{
	/**
	 * get the value of an object given a label
	 * @param label the data value's key
	 * @return a data object
	 */
	Object eval ( String label );

	default String evalToString ( String label )
	{
		return evalToString ( label, null );
	}

	default String evalToString ( String label, String defval )
	{
		final Object val = eval ( label );
		if ( val != null ) return val.toString ();
		return defval;
	}
}
