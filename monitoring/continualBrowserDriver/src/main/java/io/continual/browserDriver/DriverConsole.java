package io.continual.browserDriver;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;

import io.continual.browserDriver.DriverManager.Product;
import io.continual.browserDriver.log.BrowserLog;
import io.continual.builder.Builder.BuildFailure;
import io.continual.resources.ResourceLoader;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.impl.nvJsonObject;

public class DriverConsole extends ConsoleProgram
{
	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		final JSONObject config = loadConfig ( clp.getFileArgumentsAsString () );
		final WebDriver driver = loadWebDriver ( config );
		try
		{
			final BrowserLog blog = new BrowserLog ();
			final BrowserDriver bdriver = new BrowserDriver ( driver, -1 );
			
			final BrowserRunner runner = BrowserRunner.build ( config, new nvJsonObject ( config ) );
			final JSONObject state = runner.run ( bdriver, blog );
			final JSONObject log = blog.exportToJson ();

			final JSONObject report = new JSONObject ()
				.put ( "state", state )
				.put ( "log", log )
			;
			
			System.out.println ( report.toString ( 4 ) );
		}
		catch ( JSONException | BuildFailure e )
		{
			throw new StartupFailureException ( e );
		}
		finally
		{
			driver.quit ();
		}

		return null;
	}

	public static void main ( String[] args )
	{
		try
		{
			final DriverConsole bd = new DriverConsole ();
			bd.runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			System.err.println ( e.getMessage () );
			e.printStackTrace ( System.err );
			System.exit ( 1 );
		}
    }

	private static JSONObject loadConfig ( String name ) throws StartupFailureException
	{
		if ( name == null )
		{
			throw new StartupFailureException ( "No script resource provided." );
		}

		try
		{
			final InputStream res = ResourceLoader.load ( name );
			if ( res == null )
			{
				throw new StartupFailureException ( "Resource " + name + " not found." );
			}
			return new JSONObject ( new CommentedJsonTokener ( res ) );
		}
		catch ( JSONException | IOException e )
		{
			throw new StartupFailureException ( e );
		}
	}

	private WebDriver loadWebDriver ( JSONObject config )
	{
		Product p = Product.CHROME;

		final JSONObject driverOptions = config.optJSONObject ( "driverOptions" );
		if ( driverOptions != null )
		{
			p = Product.fromConfig ( driverOptions.optString ( "browser", Product.CHROME.toString () ) );
		}
 		return new DriverManager ().getDriver ( p );
	}
}
