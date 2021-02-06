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

public class ExprDataSourceStack implements ExprDataSource
{
	public ExprDataSourceStack ( ExprDataSource... sources )
	{
		fSources = sources;
	}
	
	@Override
	public Object eval ( String label )
	{
		for ( ExprDataSource src : fSources )
		{
			final Object val = src.eval ( label );
			if ( val != null ) return val;
		}
		return null;
	}

	private final ExprDataSource[] fSources;
}
