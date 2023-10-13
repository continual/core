package io.continual.templating;

import io.continual.services.Service;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;

/**
 * A catalog of named templates
 */
public interface ContinualTemplateCatalog extends Service
{
	/**
	 * Get one or more named templates from this catalog. If more than one name is given, the
	 * templates are concatenated in the given sequence.
	 * @param names
	 * @return a template source
	 */
	ContinualTemplateSource getTemplate ( String... names ) throws TemplateNotFoundException;
}
