package io.continual.templating;

import java.io.IOException;
import java.io.OutputStream;

import io.continual.services.Service;
import io.continual.templating.impl.ContinualTemplateSource;
import io.continual.templating.impl.ContinualTemplateSource.TemplateNotFoundException;

public interface ContinualTemplateEngine extends Service
{
	/**
	 * Create a template context for this engine.
	 * @return a template context
	 */
	ContinualTemplateContext createContext ();

	/**
	 * Render the named template to the given output stream. The output is flushed but not closed.
	 * 
	 * @param templateSrc
	 * @param context
	 * @param outTo an output stream
	 * @throws TemplateNotFoundException 
	 * @throws IOException 
	 */
	void renderTemplate ( ContinualTemplateSource templateSrc, ContinualTemplateContext context, OutputStream outTo ) throws TemplateNotFoundException, IOException;
}
