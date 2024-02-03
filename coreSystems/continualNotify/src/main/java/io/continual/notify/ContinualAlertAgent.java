package io.continual.notify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.time.Clock;

/**
 * A simple in-memory alert agent that tracks onsets and clears for a condition against a subject.
 * When there's a state change, a notification is emitted via a Notifier instance.
 */
public class ContinualAlertAgent
{
	public interface Alert
	{
		String subject ();

		String condition ();

		long when ();

		default void clear () { clear ( Clock.now () ); }

		void clear ( long clearTimeMs );
	}

	public ContinualAlertAgent ( ContinualNotifier out )
	{
		fAlertsBySubjectAndCondition = new HashMap<> ();
		fEventsOut = out;
	}

	public Alert onset ( String subject, String condition )
	{
		return this.onset ( subject, condition, Clock.now () );
	}

	public Alert onset ( String subject, String condition, Throwable x )
	{
		final JSONObject addlData = new JSONObject ();
		populateExceptionInto ( addlData, x );
		return onset ( subject, condition, Clock.now (), addlData );
	}

	public Alert onset ( String subject, String condition, long atMs )
	{
		return onset ( subject, condition, atMs, new JSONObject () );
	}

	public Alert onset ( String subject, String condition, JSONObject addlData )
	{
		return onset ( subject, condition, Clock.now (), addlData );
	}

	public static void populateExceptionInto ( JSONObject addlData, Throwable t )
	{
		String stack = "??";
		try (
			final ByteArrayOutputStream baos = new ByteArrayOutputStream (); 
			final PrintStream ps = new PrintStream ( baos )
		)
		{
			t.printStackTrace ( ps );
			ps.close ();
			stack = new String ( baos.toByteArray () );
		}
		catch ( IOException x )
		{
			stack = "?? IOException: " + x.getMessage ();
		}

		addlData
			.put ( "class", t.getClass ().getName () )
			.put ( "message", t.getMessage () )
			.put ( "stack", stack )
		;
	}
	
	public Alert onset ( String subject, String condition, long atMs, JSONObject addlData )
	{
		HashMap<String,Alert> byCondition = fAlertsBySubjectAndCondition.get ( subject );
		if ( byCondition == null )
		{
			byCondition = new HashMap<> ();
			fAlertsBySubjectAndCondition.put ( subject, byCondition );
		}

		Alert alert = byCondition.get ( condition );
		if ( alert != null )
		{
			log.info ( "Alert for {}:{} already exists; ignored.", subject, condition );
		}
		else
		{
			alert = new IntAlert ( subject, condition, atMs, addlData );

			fEventsOut
				.onSubject ( alert.subject() )
				.withCondition ( alert.condition() )
				.withAddlData ( "at", atMs )
				.withAddlData ( "addl", addlData )
				.asOnset ()
				.send ()
			;

			byCondition.put ( condition, alert );

			log.info ( "Raised alert: " + alert.toString () );
		}
		return alert;
	}

	public Alert get ( String subject, String condition )
	{
		final HashMap<String,Alert> byCondition = fAlertsBySubjectAndCondition.get ( subject );
		if ( byCondition == null ) return null;
		return byCondition.get ( condition );
	}

	public Alert clear ( String subject, String condition )
	{
		final Alert a = get ( subject, condition );
		if ( a != null )
		{
			a.clear ();
		}
		return a;
	}

	private class IntAlert implements Alert
	{
		public IntAlert ( String subj, String cond, long when, JSONObject addlData )
		{
			fSubject = subj;
			fCondition = cond;
			fWhen = when;
			fAddlData = addlData == null ? new JSONObject () : addlData;
		}

		@Override
		public String toString ()
		{
			return new JSONObject ()
				.put ( "subject", subject () )
				.put ( "condition", condition () )
				.put ( "when", when () )
				.put ( "addldata", fAddlData )
				.toString ()
			;
		}

		@Override
		public String subject () { return fSubject; }

		@Override
		public String condition () { return fCondition; }

		@Override
		public long when () { return fWhen; }
		
		@Override
		public void clear ( long clearTimeMs )
		{
			final HashMap<String,Alert> byCondition = fAlertsBySubjectAndCondition.get ( subject() );
			final Alert fromMap = byCondition.remove ( condition() );
			if ( fromMap != this ) // the same instance
			{
				log.warn ( "Lookup for clear of {} found a different instance.", this );
			}
			else
			{
				fEventsOut
					.onSubject ( subject() )
					.withCondition ( condition() )
					.withAddlData ( "at", clearTimeMs )
					.asClear ()
					.send ()
				;
			}
		}

		private final String fSubject;
		private final String fCondition;
		private final long fWhen;
		private final JSONObject fAddlData;
	}

	private final HashMap<String,HashMap<String,Alert>> fAlertsBySubjectAndCondition;
	private final ContinualNotifier fEventsOut;

	private static final Logger log = LoggerFactory.getLogger ( ContinualAlertAgent.class );
}
