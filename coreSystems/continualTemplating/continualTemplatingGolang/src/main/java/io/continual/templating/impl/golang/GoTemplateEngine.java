package io.continual.templating.impl.golang;

import java.io.IOException;
import java.io.OutputStream;

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
import io.continual.util.data.TypeConvertor;
import ru.proninyaroslav.template.FuncMap;
import ru.proninyaroslav.template.Template;
import ru.proninyaroslav.template.exceptions.ExecException;
import ru.proninyaroslav.template.exceptions.InternalException;
import ru.proninyaroslav.template.exceptions.ParseException;

public class GoTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	private static final String kSetting_TemplateCacheSize = "cacheSize";
	private static final long kDefault_TemplateCacheSize = 512;
	
	public GoTemplateEngine ( ServiceContainer sc, JSONObject config )
	{
		fTemplateCache = new LruCache<String,Template> ( sc.getExprEval ().evaluateTextToLong ( config.opt ( kSetting_TemplateCacheSize ), kDefault_TemplateCacheSize ) );
	}

	@Override
	public ContinualTemplateContext createContext ()
	{
		return new BasicContext ();
	}

	@Override
	public void renderTemplate ( ContinualTemplateSource templateSrc, ContinualTemplateContext context, OutputStream outTo ) throws TemplateNotFoundException, IOException, TemplateParseException
	{
		if ( ! ( context instanceof BasicContext ) )
		{
			throw new IllegalStateException ( "Context was not created by this engine." );
		}

		try
		{
			final Template goTemplate = getTemplate ( templateSrc );
			goTemplate.execute ( outTo, context );
	
			// flush stream
			outTo.flush ();
		}
		catch ( ExecException x )
		{
			log.warn ( x.getMessage () );
			throw new IOException ( x );
		}
	}

	private Template getTemplate ( ContinualTemplateSource templateSrc ) throws TemplateNotFoundException, IOException, TemplateParseException
	{
		final String key = templateSrc.getName ();
		Template result = fTemplateCache.get ( key );
		if ( result == null )
		{
			try
			{
				result = new Template ( templateSrc.getName () );
				result.addFuncs ( kFnMap );
				result.parse ( templateSrc.getTemplate () );
				fTemplateCache.put ( key, result );
			}
			catch ( ParseException | InternalException x )
			{
				log.warn ( x.getMessage () );
				throw new TemplateParseException ( x );
			}
		}
		return result;
	}

	private final LruCache<String,Template> fTemplateCache; 

	private static final FuncMap kFnMap = new FuncMap ();
	static
	{
		kFnMap.put ( "default", "fnDefault", GoTemplateEngine.class );
		kFnMap.put ( "quote", "fnQuote", GoTemplateEngine.class );
	}

	public static Object fnDefault ( Object... args )
	{
		if ( args.length < 1 || args.length > 2 )
		{
			throw new IllegalArgumentException ( "'default' takes two arguments (including last value)" );
		}
		
		final Object defval = args[0];
		final Object curval = args.length == 2 ? args[1] : null;
		
		if ( curval == null ) return defval;
		if ( curval instanceof Number && ((Number)curval).doubleValue () == 0.0 ) return defval;
		if ( curval instanceof String && ((String)curval).length () == 0 ) return defval;
		if ( curval instanceof Boolean && ( !(Boolean)curval) ) return defval;

//		Lists: []
//		Dicts: {}
		
		return curval;
	}
	
	public static String fnQuote ( Object... args )
	{
		if ( args.length != 1 ) return null;

		final Object val = args[0];
		final String sval = val == null ? "" : val.toString ();
		final boolean isQuoted = sval.startsWith ( "\"" ) && sval.endsWith ( "\"" );
		if ( !isQuoted )
		{
			return "\"" + TypeConvertor.encode ( sval, '\\', new char[] { '"' }, new char[] { '"' } ) + "\"";
		}
		else
		{
			return sval;
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( GoTemplateEngine.class );
}
