package io.continual.browserDriver;

import org.json.JSONObject;

import io.continual.browserDriver.log.BrowserLog;

public interface ActionContext
{
	BrowserDriver getDriver ();

	BrowserLog getLog ();

	JSONObject getState ();
}
