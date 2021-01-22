package io.continual.services.processor.engine.library.processors.StringUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class Split implements Processor
{
	public Split ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fFrom = config.getString ( "from" );

		fTo = new ArrayList<> ();
		JsonVisitor.forEachElement ( config.getJSONArray ( "to" ), new ArrayVisitor<String,JSONException> ()
		{
			@Override
			public boolean visit ( String to ) throws JSONException
			{
				fTo.add ( to );
				return true;
			}
		} );

		fRegex = config.getString ( "regex" );
		fPattern = Pattern.compile ( fRegex );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String val = context.evalExpression ( fFrom );
		final Matcher m = fPattern.matcher ( val );
		if ( m.matches () )
		{
			final int groups = Math.min ( m.groupCount (), fTo.size () );
			for ( int i=0; i<groups; i++ )
			{
				final String matchVal = m.group ( i+1 );
				final String key = fTo.get ( i );
				context.getMessage ().putValue ( key, matchVal );
			}
		}
	}

	private final String fFrom;
	private final String fRegex;
	private final Pattern fPattern;
	private ArrayList<String> fTo;
}
