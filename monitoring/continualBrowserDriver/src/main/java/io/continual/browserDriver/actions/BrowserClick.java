package io.continual.browserDriver.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.nv.NvReadable;

public class BrowserClick extends BrowserValidatingAction implements BrowserAction
{
	public static BrowserClick fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserClick ( t, settings );
	}

	public BrowserClick ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		try
		{
			final WebElement element = ctx.getDriver().findElement ( getDetails(), getSettings() );
			element.click ();
			validate ( ctx.getDriver () );
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
