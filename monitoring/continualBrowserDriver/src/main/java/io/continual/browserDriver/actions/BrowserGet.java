package io.continual.browserDriver.actions;

import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.util.nv.NvReadable;

public class BrowserGet extends BrowserValidatingAction implements BrowserAction
{
	public static BrowserGet fromJson ( JSONObject t, NvReadable settings )
	{
		return new BrowserGet ( t, settings );
	}

	public BrowserGet ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	@Override
	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		final String url = ctx.getDriver().eval ( "url", getDetails(), getSettings() );
		if ( url != null )
		{
			try
			{
				ctx.getDriver().getWebDriver ().get ( url );
				validate ( ctx.getDriver () );
			}
			catch ( WebDriverException x )
			{
				throw new BrowserActionFailure ( x.getMessage(), x );
			}
		}
		else
		{
			throw new BrowserActionFailure ( "No URL provided to Get" );
		}
	}
}
