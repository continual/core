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

package io.continual.services.model.core;

import java.util.Iterator;

/**
 * A list of model objects. This list could be thousands of entries long and is therefore
 * presented only as an iterator.
 */
public interface ModelObjectList extends Iterable<ModelObject>
{
	static ModelObjectList emptyList ()
	{
		return new ModelObjectList ()
		{
			@Override
			public Iterator<ModelObject> iterator ()
			{
				return new Iterator<ModelObject> ()
				{
					@Override
					public boolean hasNext () { return false; }

					@Override
					public ModelObject next () { return null; }
				};
			}
		};
	}
}
