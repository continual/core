package io.continual.browserDriver.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.nv.NvReadable;

public class BrowserHover extends StdBrowserAction implements BrowserAction
{
	public static BrowserHover fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserHover ( t, settings );
	}

	public BrowserHover ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	@Override
	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		try
		{
			final WebElement element = ctx.getDriver().findElement ( getDetails(), getSettings() );
			new Actions( ctx.getDriver ().getWebDriver () ).moveToElement( element ).perform ();
		}
		catch ( WebDriverException x )
		{
			throw new BrowserActionFailure ( x );
		}
		catch ( JSONException e )
		{
			throw new BrowserActionFailure ( e );
		}
	}
}
