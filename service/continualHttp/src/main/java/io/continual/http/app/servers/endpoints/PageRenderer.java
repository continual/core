package io.continual.http.app.servers.endpoints;

import java.io.IOException;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.sessions.CHttpUserSession;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.templating.ContinualTemplateCatalog;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateEngine;
import io.continual.templating.ContinualTemplateEngine.TemplateParseException;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import io.continual.util.standards.MimeTypes;

public class PageRenderer<I extends Identity> extends TypicalUiEndpoint<I>
{
	public PageRenderer ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fTemplates = sc.getReqd ( "templateEngine", ContinualTemplateEngine.class );
		fTemplateCatalog = sc.getReqd ( "templateCatalog", ContinualTemplateCatalog.class );

		fPrefix = config.optString ( "prefix", null );
		fRequiredSuffix = config.optString ( "reqdSuffix", null );
	}

	public void getIndex ( CHttpRequestContext context ) throws TemplateNotFoundException, IOException, TemplateParseException
	{
		getPage ( context, "index.html" );
	}

	public void getPage ( CHttpRequestContext context, String path ) throws TemplateNotFoundException, IOException, TemplateParseException
	{
		final ContinualTemplateContext templateCtx = fTemplates.createContext ();
		CHttpUserSession.getSession ( context ).populateTemplateContext ( templateCtx );
		updateContext ( context, templateCtx, path );

		if ( fPrefix != null )
		{
			path = fPrefix + path;
		}
		if ( fRequiredSuffix != null && !path.endsWith ( fRequiredSuffix ) )
		{
			path = path + fRequiredSuffix;
		}
		
		final ContinualTemplateSource template = fTemplateCatalog.getTemplate ( path );
		fTemplates.renderTemplate ( template, templateCtx, context.response ().getStreamForBinaryResponse ( MimeTypes.kHtml ) );
	}

	protected void updateContext ( CHttpRequestContext context, ContinualTemplateContext templateCtx, String path )
	{
	}

	private final ContinualTemplateEngine fTemplates;
	private final ContinualTemplateCatalog fTemplateCatalog;

	private final String fPrefix;
	private final String fRequiredSuffix;
}
