package io.continual.services.processor.engine.library.processors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class AppendArray implements Processor
{
	public static final String kSetting_Target = "target";
	public static final String kSetting_Value = "value";

	public AppendArray ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fTargetArray = config.getString ( kSetting_Target );
		fValue = config.get ( kSetting_Value );
	}
	
	@Override
	public void process ( MessageProcessingContext context )
	{
		Object val = null;
		if ( fValue instanceof String )
		{
			val = context.evalExpression ( (String) fValue );
		}
		else if ( fValue instanceof JSONObject )
		{
			final JSONObject target = new JSONObject ();
			JsonVisitor.forEachElement ( (JSONObject) fValue, new ObjectVisitor<Object,JSONException> ()
			{
				@Override
				public boolean visit ( String templateKey, Object templateValue ) throws JSONException
				{
					// FIXME: improve this for alternate types like objects
					target.put ( templateKey, context.evalExpression ( templateValue.toString () ) );
					return true;
				}
			} );
			val = target;
		}
		else
		{
			context.warn ( "AppendArray does not know how to handle type " + fValue.getClass ().getSimpleName () + ". Ignored." );
			return;
		}

		JSONArray array = context.getMessage ().accessRawJson ().optJSONArray ( fTargetArray );
		if ( array == null )
		{
			array = new JSONArray ();
			context.getMessage ().accessRawJson ().put ( fTargetArray, array );
		}
		array.put ( val );
	}

	private final String fTargetArray;
	private final Object fValue;
}
