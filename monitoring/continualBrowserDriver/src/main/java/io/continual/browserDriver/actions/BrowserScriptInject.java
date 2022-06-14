package io.continual.browserDriver.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriverException;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.browserDriver.BrowserDriver;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.nv.NvReadable;

public class BrowserScriptInject extends BrowserValidatingAction implements BrowserAction
{
	public static BrowserScriptInject fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserScriptInject ( t, settings );
	}

	public BrowserScriptInject ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	@Override
	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		final String script = ctx.getDriver().eval ( "script", getDetails(), getSettings() );
		if ( script != null )
		{
			try
			{
				final BrowserDriver wd = ctx.getDriver ();
				if ( wd instanceof JavascriptExecutor )
				{
					final JavascriptExecutor je = (JavascriptExecutor) wd;
					je.executeScript ( script );	// no args
				}
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
