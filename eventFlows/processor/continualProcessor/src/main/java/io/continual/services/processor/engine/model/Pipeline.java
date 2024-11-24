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

import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsCatalog.PathPopper;
import io.continual.metrics.metricTypes.Timer;

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

	public int size ()
	{
		return fRules.size ();
	}

	public void process ( MessageProcessingContext context )
	{
		final MetricsCatalog mc = context.getMetrics ();

		int ruleIndex = 0;
		for ( Rule r : fRules )
		{
			try ( PathPopper pp = mc.push ( "rule-" + ruleIndex++ ) )
			{
				try ( Timer.Context ruleDurCtx = mc.timer ( "total" ).time () )
				{
					final List<Processor> procs;
					final String procChainLabel;
		
					final Filter f = r.getFilter ();
					if ( f == null || f.passes ( context ) )
					{
						procs = r.getThenProcs ();
						procChainLabel = "filterPass";
					}
					else
					{
						procs = r.getElseProcs ();
						procChainLabel = "filterFail";
					}

					try ( PathPopper pp2 = mc.push ( procChainLabel ) )
					{
						int procIndex = 0;
						for ( Processor p : procs )
						{
							final String procName = makeMetricsName ( p, procIndex++ );
							try ( PathPopper pp3 = mc.push ( procName ) )
							{
								try ( Timer.Context procDurCtx = mc.timer ( "totalTime" ).time () )
								{
									p.process ( context );
									if ( !context.shouldContinue () )
									{
										// break from the processor loop
										break;
									}
								}
							}
						}
					}
		
					// break from the rule loop
					if ( !context.shouldContinue () )
					{
						break;
					}
				}
			}
		}
	}

	private static String makeMetricsName ( Processor p, int i )
	{
		final String clazz = p.getClass ().getSimpleName ().replaceAll ( "/", "-" );
		return "proc-" + i + " (" + clazz + ")";
	}

	private final ArrayList<Rule> fRules;
}
