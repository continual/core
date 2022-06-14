package io.continual.browserDriver.actions;

import org.json.JSONObject;

import io.continual.browserDriver.BrowserAction;
import io.continual.util.nv.NvReadable;

public abstract class StdBrowserAction implements BrowserAction
{
	public StdBrowserAction ( JSONObject details, NvReadable settings )
	{
		fDetails = details;
		fSettings = settings;
	}

	@Override
	public String toString ()
	{
		return fDetails.optString ( "name", "(unnamed)" );
	}

	@Override
	public String getName ()
	{
		return fDetails.optString ( "name", "(unnamed)" );
	}

	@Override
	public long getDifferentialMs ()
	{
		return fDetails.optLong ( kSetting_Differential, 0 );
	}

	@Override
	public int getPauseMs ()
	{
		return fDetails.optInt ( "thenPause", 0 );
	}

	protected JSONObject getDetails ()
	{
		return fDetails;
	}
	
	protected NvReadable getSettings ()
	{
		return fSettings;
	}

	private final JSONObject fDetails;
	private final NvReadable fSettings;

	private static final String kSetting_Differential = "timeAdjustmentMs";
}
