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

package io.continual.services.processor.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A pipeline of rules.
 */
public class Pipeline
{
	public Pipeline ()
	{
		this ( new ArrayList<Rule> () );
	}

	public Pipeline ( List<Rule> rules )
	{
		fRules = new ArrayList<> ();
		fRules.addAll ( rules );
	}

	public Pipeline addRule ( Rule r )
	{
		fRules.add ( r );
		return this;
	}

	public void process ( MessageProcessingContext context )
	{
		for ( Rule r : fRules )
		{
			final List<Processor> procs;

			final Filter f = r.getFilter ();
			if ( f == null || f.passes ( context ) )
			{
				procs = r.getThenProcs ();
			}
			else
			{
				procs = r.getElseProcs ();
			}

			for ( Processor p : procs )
			{
				p.process ( context );
				if ( !context.shouldContinue () )
				{
					// break from the processor loop
					break;
				}
			}

			// break from the rule loop
			if ( !context.shouldContinue () )
			{
				break;
			}
		}
	}

	private final ArrayList<Rule> fRules;
}
