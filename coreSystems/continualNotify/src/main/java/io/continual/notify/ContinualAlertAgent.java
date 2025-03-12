package io.continual.notify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

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
	/**
	 * An alert, which is a condition that's raised on a specific subject at a specific time.
	 */
	public interface Alert
	{
		/**
		 * Get the subject of the alert.
		 * @return the subject
		 */
		String subject ();

		/**
		 * Get the condition of the alert.
		 * @return the condition
		 */
		String condition ();

		/**
		 * Get the timestamp of the alert's onset.
		 * @return the onset time
		 */
		long when ();

		/**
		 * Clear the alert at the current time.
		 */
		default void clear () { clear ( Clock.now () ); }

		/**
		 * Clear the alert at the given time.
		 * @param clearTimeMs the time at which the alert is cleared
		 */
		void clear ( long clearTimeMs );
	}

	/**
	 * Construct an alert agent that emits notifications to a default notifier. This is equivalent to
	 * "ContinualAlertAgent ( new ContinualNotifier () )"
	 */
	public ContinualAlertAgent ( )
	{
		this ( new ContinualNotifier () );
	}

	/**
	 * Construct an alert agent that emits notifications via the given notifier.
	 * @param out the notification emitter
	 */
	public ContinualAlertAgent ( ContinualNotifier out )
	{
		fAlertsBySubjectAndCondition = new HashMap<> ();
		fEventsOut = out;
	}

	/**
	 * Onset an alert for the given subject and condition at the current time.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @return an alert
	 */
	public Alert onset ( String subject, String condition )
	{
		return this.onset ( subject, condition, Clock.now () );
	}

	/**
	 * Onset an alert for the given subject and condition at the current time with an exception as additional data.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @param x an throwable to include in the alert
	 * @return an alert
	 */
	public Alert onset ( String subject, String condition, Throwable x )
	{
		final JSONObject addlData = new JSONObject ();
		populateExceptionInto ( addlData, x );
		return onset ( subject, condition, Clock.now (), addlData );
	}

	/**
	 * Onset an alert for the given subject and condition at the given time.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @param atMs the time at which the alert is raised
	 * @return an alert
	 */
	public Alert onset ( String subject, String condition, long atMs )
	{
		return onset ( subject, condition, atMs, new JSONObject () );
	}

	/**
	 * Onset an alert for the given subject and condition at the current time with additional JSON data.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @param addlData additional data for the alert
	 * @return an alert
	 */
	public Alert onset ( String subject, String condition, JSONObject addlData )
	{
		return onset ( subject, condition, Clock.now (), addlData );
	}

	/**
	 * Translate an exception into a JSON object that can be used for additional alert data.
	 * @param addlData the target object to populate
	 * @param t the throwable to translate into additional data
	 */
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
			stack = baos.toString ();
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

	/**
	 * Onset an alert for the given subject and condition at the given time with additional JSON data.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @param atMs the time at which the alert is raised
	 * @param addlData additional data for the alert
	 * @return an alert
	 */
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

			log.info ( "Raised alert: {}", alert );
		}
		return alert;
	}

	/**
	 * Get an existing alert for the given subject and condition.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @return an alert, or null if none exists
	 */
	public Alert get ( String subject, String condition )
	{
		final HashMap<String,Alert> byCondition = fAlertsBySubjectAndCondition.get ( subject );
		if ( byCondition == null ) return null;
		return byCondition.get ( condition );
	}

	/**
	 * Clear an alert for the given subject and condition, if it exists.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @return the cleared alert, or null if none exists
	 */
	public Alert clear ( String subject, String condition )
	{
		final Alert a = get ( subject, condition );
		if ( a != null )
		{
			a.clear ();
		}
		return a;
	}

	/**
	 * Get a collection of standing alerts
	 * @return a collection of standing alerts
	 */
	public Collection<Alert> standingAlerts ()
	{
		final LinkedList<Alert> result = new LinkedList<> ();
		for ( Entry<String, HashMap<String, Alert>> e : fAlertsBySubjectAndCondition.entrySet () )
		{
			result.addAll ( e.getValue().values() );
		}
		return result;
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
				log.info ( "Cleared alert: {}", this );
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
