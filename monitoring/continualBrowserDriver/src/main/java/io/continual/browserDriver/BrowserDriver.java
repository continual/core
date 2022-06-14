package io.continual.browserDriver;

import java.io.Closeable;
import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.browserDriver.BrowserAction.BrowserActionFailure;
import io.continual.util.nv.NvReadable;

public class BrowserDriver implements Closeable
{
	public BrowserDriver ( WebDriver driver, int proxyPort )
	{
		fDriver = driver;
		fProxyPort = proxyPort;
	}

	@Override
	public void close ()
	{
		if ( fDriver != null ) 
		{
			try
			{
				log.info ( "Quitting browser..." );
				fDriver.quit ();
			}
			catch ( UnreachableBrowserException x )
			{
				log.warn ( "Can't reach browser. " + x.getMessage () );
			}
		}
	}

	public WebDriver getWebDriver () 
	{
		return fDriver;
	}
	
	public String eval ( String key, JSONObject obj, NvReadable settings )
	{
		String result = obj.optString ( key, null );
		if ( result != null )
		{
			int openBrace = -1;
			while ( ( openBrace = result.indexOf ( "${" ) ) >= 0 )
			{
				int closeBrace = result.indexOf ( "}", openBrace+2 );
				if ( closeBrace < 0 )
				{
					break;	// malformed. just return it.
				}
				final String settingsKey = result.substring ( openBrace + 2, closeBrace );
				final String val = settings.getString ( settingsKey, "" );
				
				final StringBuffer replacement = new StringBuffer ();
				if ( openBrace > 0 )
				{
					replacement.append ( result.substring ( 0, openBrace ) );
				}
				replacement.append ( val );
				if ( closeBrace < result.length () - 1 )
				{
					replacement.append ( result.substring ( closeBrace + 1 ) );
				}
				result = replacement.toString ();
			}
		}
		return result;
	}

	public String getTitle ( BrowserDriver browser )
	{
		return fDriver.getTitle ();
	}

	public WebElement findElement ( JSONObject config, NvReadable settings ) throws JSONException, BrowserActionFailure
	{
		try
		{
			final List<WebElement> elements = findElements ( config, settings );
			if ( elements == null || elements.size() == 0 )
			{
				throw new BrowserActionFailure ( "No elements found." );
			}
			for ( WebElement element : elements )
			{
				if ( element.isDisplayed () )
				{
					return element;
				}
			}
			// here, none are displayed. return the first.
			return elements.get ( 0 );
		}
		catch ( NoSuchElementException x )
		{
			throw new BrowserActionFailure ( "No elements found.", x );
		}
	}

	public List<WebElement> findElements ( JSONObject config, NvReadable settings ) throws JSONException, BrowserActionFailure
	{
		// try to find by id, name, link text, or xpath
		By by = null;
		String val = null;
		if ( ( val = eval ( "id", config, settings ) ) != null )
		{
			by = By.id ( val );
		}
		else if ( ( val = eval ( "link", config, settings ) ) != null )
		{
			by = By.linkText ( val );
		}
		else if ( ( val = eval ( "partial-link", config, settings ) ) != null )
		{
			by = By.partialLinkText ( val );
		}
		else if ( ( val = eval ( "css", config, settings ) ) != null )
		{
			by = By.cssSelector ( val );
		}
		else if ( ( val = eval ( "xpath", config, settings ) ) != null )
		{
			by = By.xpath ( val );
		}
		else if ( ( val = eval ( "name", config, settings ) ) != null )
		{
			// 'name' is last because it conflicts with the step name!
			by = By.name ( val );
		}

		if ( by == null )
		{
			throw new JSONException ( "Didn't find id, name, link, or xpath locator in data entry." );
		}

		log.info ( "Locating element(s) via [{}]...", by.toString () );
		final List<WebElement> elements = fDriver.findElements ( by );
		return elements;
	}

	private final WebDriver fDriver;
	private final int fProxyPort;

	private static final Logger log = LoggerFactory.getLogger ( BrowserDriver.class ); 
}
