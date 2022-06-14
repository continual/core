package io.continual.browserDriver;

import java.util.HashMap;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

class DriverManager
{
	public enum Product
	{
		CHROME,
		SAFARI,
		EDGE,
		FIREFOX;

		public static Product fromConfig ( String val )
		{
			if ( val == null ) return null;

			val = val.trim ().toUpperCase ();
			return Product.valueOf ( val );
		}
	}

	public interface DriverSetupStep
	{
		WebDriverManager runSetup ( WebDriverManager wdm );
	}

	public WebDriver getDriver ( Product p, DriverSetupStep... driverSetupSteps )
	{
		setup ( p, driverSetupSteps );
		switch ( p )
		{
			case CHROME:
				return new ChromeDriver ();

			case EDGE:
				return new EdgeDriver ();

			case FIREFOX:
				return new FirefoxDriver ();

			case SAFARI:
				return new SafariDriver ();

			default:
				throw new IllegalArgumentException ( "Unrecognized browser product " + p.toString () );
		}
    }

	private void setup ( Product p, DriverSetupStep... driverSetupSteps )
	{
		final Boolean isSetup = fSetups.get ( p );
		if ( isSetup == null || !isSetup )
		{
			WebDriverManager wdm;
			switch ( p )
			{
				case CHROME:
					wdm = WebDriverManager.chromedriver ();
					break;
	
				case EDGE:
					wdm = WebDriverManager.edgedriver ();
					break;

				case FIREFOX:
					wdm = WebDriverManager.firefoxdriver ();
					break;

				case SAFARI:
					wdm = WebDriverManager.safaridriver ();
					break;

				default:
					throw new IllegalArgumentException ( "Unrecognized browser product " + p.toString () );
			}
			for ( DriverSetupStep dss : driverSetupSteps )
			{
				wdm = dss.runSetup ( wdm );
			}
			wdm.setup ();

			fSetups.put ( p, true );
		}
	}

	private final HashMap<Product,Boolean> fSetups = new HashMap<> ();
}


/*

// setup a proxy if configured to do so
if ( fProxyPort > 0 && TypeConvertor.convertToBoolean ( System.getProperty ( "setSeleniumProxy", "false" ) ) )
{
	final String proxyConnection = "localhost:" + fProxyPort;
	final Proxy proxy = new Proxy ()
		.setProxyType ( ProxyType.MANUAL )
		.setHttpProxy ( proxyConnection )
		.setFtpProxy ( proxyConnection )
		.setSslProxy ( proxyConnection )
		.setSocksProxy ( proxyConnection )
	;
	capabilities.setCapability ( CapabilityType.PROXY, proxy );
*/
