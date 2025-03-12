package io.continual.notify;

import io.continual.util.time.Clock;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

/**
 * An interface for tracking alerts.
 */
public interface ContinualAlertTracker
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
     * Onset an alert for the given subject and condition at the current time.
     * @param subject the alert's subject
     * @param condition the alert's condition
     * @return an alert
     */
    default ContinualAlertAgent.Alert onset (String subject, String condition )
    {
        return onset ( subject, condition, Clock.now () );
    }

    /**
     * Onset an alert for the given subject and condition at the current time with an exception as additional data.
     * @param subject the alert's subject
     * @param condition the alert's condition
     * @param x an throwable to include in the alert
     * @return an alert
     */
    default ContinualAlertAgent.Alert onset (String subject, String condition, Throwable x )
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
    default ContinualAlertAgent.Alert onset (String subject, String condition, long atMs )
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
    default ContinualAlertAgent.Alert onset (String subject, String condition, JSONObject addlData )
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
    ContinualAlertAgent.Alert onset (String subject, String condition, long atMs, JSONObject addlData );

    /**
     * Get an existing alert for the given subject and condition.
     * @param subject the alert's subject
     * @param condition the alert's condition
     * @return an alert, or null if none exists
     */
    ContinualAlertAgent.Alert get (String subject, String condition );

    /**
     * Clear an alert for the given subject and condition, if it exists.
     * @param subject the alert's subject
     * @param condition the alert's condition
     * @return the cleared alert, or null if none exists
     */
    ContinualAlertAgent.Alert clear (String subject, String condition );

    /**
     * Get a collection of standing alerts
     * @return a collection of standing alerts
     */
    Collection<ContinualAlertAgent.Alert> standingAlerts ();
}
