package io.continual.services.processor.library.jsoup.processors;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class HtmlXPath  implements Processor
{
	public HtmlXPath ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fHtmlExpr = config.getString ( "htmlExpr" );
			fUpdates = new LinkedList<> ();
			JsonVisitor.forEachElement ( config.getJSONArray ( "updates" ), new ArrayVisitor<JSONObject,BuildFailure> ()
			{
				@Override
				public boolean visit ( JSONObject updateInfo ) throws JSONException, BuildFailure
				{
					fUpdates.add ( new Updater ( updateInfo ) );
					return true;
				}
			} );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String html = context.evalExpression ( fHtmlExpr );
		if ( html == null || html.isEmpty () )
		{
			context.warn ( fHtmlExpr + " evaluated to an empty string. XPath processing skipped." );
			return;
		}

		final Document doc = Jsoup.parse ( html );
		for ( Updater u : fUpdates )
		{
			u.update ( context, doc );
		}
	}

	private final String fHtmlExpr;
	private final LinkedList<Updater> fUpdates;

	private class Updater
	{
		public Updater ( JSONObject updateInfo ) throws BuildFailure
		{
			fPathExpr = updateInfo.getString ( "xpath" );
			fTargetArrayExpr = updateInfo.optString ( "targetArray", null );
			fTargetFieldExpr = updateInfo.optString ( "targetField", null );
			fTextFormat = updateInfo.optString ( "format", "html" );

			if ( fTargetArrayExpr == null && fTargetFieldExpr == null )
			{
				throw new BuildFailure ( "Either targetArray or targetField must be specified in an XPath updater." );
			}
		}

		public void update ( MessageProcessingContext context, Document doc )
		{
			final String path = context.evalExpression ( fPathExpr );
			if ( path == null || path.isEmpty () )
			{
				context.warn ( fPathExpr + " evaluated to an empty string. XPath processing skipped." );
				return;
			}

			if ( fTargetArrayExpr != null )
			{
				final String targetArray = context.evalExpression ( fTargetArrayExpr );
				if ( targetArray == null || targetArray.isEmpty () )
				{
					context.warn ( fTargetArrayExpr + " evaluated to an empty string. XPath update skipped." );
					return;
				}

				final Elements elements = doc.selectXpath ( path );
				for ( Element e : elements )
				{
					context.getMessage ().appendRawValue ( targetArray, new JSONObject ()
						.put ( "html", e.outerHtml () )
						.put ( "text", e.text () )
					);
				}
			}
			else if ( fTargetFieldExpr != null )
			{
				final String targetField = context.evalExpression ( fTargetFieldExpr );
				if ( targetField == null || targetField.isEmpty () )
				{
					context.warn ( fTargetFieldExpr + " evaluated to an empty string. XPath update skipped." );
					return;
				}

				final Elements elements = doc.selectXpath ( path );
				if ( elements.isEmpty () )
				{
					context.warn ( "XPath expression " + path + " returned nothing." );
					return;
				}

				if ( elements.size () > 1 )
				{
					context.warn ( "XPath expression " + path + " returned multiple elements. Only the first one will be used." );
				}
				final Element el = elements.first ();

				Object val = null;
				if ( fTextFormat.equals ( "html" ) )
				{
					val = el.outerHtml ();
				}
				else if ( fTextFormat.equals ( "text" ) )
				{
					val = el.text ();
                }
				else if ( fTextFormat.equals ( "object" ) )
				{
					final JSONObject attrs = new JSONObject ();
					for ( Attribute attr : el.attributes () )
					{
						attrs.put ( attr.getKey (), attr.getValue () );
					}

					val = new JSONObject ()
						.put ( "id", el.id () )
						.put ( "attrs", attrs )
						.put ( "text",  el.text () )
						.put ( "html", el.html () )
					;
                }
                else
                {
                    context.warn ( "Unknown text format: " + fTextFormat + ". Using 'html'." );
                    val = el.outerHtml ();
				}

				context.getMessage ().putRawValue ( targetField, val );
			}
		}

		private final String fPathExpr;
		private final String fTargetArrayExpr;
		private final String fTargetFieldExpr;
		private final String fTextFormat;
	}
}
