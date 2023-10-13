package io.continual.templating.impl.dollarEval;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.templating.impl.BasicContext;
import io.continual.util.data.StreamTools;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class DollarEvalTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public DollarEvalTemplateEngine ( ServiceContainer sc, JSONObject config )
	{
	}

	@Override
	public ContinualTemplateContext createContext ()
	{
		return new BasicContext ();
	}

	@Override
	public void renderTemplate ( ContinualTemplateSource templateSrc, ContinualTemplateContext context, OutputStream outTo ) throws TemplateNotFoundException, IOException
	{
		final String src = new String ( StreamTools.readBytes ( templateSrc.getTemplate () ), StandardCharsets.UTF_8 );
		final String out = ExpressionEvaluator.evaluateText ( src, new ExprDataSource ()
		{
			@Override
			public Object eval ( String label ) { return context.get ( label ); }
		} );
		outTo.write ( out.getBytes ( StandardCharsets.UTF_8 ) );
	}
}
