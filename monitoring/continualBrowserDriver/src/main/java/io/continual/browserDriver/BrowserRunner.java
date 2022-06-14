package io.continual.browserDriver;

import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.browserDriver.BrowserAction.BrowserActionFailure;
import io.continual.browserDriver.BrowserAction.BrowserTimeoutFailure;
import io.continual.browserDriver.actions.BrowserGet;
import io.continual.browserDriver.log.BrowserLog;
import io.continual.browserDriver.log.BrowserLogActionData;
import io.continual.browserDriver.log.BrowserLogEntryStatus;
import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.nv.NvReadable;

public class BrowserRunner
{
	public static BrowserRunner build ( InputStream script, final NvReadable settings ) throws JSONException, BuildFailure
	{
		final JSONObject top = new JSONObject ( new CommentedJsonTokener ( script ) );
		return build ( top, settings );
	}

	public static BrowserRunner build ( JSONObject script, final NvReadable settings ) throws JSONException, BuildFailure
	{
		final BrowserRunner result = new BrowserRunner ( settings );

		JsonVisitor.forEachElement ( script.optJSONArray ( "steps" ), new ArrayVisitor<JSONObject,Builder.BuildFailure> ()
		{
			@Override
			public boolean visit ( JSONObject step ) throws Builder.BuildFailure
			{
				final BrowserAction ba = Builder
					.withBaseClass ( BrowserAction.class )
					.searchingPath ( BrowserGet.class.getPackage ().getName () )
					.withClassNameInData ()
					.usingData ( step )
					.providingContext ( settings )
					.build ()
				;
				result.addAction ( ba );
				return true;
			}
		} );

		return result;
	}

	public JSONObject run ( final BrowserDriver driver, final BrowserLog blog )
	{
		final JSONObject state = new JSONObject ();

		final ActionContext ctx = new ActionContext () {

			@Override
			public BrowserDriver getDriver () { return driver; }

			@Override
			public BrowserLog getLog () { return blog; }

			@Override
			public JSONObject getState () { return state; }
		};

		run ( ctx );

		return state;
	}

	public void run ( final ActionContext ctx )
	{
		log.info ( "Running browser actions..." );
		for ( BrowserAction ba : fSteps )
		{
			final BrowserLog blog = ctx.getLog ();

			final String name = ba.getName ();
			final BrowserLogActionData action = blog.startAction ( name );

			BrowserLogEntryStatus status = BrowserLogEntryStatus.OKAY;
			String errMsg = null;

			try
			{
				runAction ( ba, ctx );
				status = BrowserLogEntryStatus.OKAY;
			}
			catch ( BrowserTimeoutFailure x )
			{
				status = BrowserLogEntryStatus.TIMEOUT;
				errMsg = x.getMessage ();
				log.warn ( x.getMessage() );
			}
			catch ( BrowserActionFailure x )
			{
				status = BrowserLogEntryStatus.FAIL;
				errMsg = x.getMessage ();
				log.warn ( x.getMessage() );
			}
			catch ( JSONException x )
			{
				status = BrowserLogEntryStatus.FAIL;
				errMsg = x.getMessage ();
				log.warn ( x.getMessage() );
			}

			action.markComplete ( status, ba.getDifferentialMs (), errMsg );
			blog.stopAction ();

			if ( status != BrowserLogEntryStatus.OKAY )
			{
				break;
			}
		}
	}

	protected void runAction ( BrowserAction ba, ActionContext ctx ) throws JSONException, BrowserActionFailure
	{
		final long timeoutMs = fSettings.getLong ( kSetting_TimeoutMs, 30*1000 );

		runAction ( ba, ctx, timeoutMs );

		final int thenWaitMs = ba.getPauseMs ();
		if ( thenWaitMs > 0 )
		{
			log.info ( "\twaiting " + thenWaitMs + " ms." );
			try
			{
				Thread.sleep ( thenWaitMs );
			}
			catch ( InterruptedException e )
			{
				// ignore
			}
		}
	}

	private void runAction ( final BrowserAction ba, final ActionContext ctx, long timeoutMs ) throws BrowserActionFailure
	{
		log.info ( "Running [" + ba.toString () + "], up to " + timeoutMs + " ms." );

		final Backgrounder b = new Backgrounder ( ba, ctx );
		final Thread t = new Thread ( b );
		t.start ();
	
		try
		{
			t.join ( timeoutMs );
		}
		catch ( InterruptedException e )
		{
			// nothing to do here
		}

		log.info ( "Action thread joined." );
		if ( t.isAlive () )
		{
			log.warn ( "Action timeout: " + ba.toString () );
			t.interrupt ();
			ctx.getDriver ().close ();

			throw new BrowserTimeoutFailure ( "Terminated " + ba.toString() + " after " + timeoutMs + " ms." );
		}
	
		final BrowserActionFailure f = b.getThrown ();
		if ( f != null )
		{
			throw f;
		}
	
		log.info ( "\treturned from " + ba.toString () );
	}

	protected BrowserRunner addAction ( BrowserAction ba )
	{
		fSteps.add ( ba );
		return this;
	}

	private BrowserRunner ( NvReadable settings )
	{
		fSteps = new ArrayList<BrowserAction> ();
		fSettings = settings;
	}

	private static final String kSetting_TimeoutMs = "actionTimeoutMs";

	private final NvReadable fSettings;
	private final ArrayList<BrowserAction> fSteps;

	private static final Logger log = LoggerFactory.getLogger ( BrowserRunner.class );

	public static class Backgrounder implements Runnable
	{
		public Backgrounder ( BrowserAction ba, ActionContext ctx )
		{
			fAction = ba;
			fContext = ctx;
			fThrown = null;
		}
	
		@Override
		public void run ()
		{
			log.info ( "Starting action \"" + fAction.getName () + "\"." );
			try
			{
				fAction.act ( fContext );
			}
			catch ( BrowserActionFailure e )
			{
				synchronized ( this )
				{
					fThrown = e;
				}
			}
			log.info ( "Finished action \"" + fAction.getName () + "\"." );
		}
	
		public synchronized BrowserActionFailure getThrown ()
		{
			return fThrown;
		}
	
		private final BrowserAction fAction;
		private final ActionContext fContext;
		private BrowserActionFailure fThrown;
	}
}
