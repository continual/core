package io.continual.templating;

import java.io.IOException;
import java.io.OutputStream;

import io.continual.services.Service;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;

public interface ContinualTemplateEngine extends Service
{
	public static class TemplateParseException extends Exception
	{
		public TemplateParseException ( String msg ) { super ( msg ); }
		public TemplateParseException ( Throwable t ) { super ( t ); }
		private static final long serialVersionUID = 1L;
	};

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
	void renderTemplate ( ContinualTemplateSource templateSrc, ContinualTemplateContext context, OutputStream outTo ) throws TemplateNotFoundException, TemplateParseException, IOException;
}
