package io.continual.templating.impl.golang;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.templating.impl.BasicContext;
import io.continual.util.collections.LruCache;
import io.github.verils.gotemplate.GoTemplate;
import io.github.verils.gotemplate.GoTemplateFactory;
import io.github.verils.gotemplate.TemplateExecutionException;
import io.github.verils.gotemplate.TemplateParseException;

public class GoTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	private static final String kSetting_TemplateCacheSize = "cacheSize";
	private static final long kDefault_TemplateCacheSize = 512;
	
	public GoTemplateEngine ( ServiceContainer sc, JSONObject config )
	{
		fTemplateCache = new LruCache<String,GoTemplate> ( sc.getExprEval ().evaluateTextToLong ( config.opt ( kSetting_TemplateCacheSize ), kDefault_TemplateCacheSize ) );
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

		try
		{
			final GoTemplate goTemplate = getTemplate ( templateSrc );

			final OutputStreamWriter writer = new OutputStreamWriter ( outTo );
			final Map<String,Object> data = ((BasicContext)context).getAsMap ();
			goTemplate.execute ( data, writer );
	
			// flush stream
			writer.flush ();
		}
		catch ( io.github.verils.gotemplate.TemplateNotFoundException x )
		{
			// this really shouldn't happen
			throw new TemplateNotFoundException ( x );
		}
		catch ( TemplateExecutionException x )
		{
			log.warn ( x.getMessage () );
			throw new IOException ( x );
		}
	}

	private GoTemplate getTemplate ( ContinualTemplateSource templateSrc ) throws TemplateNotFoundException, IOException
	{
		final String key = templateSrc.getName ();
		GoTemplate result = fTemplateCache.get ( key );
		if ( result == null )
		{
			try
			{
				final GoTemplateFactory goTemplateFactory = new GoTemplateFactory ();
				goTemplateFactory.parse ( templateSrc.getName (), templateSrc.getTemplate () );
	
				result = goTemplateFactory.getTemplate ( templateSrc.getName () );
	
				fTemplateCache.put ( key, result );
			}
			catch ( io.github.verils.gotemplate.TemplateNotFoundException x )
			{
				// this really shouldn't happen
				throw new TemplateNotFoundException ( x );
			}
			catch ( TemplateParseException x )
			{
				log.warn ( x.getMessage () );
				throw new IOException ( x );
			}
		}
		return result;
	}
	
	private final LruCache<String,GoTemplate> fTemplateCache; 
	private static final Logger log = LoggerFactory.getLogger ( GoTemplateEngine.class );
}
