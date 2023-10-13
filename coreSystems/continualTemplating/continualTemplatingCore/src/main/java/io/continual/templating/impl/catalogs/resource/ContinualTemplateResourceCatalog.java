package io.continual.templating.impl.catalogs.resource;

import org.json.JSONObject;

import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateCatalog;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;

public class ContinualTemplateResourceCatalog extends SimpleService implements ContinualTemplateCatalog
{
	public static final String kSetting_BasePkg = "resourceBasePkg";
	public static final String kDefault_BasePkg = "";

	public ContinualTemplateResourceCatalog ( ServiceContainer sc, JSONObject config )
	{
		fBasePkg = sc.getExprEval ().evaluateText ( config.optString ( kSetting_BasePkg, kDefault_BasePkg ) );
	}

	@Override
	public ContinualTemplateSource getTemplate ( String... names ) throws TemplateNotFoundException
	{
		final ContinualTemplateSource[] srcs = new ContinualTemplateSource [ names.length ];
		for ( int i=0; i<names.length; i++ )
		{
			String res = names[i];
			if ( fBasePkg.length () > 0 )
			{
				res = fBasePkg + "/" + names[i];
			}
			srcs[i] = ContinualTemplateSource.fromResource ( res, names[i] );
		}
		return ContinualTemplateSource.combinedStreams ( srcs );
	}

	private final String fBasePkg;
}
