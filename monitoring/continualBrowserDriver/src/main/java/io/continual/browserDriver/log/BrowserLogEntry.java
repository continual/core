package io.continual.browserDriver.log;

import java.util.HashMap;
import java.util.Map;

public class BrowserLogEntry
{
	public BrowserLogEntry ( String name, BrowserLogEntryStatus status, long startAtMs, long endAtMs, long calculatedDurationMs, String errMsg )
	{
		fName = name;
		fStatus = status;
		fStartTimeMs = startAtMs;
		fEndTimeMs = endAtMs;
		fCalcdDurationMs = calculatedDurationMs;
		fErrMsg = errMsg;
		fDetails = new HashMap<String,String> ();
	}

	public BrowserLogEntry addDetail ( String name, String val )
	{
		fDetails.put ( name, val );
		return this;
	}

	@Override
	public String toString ()
	{
		return fName + ": " + fStatus.toString () + ", " + fCalcdDurationMs + " ms. " + (fErrMsg==null?"":fErrMsg);
	}
	
	public final String fName;
	public final BrowserLogEntryStatus fStatus;
	public final long fStartTimeMs;
	public final long fEndTimeMs;
	public final long fCalcdDurationMs;
	public final String fErrMsg;	// can be null
	public final Map<String,String> fDetails;
}
