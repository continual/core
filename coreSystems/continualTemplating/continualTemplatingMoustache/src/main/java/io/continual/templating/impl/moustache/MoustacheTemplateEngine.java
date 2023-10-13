package io.continual.templating.impl.moustache;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheResolver;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateCatalog;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.templating.impl.BasicContext;

public class MoustacheTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public MoustacheTemplateEngine ( ServiceContainer sc, JSONObject config )
	{
		fTemplateCatalog = sc.get ( "templateCatalog", ContinualTemplateCatalog.class );
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
			final MustacheResolver mr = new MustacheResolver ()
			{
				@Override
				public Reader getReader ( String resourceName )
				{
					if ( fTemplateCatalog == null )
					{
						log.warn ( "The moustache template resolver needs a 'templateCatalog' registered in the service container." );
						return null;
					}

					try
					{
						final ContinualTemplateSource cts = fTemplateCatalog.getTemplate ( resourceName );
						return new InputStreamReader ( cts.getTemplate () );
					}
					catch ( TemplateNotFoundException e )
					{
						LoggerFactory.getLogger ( getClass() ).warn ( "Couldn't load template {}", resourceName );
						return null;
					}
				}
			};

			final MustacheFactory mf = new DefaultMustacheFactory ( mr );
			final Mustache mustache = mf.compile ( reader, templateSrc.getName () );
			final OutputStreamWriter writer = new OutputStreamWriter ( outTo );
			mustache.execute ( writer, ((BasicContext)context).getAsMap () ).flush ();
		}

		// flush stream
		outTo.flush ();
	}

	private final ContinualTemplateCatalog fTemplateCatalog;

	private static final Logger log = LoggerFactory.getLogger ( MoustacheTemplateEngine.class );
}
