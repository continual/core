package io.continual.templating.impl.dollarEval;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.impl.ContinualTemplateSource;
import io.continual.templating.impl.ContinualTemplateSource.TemplateNotFoundException;
import junit.framework.TestCase;

public class DollarEvalTemplatingEngineTest extends TestCase
{
	@Test
	public void testTokenizing () throws IOException, TemplateNotFoundException
	{
		final DollarEvalTemplateEngine e = new DollarEvalTemplateEngine ( null, null );

		final ContinualTemplateContext ctc = e.createContext ();
		ctc.put ( "a", 123 );
		ctc.put ( "b", "cde" );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		e.renderTemplate ( ContinualTemplateSource.fromString ( "${a}b${b}" ), ctc, baos );
		final String out = new String ( baos.toByteArray (), StandardCharsets.UTF_8 );
		assertEquals ( "123bcde", out );
	}
}
