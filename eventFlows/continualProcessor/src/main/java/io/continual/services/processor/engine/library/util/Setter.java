package io.continual.services.processor.engine.library.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

/**
 * Utility for setting values into a message in order to keep behavior consistent across library
 * and extension processors.
 */
public class Setter
{
	public static void evaluateAndSet ( MessageProcessingContext context, String key, Object val, boolean appendArray )
	{
		final Message msg = context.getMessage ();
		JsonEval.setValue ( msg.accessRawJson(), key, evaluate ( context, val, msg ), appendArray );
	}

	public static JSONObject evaluate ( MessageProcessingContext mpc, JSONObject value, Message msg, final ExprDataSource... addlSrcs )
	{
		final JSONObject replacement = new JSONObject ();
		JsonVisitor.forEachElement ( value, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String origKey, Object val ) throws JSONException
			{
				final String keyToUse = evaluateToString ( mpc, origKey, msg, addlSrcs );
				replacement.put ( keyToUse, evaluate ( mpc, val, msg, addlSrcs ) );
				return true;
			}
		} );
		return replacement;
	}

	public static JSONArray evaluate ( MessageProcessingContext mpc, JSONArray value, Message msg, final ExprDataSource... addlSrcs )
	{
		final JSONArray replacement = new JSONArray ();
		JsonVisitor.forEachElement ( value, new ArrayVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( Object val ) throws JSONException
			{
				replacement.put ( evaluate ( mpc, val, msg, addlSrcs ) );
				return true;
			}
		} );
		return replacement;
	}

	public static Object evaluate ( MessageProcessingContext mpc, Object t, Message msg, final ExprDataSource... addlSrcs )
	{
		if ( t instanceof JSONObject )
		{
			return evaluate ( mpc, (JSONObject) t, msg, addlSrcs );
		}
		else if ( t instanceof JSONArray )
		{
			return evaluate ( mpc, (JSONArray) t, msg, addlSrcs );
		}
		else if ( t instanceof String )
		{
			final String key = t.toString ();
			if ( key.startsWith ( "json:${" ) )
			{
				final String val = mpc.evalExpression ( key.substring ( 5 ), addlSrcs );
				if ( val == null ) return new JSONObject ();
				if ( val.startsWith ( "{" ) )
				{
					try
					{
						return new JSONObject ( new CommentedJsonTokener ( val ) );
					}
					catch ( JSONException x )
					{
						return val;
					}
				}
				else if ( val.startsWith ( "[" ) )
				{
					try
					{
						return new JSONArray ( new CommentedJsonTokener ( val ) );
					}
					catch ( JSONException x )
					{
						return val;
					}
				}
				else
				{
					return val;
				}
			}
			else
			{
				final String val = mpc.evalExpression ( key, addlSrcs );
				return val == null ? "" : val;
			}
		}
		return t;
	}

	public static String evaluateToString ( MessageProcessingContext mpc, Object t, Message msg, final ExprDataSource... addlSrcs )
	{
		Object o = evaluate ( mpc, t, msg, addlSrcs );
		return o == null ? "" : o.toString ();
	}
}
