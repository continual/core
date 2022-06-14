package io.continual.browserDriver.actions;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.nv.NvReadable;

public class BrowserCollect extends StdBrowserAction implements BrowserAction
{
	public static BrowserCollect fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserCollect ( t, settings );
	}

	public BrowserCollect ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );

		fLabel = t.getString ( "label" );
		fAsText = t.optBoolean ( "text", false );
	}

	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		try
		{
			final List<WebElement> elements = ctx.getDriver().findElements ( getDetails(), getSettings() );
			if ( fAsText )
			{
				final StringBuilder sb = new StringBuilder ();
				for ( WebElement element : elements )
				{
					sb.append ( element.getText () );
					sb.append ( System.lineSeparator () );
				}
				ctx.getState ().put ( fLabel, sb.toString () );
			}
			else if ( elements.size () == 0 )
			{
				ctx.getState ().put ( fLabel, "" );
			}
			else if ( elements.size () == 1 )
			{
				ctx.getState ().put ( fLabel, elementToJson ( elements.iterator ().next () ) );
			}
			else
			{
				final JSONArray array = new JSONArray ();
				for ( WebElement element : elements )
				{
					array.put ( elementToJson ( element ) );
				}
				ctx.getState ().put ( fLabel, array );
			}
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

	private JSONObject elementToJson ( WebElement element )
	{
		final JSONObject result = new JSONObject ();
		final String html = element.getAttribute ( "outerHTML" );

		result
			.put ( "id", element.getAttribute ( "id" ) )
			.put ( "class", element.getAttribute ( "class" ) )
			.put ( "text", element.getText () )
			.put ( "tag", element.getTagName () )
			.put ( "html", html )
		;

		final JSONArray children = new JSONArray ();
		for ( WebElement e : element.findElements ( By.xpath ( ".//*" ) ) )
		{
			final JSONObject child = elementToJson ( e );
			children.put ( child );
		}
		result.put ( "children", children );

		return result;
	}

	private final String fLabel;
	private final boolean fAsText;
}
