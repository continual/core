package io.continual.notify;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple in-memory alert agent that tracks onsets and clears for a condition against a subject.
 * When there's a state change, a notification is emitted via a Notifier instance.
 */
public class ContinualAlertAgent implements ContinualAlertTracker
{
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
	 * Onset an alert for the given subject and condition at the given time with additional JSON data.
	 * @param subject the alert's subject
	 * @param condition the alert's condition
	 * @param atMs the time at which the alert is raised
	 * @param addlData additional data for the alert
	 * @return an alert
	 */
	@Override
	public Alert onset ( String subject, String condition, long atMs, JSONObject addlData )
	{
		final HashMap<String,Alert> byCondition = fAlertsBySubjectAndCondition.computeIfAbsent (
			subject, k -> new HashMap<> ()
		);

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
	@Override
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
	@Override
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
	@Override
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
