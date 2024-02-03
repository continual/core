package io.continual.templating.impl.moustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONObject;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.impl.BasicContext;
import io.continual.templating.impl.ContinualTemplateSource;
import io.continual.templating.impl.ContinualTemplateSource.TemplateNotFoundException;

public class MoustacheTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public MoustacheTemplateEngine ( ServiceContainer sc, JSONObject config )
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
		if ( ! ( context instanceof BasicContext ) )
		{
			throw new IllegalStateException ( "Context was not created by this engine." );
		}

		try (
			final InputStreamReader reader = new InputStreamReader ( templateSrc.getTemplate () );
		)
		{
			final MustacheFactory mf = new DefaultMustacheFactory ();
			final Mustache mustache = mf.compile ( reader, "default" );
			final OutputStreamWriter writer = new OutputStreamWriter ( outTo );
			mustache.execute ( writer, ((BasicContext)context).getAsMap () ).flush ();
		}

		// flush stream
		outTo.flush ();
	}
}
