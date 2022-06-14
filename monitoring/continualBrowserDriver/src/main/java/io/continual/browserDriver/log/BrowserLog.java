package io.continual.browserDriver.log;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.browserDriver.HttpProxy;

/**
 * The browser log acts as a journal of all actions taken.
 */
public class BrowserLog implements Closeable
{
	public BrowserLog ( )
	{
		this ( null );
	}

	public BrowserLog ( HttpProxy proxy )
	{
		fActions = new LinkedList<BrowserLogActionData> ();
		fProxy = proxy;
	}

	public synchronized void close ()
	{
	}

	public synchronized BrowserLogActionData startAction ( String name )
	{
		if ( fProxy != null ) fProxy.startTiming ( name );

		final BrowserLogActionData ad = new BrowserLogActionData ( name );
		fActions.add ( ad );
		return ad;
	}

	public synchronized void stopAction ()
	{
		if ( fActions.size () == 0 )
		{
			log.warn ( "No action in progress; returning throw-away action" );
			return;
		}

		try
		{
			final BrowserLogActionData ad = fActions.getLast ();
			if ( fProxy != null )
			{
				ad.addTimingData ( fProxy.stopTiming () );
			}
		}
		catch ( IOException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized List<BrowserLogActionData> getEntries ()
	{
		final LinkedList<BrowserLogActionData> list = new LinkedList<BrowserLogActionData>(fActions);
		Collections.sort ( list, new Comparator<BrowserLogActionData> () {
			@Override
			public int compare ( BrowserLogActionData o1, BrowserLogActionData o2 )
			{
				return Long.compare ( o1.fStartMs, o2.fStartMs );
			}
		} );
		return list;
	}

	public synchronized JSONObject exportToJson ()
	{
		final JSONObject result = new JSONObject ();

		final JSONArray actions = new JSONArray ();
		for ( BrowserLogActionData action : getEntries () )
		{
			final long startMs = action.fStartMs;
			
			final JSONObject actionObj = new JSONObject ();
			actionObj
				.put ( "name", action.fName )
				.put ( "status", action.fStatus.toString () )
				.put ( "startMs", action.fStartMs )
				.put ( "endMs", action.fEndMs )
				.put ( "durationMs", action.fCalcdDurationMs )
				.put ( "errMsg", action.fErrMsg )
				.put ( "timing", action.getTimingData () )
			;

			final JSONArray trxs = new JSONArray ();
			for ( BrowserLogEntry entry : action.getEntries () )
			{
				if ( entry.fStartTimeMs < action.fStartMs )
				{
					log.warn ( "Entry start time is ahead of action start time." );
				}

				final JSONObject e = new JSONObject ()
					.put ( "label", entry.fName )
					.put ( "offsetMs", entry.fStartTimeMs - startMs )
					.put ( "startMs", entry.fStartTimeMs )
					.put ( "endMs", entry.fEndTimeMs )
					.put ( "durationMs", entry.fCalcdDurationMs )
				;
				switch ( entry.fStatus )
				{
					case OKAY:
					case FAIL:
					case TIMEOUT:
						e
							.put ( "status", entry.fStatus.toString ().toUpperCase () )
							.put ( "message", entry.fErrMsg )
						;
						break;

					case NOT_PROVIDED:
					default:
						break;
				}
				for ( Entry<String, String> detail : entry.fDetails.entrySet () )
				{
					e.put ( detail.getKey (), detail.getValue () );
				}
				trxs.put ( e );
			}
			actionObj.put ( "transactions", trxs );

			actions.put ( actionObj );
		}

		result.put ( "actions", actions );
		return result;
	}

	private final HttpProxy fProxy;
	private final LinkedList<BrowserLogActionData> fActions;

	private static final Logger log = LoggerFactory.getLogger ( BrowserLog.class );
}
