package io.continual.browserDriver;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;

import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class DriverTesting extends ConsoleProgram
{
	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		try
		{
			final String kProxy = "localhost:8080";
			//final String kProfileName = "/home/headless/.mozilla/firefox/0kvic88y.default-release";
			final String kProfileName = "/Users/peter/Library/Application Support/Firefox/Profiles/p56h4g9a.default-release";
			
			// basic setup with proxy
			final FirefoxOptions browserOptions = new FirefoxOptions ();
			browserOptions.setProxy ( new Proxy ()
				.setHttpProxy ( kProxy )
				.setSslProxy ( kProxy )
			);
			browserOptions.setProfile ( new FirefoxProfile ( new File ( kProfileName ) ) );

			final RemoteWebDriver driver = new RemoteWebDriver ( new URL ( "http://localhost:4444" ), browserOptions );

			// go to a page and take a screen shot
			driver.get ( "https://www.continual.io" );

			final File screenshot = driver.getScreenshotAs ( OutputType.FILE );
			screenshot.deleteOnExit ();	// this happens by spec anyway

			driver.quit ();
		}
		catch ( MalformedURLException e )
		{
			throw new StartupFailureException ( e );
		}

		return null;
	}

	public static void main ( String[] args )
	{
		try
		{
			final DriverTesting bd = new DriverTesting ();
			bd.runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			System.err.println ( e.getMessage () );
			e.printStackTrace ( System.err );
			System.exit ( 1 );
		}
    }
}

