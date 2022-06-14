package io.continual.browserDriver.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserDriver;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.nv.NvReadable;

public class BrowserFormPost extends BrowserValidatingAction
{
	public static BrowserFormPost fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserFormPost ( t, settings );
	}

	public BrowserFormPost ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	@Override
	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		try
		{
			enterData ( ctx.getDriver () );
			postForm ( ctx.getDriver () );
			validate ( ctx.getDriver () );
		}
		catch ( WebDriverException x )
		{
			throw new BrowserActionFailure ( x.getMessage(), x );
		}
		catch ( JSONException e )
		{
			throw new BrowserActionFailure ( e );
		}
	}

	private void postForm ( BrowserDriver browser ) throws JSONException, BrowserActionFailure
	{
		log.info ( "Submitting form." );

		final JSONObject o = getDetails().getJSONObject ( "submit" );
		final WebElement element = browser.findElement ( o, getSettings() );
		element.click ();
	}

	private void enterData ( final BrowserDriver browser ) throws BrowserActionFailure
	{
		try
		{
			log.info ( "Filling out form." );

			final JSONArray data = getDetails().optJSONArray ( "populate" );
			if ( data != null )
			{
				JsonVisitor.forEachElement ( data, new ArrayVisitor<JSONObject,BrowserActionFailure> ()
				{
					public boolean visit ( JSONObject t ) throws JSONException, BrowserActionFailure
					{
						final String val = browser.eval ( "value", t, getSettings() );

						final WebElement element = browser.findElement ( t, getSettings() );

						element.click ();
						element.sendKeys ( val );

						return true;
					}
				} );
			}
		}
		catch ( WebDriverException x )
		{
			log.warn ( "WebDriverException: " + x.getMessage (), x );
			throw new BrowserActionFailure ( x.getMessage(), x );
		}
		catch ( JSONException x )
		{
			log.warn ( "JSONException: " + x.getMessage (), x );
			throw new BrowserActionFailure ( x.getMessage(), x );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( BrowserFormPost.class );
}
