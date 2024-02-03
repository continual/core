package io.continual.templating.impl.simpleDoubleBrace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.impl.ContinualTemplateSource;
import io.continual.templating.impl.ContinualTemplateSource.TemplateNotFoundException;
import junit.framework.TestCase;

public class SimpleDoubleBraceTemplatingEngineTest extends TestCase
{
	@Test
	public void testTokenizing () throws IOException
	{
		final SimpleDoubleBraceTemplateEngine e = new SimpleDoubleBraceTemplateEngine ( null, null );

		for ( String[] test : kTokenizingTests )
		{
			final ByteArrayInputStream is = new ByteArrayInputStream ( test[0].getBytes ( StandardCharsets.UTF_8 ) );
			for ( int i=1; i<test.length; i++ )
			{
				assertEquals ( test[i], e.readChunk ( is ) );
			}
		}
	}

	@Test
	public void testTemplating () throws IOException, TemplateNotFoundException
	{
		final SimpleDoubleBraceTemplateEngine e = new SimpleDoubleBraceTemplateEngine ( null, null );

		final ContinualTemplateContext ctc = e.createContext ();
		ctc.put ( "a", 123 );
		ctc.put ( "b", "cde" );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		e.renderTemplate ( ContinualTemplateSource.fromString ( "{{a}}b{{b}}" ), ctc, baos );
		final String out = new String ( baos.toByteArray (), StandardCharsets.UTF_8 );
		assertEquals ( "123bcde", out );
	}

	private static final String[][] kTokenizingTests = new String[][]
	{
		new String[] { "foo{{bar }}baz", "foo", "{{", "bar ", "}}", "baz" }, 
		new String[] { "{{b}}", "{{", "b", "}}" }, 
		new String[] { "{{}}", "{{", "}}" }, 
		new String[] { "{{ bad input", "{{", " bad input" }, 
	};
}
