package io.continual.browserDriver.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.browserDriver.BrowserDriver;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.nv.NvReadable;

public abstract class BrowserValidatingAction extends StdBrowserAction
{
	public BrowserValidatingAction ( JSONObject t, NvReadable settings )
	{
		super ( t, settings );
	}

	protected void validate ( final BrowserDriver browser ) throws JSONException, BrowserActionFailure
	{
		final JSONArray steps = getDetails().optJSONArray ( "responseValidator" );
		if ( steps == null )
		{
			log.info ( "No validation steps." );
			return;
		}

		JsonVisitor.forEachElement ( steps, new JsonVisitor.ArrayVisitor<JSONObject,BrowserActionFailure>()
		{
			public boolean visit ( JSONObject t ) throws JSONException, BrowserActionFailure
			{
				final String verb = t.getString ( "action" );
				final long waitMs = t.optLong ( "waitMs", 500L );
				final long timeoutAtMs = System.currentTimeMillis () + waitMs;

				log.info ( "Validating with " + verb + " for up to " + waitMs + " ms." );
				
				if ( verb.equalsIgnoreCase ( "locate" ) )
				{
					boolean satisfied = false;
					boolean triedOnce = false;
					while ( !satisfied && ( !triedOnce || System.currentTimeMillis () < timeoutAtMs ) )
					{
						try
						{
							triedOnce = true;
							browser.findElement ( t, getSettings() );
							satisfied = true;
						}
						catch ( BrowserActionFailure x )
						{
							// not found, loop around
						}
						if ( !satisfied ) validationSleep ();
					}

					if ( !satisfied )
					{
						throw new BrowserActionFailure ( "Did not complete " + t.toString () + "." );
					}
				}
				else if ( verb.equalsIgnoreCase ( "title" ) )
				{
					final String title = browser.getTitle ( browser );
					final String expect = t.optString ( "text", "" );

					boolean satisfied = false;
					boolean triedOnce = false;

					while ( !satisfied && ( !triedOnce || System.currentTimeMillis () < timeoutAtMs ) )
					{
						triedOnce = true;
						satisfied = ( title.equals ( expect ) );
						if ( !satisfied ) validationSleep ();
					}
					if ( !satisfied )
					{
						throw new BrowserActionFailure ( "Expected page title [" + expect + "] but found [" + title + "]." );
					}
				}
				else
				{
					throw new BrowserActionFailure ( "Unknown action " + verb + "." );
				}

				return true;
			}
		} );
	}

	private void validationSleep () throws BrowserActionFailure
	{
		try
		{
			Thread.sleep ( 100 );
		}
		catch ( InterruptedException e )
		{
			throw new BrowserActionFailure ( e );
		}
	}
	
	private static final Logger log = LoggerFactory.getLogger ( BrowserValidatingAction.class ); 
}
