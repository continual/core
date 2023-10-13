package io.continual.templating.impl.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateCatalog;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class VelocityTemplateEngine extends SimpleService implements ContinualTemplateEngine
{
	public VelocityTemplateEngine ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fTemplateCatalog = sc.get ( config.optString ( "templateCatalog", "" ), ContinualTemplateCatalog.class );

		final Properties props = new Properties ();
		if ( fTemplateCatalog != null )
		{
			props.setProperty ( RuntimeConstants.RESOURCE_LOADER, "catalog" );
			props.setProperty ( "resource.loader.catalog.class", CatalogLoader.class.getName () );
			props.setProperty ( "resource.loader.catalog.cache", new Boolean ( config.optBoolean ( "catalogCache", true ) ).toString () );
		}

		fEngine = new VelocityEngine ( props );

		if ( fTemplateCatalog != null )
		{
			fEngine.setProperty ( "resource.loader.catalog.instance", new CatalogLoader () );
		}

		fEngine.init ();

		fBaseContext = new VelocityContext ();

		// enable relative template finding
		{
			final EventCartridge ec = new EventCartridge ();
			ec.addEventHandler ( new IncludeRelativePath () );
			ec.attachToContext ( fBaseContext );
		}

		// compatibility for some older drumlin-based systems
		fBaseContext.put ( "servletRoot", "" );
		fBaseContext.put ( "warRoot", "" );

		// addl context from setup
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
	private final ContinualTemplateCatalog fTemplateCatalog;

	private static class ContextWrapper implements ContinualTemplateContext 
	{
		public ContextWrapper ( VelocityContext baseContext )
		{
			fActualCtx = new VelocityContext ( baseContext );
		}

		@Override
		public Object get ( String key ) { return fActualCtx.get ( key ); }

		@Override
		public ContinualTemplateContext put ( String key, Object o )
		{
			fActualCtx.put ( key, o );
			return this;
		}

		@Override
		public ContinualTemplateContext putAll ( Map<String,?> data )
		{
			for ( Map.Entry<String,?> e : data.entrySet () )
			{
				put ( e.getKey (), e.getValue () );
			}
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

	public class CatalogLoader extends ResourceLoader
	{
		@Override
		public void init ( ExtProperties configuration )
		{
		}

		@Override
		public Reader getResourceReader ( String source, String encoding ) throws ResourceNotFoundException
		{
			try
			{
				final InputStream is = fTemplateCatalog.getTemplate ( source ).getTemplate ();
				if ( is != null )
				{
					return new InputStreamReader ( is );
				}
			}
			catch ( TemplateNotFoundException e )
			{
				throw new ResourceNotFoundException ( e );
			}
			throw new ResourceNotFoundException ( source );
		}

		@Override
		public boolean isSourceModified ( Resource resource )
		{
			return false;
		}

		@Override
		public long getLastModified ( Resource resource )
		{
			return 0;
		}
	}
}
