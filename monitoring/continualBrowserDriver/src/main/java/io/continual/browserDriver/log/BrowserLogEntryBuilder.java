package io.continual.browserDriver.log;

import java.util.HashMap;
import java.util.Map;

public class BrowserLogEntryBuilder
{
	public BrowserLogEntryBuilder named ( String label )
	{
		fLabel = label;
		return this;
	}

	public BrowserLogEntryBuilder startingAt ( long epochMs )
	{
		fStartMs = epochMs;
		fEndMs = fStartMs + fDurationMs;
		return this;
	}

	public BrowserLogEntryBuilder endingAt ( long epochMs )
	{
		fEndMs = epochMs;
		fDurationMs = fEndMs - fStartMs;
		return this;
	}

	public BrowserLogEntryBuilder withDuration ( long ms )
	{
		fEndMs = fStartMs + ms;
		fDurationMs = ms;
		return this;
	}

	public BrowserLogEntryBuilder withStatus ( BrowserLogEntryStatus status )
	{
		fStatus = status;
		return this;
	}

	public BrowserLogEntryBuilder withErrorMsg ( String msg )
	{
		fErrMsg = msg;
		return this;
	}

	public BrowserLogEntryBuilder withDetails ( String name, String value )
	{
		fDetails.put ( name, value );
		return this;
	}

	public void logTo ( BrowserLogActionData ad )
	{
		final BrowserLogEntry e = new BrowserLogEntry ( fLabel, fStatus, fStartMs, fEndMs, fDurationMs, fErrMsg );
		for ( Map.Entry<String,String> ee : fDetails.entrySet () )
		{
			e.addDetail ( ee.getKey(), ee.getValue () );
		}
		ad.add ( e );
	}

	private String fLabel = "??";
	private BrowserLogEntryStatus fStatus = BrowserLogEntryStatus.NOT_PROVIDED;
	private String fErrMsg = null;
	private long fStartMs = 0;
	private long fEndMs = 0;
	private long fDurationMs = 0;
	private HashMap<String,String> fDetails = new HashMap<String,String> ();
}
