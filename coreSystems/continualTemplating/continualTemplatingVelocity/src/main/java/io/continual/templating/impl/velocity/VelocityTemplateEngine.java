package io.continual.templating.impl.velocity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.TreeSet;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.impl.ContinualTemplateSource;
import io.continual.templating.impl.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class VelocityTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public VelocityTemplateEngine ( ServiceContainer sc, JSONObject config )
	{
		fEngine = new VelocityEngine ();
		fEngine.init ();

		fBaseContext = new VelocityContext ();

		final JSONObject contextData = config == null ? null : config.optJSONObject ( "context" );
		JsonVisitor.forEachElement ( contextData, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String key, Object val ) throws JSONException
			{
				fBaseContext.put ( key, val );
				return true;
			}
		} );
	}

	@Override
	public ContinualTemplateContext createContext ()
	{
		return new ContextWrapper ( fBaseContext );
	}

	@Override
	public void renderTemplate ( ContinualTemplateSource templateSrc, ContinualTemplateContext context, OutputStream outTo ) throws TemplateNotFoundException, IOException
	{
		if ( ! ( context instanceof ContextWrapper ) )
		{
			throw new IllegalStateException ( "Context was not created by this engine." );
		}
		final ContextWrapper cw = (ContextWrapper) context;

		// don't assume we'll close the output stream
		final OutputStreamWriter writer = new OutputStreamWriter ( outTo );

		try ( InputStreamReader reader = new InputStreamReader ( templateSrc.getTemplate () ) )
		{
			fEngine.evaluate ( cw.getVelocityContext (), writer, templateSrc.getName (), reader );
		}
		
		// flush stream
		writer.flush ();
	}

	private final VelocityEngine fEngine;
	private final VelocityContext fBaseContext;

	private static class ContextWrapper implements ContinualTemplateContext 
	{
		public ContextWrapper ( VelocityContext baseContext )
		{
			fActualCtx = new VelocityContext ( baseContext );
		}

		@Override
		public Object get ( String key ) { return fActualCtx.get ( key ); }

		@Override
		public Set<String> keys ()
		{
			final TreeSet<String> result = new TreeSet<> ();
			for ( String key : fActualCtx.getKeys () )
			{
				result.add ( key );
			}
			return result;
		}

		@Override
		public ContinualTemplateContext put ( String key, Object o )
		{
			fActualCtx.put ( key, o );
			return this;
		}

		@Override
		public ContinualTemplateContext remove ( String key )
		{
			fActualCtx.remove ( key );
			return this;
		}

		public VelocityContext getVelocityContext ()
		{
			return fActualCtx;
		}
		
		private final VelocityContext fActualCtx; 
	};

}
