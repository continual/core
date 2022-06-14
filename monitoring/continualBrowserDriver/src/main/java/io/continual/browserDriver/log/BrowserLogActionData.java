package io.continual.browserDriver.log;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.time.Clock;

public class BrowserLogActionData 
{
	public BrowserLogActionData ( String name )
	{
		fName = name;
		fEntries = new LinkedList<BrowserLogEntry> ();
		fStartMs = Clock.now ();
	}

	public void markComplete ( BrowserLogEntryStatus status, long differentialMs, String errMsg )
	{
		fStatus = status;
		fErrMsg = errMsg;

		fEndMs = Clock.now ();
		final long totalMs = fEndMs - fStartMs;
		final long adjustedMs = totalMs + differentialMs;
		fCalcdDurationMs = Math.max ( adjustedMs, 0 );

		log.info ( "[" + fName + "]: clock=" + totalMs + "; diff=" + differentialMs + "; adj=" + adjustedMs + "" );
	}

	public void add ( BrowserLogEntry ble )
	{
		if ( fEndMs > 0 )
		{
			log.warn ( "Adding log entry after action completed." );
		}
		if ( ble.fStartTimeMs < fStartMs )
		{
			log.warn ( "Log entry has start time earlier than action." );
		}
		fEntries.add ( ble );
	}

	public void addTimingData ( JSONObject timingData )
	{
		fTimingData = timingData;
	}

	public List<BrowserLogEntry> getEntries ()
	{
		Collections.sort ( fEntries, new Comparator<BrowserLogEntry> () {

			@Override
			public int compare ( BrowserLogEntry o1, BrowserLogEntry o2 )
			{
				return Long.compare ( o1.fStartTimeMs, o2.fStartTimeMs );
			}
		} );
		return fEntries;
	}

	public BrowserLogEntryStatus getStatus ()
	{
		return fStatus;
	}

	public String getErrMsg ()
	{
		return fErrMsg;
	}

	public JSONObject getTimingData ()
	{
		return fTimingData;
	}

	public final String fName;
	public BrowserLogEntryStatus fStatus;
	public final long fStartMs;
	public long fEndMs = -1;
	public long fCalcdDurationMs = 0;
	public String fErrMsg;
	private final LinkedList<BrowserLogEntry> fEntries;
	private JSONObject fTimingData;

	private static final Logger log = LoggerFactory.getLogger ( BrowserLogActionData.class );
}
