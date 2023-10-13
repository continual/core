package io.continual.templating.impl.simpleDoubleBrace;

import java.io.IOException;
import java.io.InputStream;
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

public class SimpleDoubleBraceTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public SimpleDoubleBraceTemplateEngine ( ServiceContainer sc, JSONObject config )
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
		final InputStream is = templateSrc.getTemplate ();

		String chunk;
		StringBuilder token = null;
		while ( null != ( chunk = readChunk ( is ) ) )
		{
			if ( token == null && chunk.equals ( "{{" ) )
			{
				token = new StringBuilder ();
			}
			else if ( token != null )
			{
				if ( chunk.equals ( "}}" ) )
				{
					final String key = token.toString ().trim ();
					token = null;
	
					Object val = context.get ( key );
					if ( val == null ) val = "";
	
					outTo.write ( val.toString ().getBytes ( StandardCharsets.UTF_8 ) );
				}
				else
				{
					token.append ( chunk );
				}
			}
			else
			{
				outTo.write ( chunk.getBytes ( StandardCharsets.UTF_8 ) );
			}
		}

		// could be a malformed ending text...
		if ( token != null )
		{
			outTo.write ( "{{".getBytes ( StandardCharsets.UTF_8 ) );
			outTo.write ( token.toString ().getBytes ( StandardCharsets.UTF_8 ) );
		}

		// flush stream
		outTo.flush ();
	}

	private static final int kMaxReadLen = 32;
	String readChunk ( InputStream is ) throws IOException
	{
		final StringBuilder sb = new StringBuilder ();

		while ( sb.length () < kMaxReadLen )
		{
			is.mark ( 2 );
			final int c = is.read ();
			if ( c == '{' )
			{
				if ( sb.length () == 0 )
				{
					sb.append ( "{" );
				}
				else if ( sb.length () == 1 && sb.charAt ( 0 ) == '{' )
				{
					return "{{";
				}
				else
				{
					is.reset ();
					return sb.toString ();
				}
			}
			else if ( c == '}' )
			{
				if ( sb.length () == 0 )
				{
					sb.append ( "}" );
				}
				else if ( sb.length () == 1 && sb.charAt ( 0 ) == '}' )
				{
					return "}}";
				}
				else
				{
					is.reset ();
					return sb.toString ();
				}
			}
			else if ( c == -1 )
			{
				return sb.length () > 0 ? sb.toString () : null; 
			}
			else
			{
				sb.append ( (char) c );
			}
		}
		
		return sb.toString ();
	}
}
