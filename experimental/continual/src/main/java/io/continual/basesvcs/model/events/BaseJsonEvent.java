package io.continual.basesvcs.model.events;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class BaseJsonEvent
{
	public JSONObject getEventAsObject ()
	{
		String[] names = JSONObject.getNames ( fTop );
		if ( names == null )
		{
			names = new String[] {};
		}
		return new JSONObject ( fTop, names );
	}
	
	public String getStringForBus ()
	{
		return getEventAsObject().toString();
	}

	/**
	 * Used to convert to a queue-able message
	 */
	@Override
	public String toString ()
	{
		return getStringForBus ();
	}

	/**
	 * Get the partition name to use on the internal bus
	 * @return a partition name string
	 */
	public abstract String getInternalEventStreamName ();
	
	protected BaseJsonEvent ( JSONObject o )
	{
		fTop = o;
	}

	protected JSONObject get ( String label )
	{
		return label == null ? fTop : fTop.getJSONObject ( label );
	}
	
	public String getField ( String id )
	{
		return fTop.getString ( id );
	}

	public long getFieldLong ( String id )
	{
		return fTop.getLong ( id );
	}

	public JSONArray getFieldArray ( String id )
	{
		return fTop.optJSONArray ( id );
	}

	private JSONObject fTop;
}
