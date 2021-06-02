package io.continual.onap.services.mrCommon;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 * A host selector intended for concurrent access.
 */
public class HostSelector
{
	public static class Builder
	{
		public HostSelector build ()
		{
			return new HostSelector ( this );
		}

		public Builder withHost ( String host )
		{
			fHostSet.add ( host );
			return this;
		}

		public Builder withHosts ( List<String> hosts )
		{
			fHostSet.addAll ( hosts );
			return this;
		}

		public Builder useInGivenOrder ()
		{
			fRandomOrder = false;
			return this;
		}

		public Builder useInRandomOrder ()
		{
			fRandomOrder = true;
			return this;
		}

		private final TreeSet<String> fHostSet = new TreeSet<> ();
		public boolean fRandomOrder = true;
	}

	public static Builder builder () { return new Builder (); }
	
	/**
	 * Clear the given list and copy the host list into it.
	 * @param hosts
	 */
	public void copyInto ( List<String> hosts )
	{
		hosts.clear ();
		hosts.addAll ( fHosts );
	}

	public synchronized String selectHost ()
	{
		return fHosts.getFirst ();
	}

	public synchronized void demote ( String host )
	{
		fHosts.remove ( host );
		fHosts.addLast ( host );
	}

	private HostSelector ( Builder b )
	{
		fHosts = new LinkedList<> ();
		fHosts.addAll ( b.fHostSet );

		// choose a random order to start so that multiple instances of the same client
		// system don't all hit a single host in a cluster
		if ( b.fRandomOrder  )
		{
			Collections.shuffle ( fHosts );
		}
	}

	private final LinkedList<String> fHosts;
}
