package io.continual.browserDriver;

/**
 * An action taken in the browser
 * @author peter
 */
public interface BrowserAction
{
	public class BrowserActionFailure extends Exception
	{
		public BrowserActionFailure ( String msg ) { super(msg); }
		public BrowserActionFailure ( String msg, Throwable t ) { super(msg,t); }
		public BrowserActionFailure ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}

	public class BrowserTimeoutFailure extends BrowserActionFailure
	{
		public BrowserTimeoutFailure ( String msg ) { super(msg); }
		public BrowserTimeoutFailure ( String msg, Throwable t ) { super(msg,t); }
		public BrowserTimeoutFailure ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * get this action's name
	 * @return a name
	 */
	String getName ();

	/**
	 * get the differential time for this action
	 * @return the number of ms to add(or subtract) from the timing result
	 */
	long getDifferentialMs ();

	/**
	 * Get the number of ms to pause between this step and the next
	 * @return the time to pause
	 */
	int getPauseMs ();

	/**
	 * Act on browser
	 * @param actionContext the action context
	 * @throws BrowserActionFailure if the action fails
	 */
	void act ( ActionContext actionContext ) throws BrowserActionFailure;
}
