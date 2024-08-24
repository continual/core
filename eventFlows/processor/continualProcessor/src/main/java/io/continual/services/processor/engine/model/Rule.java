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

import io.continual.services.processor.engine.library.filters.Any;

public class Rule
{
	public static Builder newRule ()
	{
		return new Builder ();
	}

	public static class Builder
	{
		/**
		 * Set a filter on the rule
		 * @param f a filter
		 * @return this builder
		 */
		public Builder checkIf ( Filter f )
		{
			fFilter = f;
			return this;
		}

		/**
		 * Add a processor to execute always. This replaces the rule filter with an Any filter.
		 * @param p
		 * @return this builder
		 */
		public Builder alwaysDo ( Processor p )
		{
			return
				checkIf ( new Any () )
				.thenDo ( p )
			;
		}

		/**
		 * Add a processor to execute when the filter matches
		 * @param p
		 * @return this builder
		 */
		public Builder thenDo ( Processor p )
		{
			fActiveChain = fThens;
			return and ( p );
		}

		/**
		 * Add a processor to execute when the filter matches
		 * @param pp
		 * @return this builder
		 */
		public Builder thenDo ( List<? extends Processor> pp )
		{
			fActiveChain = fThens;
			for ( Processor p : pp )
			{
				and ( p );
			}
			return this;
		}

		/**
		 * Add a processor to execute when the filter doesn't match
		 * @param p
		 * @return this builder
		 */
		public Builder elseDo ( Processor p )
		{
			fActiveChain = fElses;
			return and ( p );
		}

		/**
		 * Add a processor to execute when the filter doesn't match
		 * @param pp
		 * @return this builder
		 */
		public Builder elseDo ( List<? extends Processor> pp )
		{
			fActiveChain = fElses;
			for ( Processor p : pp )
			{
				and ( p );
			}
			return this;
		}

		/**
		 * Add a processor to an existing then or else chain
		 * @param p
		 * @return this builder
		 */
		public Builder and ( Processor p )
		{
			if ( fActiveChain == null ) { throw new IllegalStateException ( "Can't add a processor without thenDo() or elseDo()" ); }

			fActiveChain.add ( p );
			return this;
		}

		/**
		 * Build the rule
		 * @return a rule
		 */
		public Rule build ()
		{
			return new Rule ( this );
		}

		private Filter fFilter = new Any ();
		private ArrayList<Processor> fThens = new ArrayList<> ();
		private ArrayList<Processor> fElses = new ArrayList<> ();
		private ArrayList<Processor> fActiveChain = null;
	}

	/**
	 * Get the filter for this rule.
	 * @return a filter
	 */
	public Filter getFilter ()
	{
		return fFilter;
	}

	public List<Processor> getThenProcs ()
	{
		return fThens;
	}

	public List<Processor> getElseProcs ()
	{
		return fElses;
	}

	private final Filter fFilter;
	private final ArrayList<Processor> fThens;
	private final ArrayList<Processor> fElses;

	private Rule ( Builder b )
	{
		fFilter = b.fFilter;
		fThens = new ArrayList<> ();
		fThens.addAll ( b.fThens );
		fElses = new ArrayList<> ();
		fElses.addAll ( b.fElses );
	}
}
