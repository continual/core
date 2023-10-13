package io.continual.templating.impl.velocity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.templating.ContinualTemplateContext;
import io.continual.templating.ContinualTemplateSource;
import io.continual.templating.ContinualTemplateSource.TemplateNotFoundException;
import junit.framework.TestCase;

public class VelocityTemplateEngineTest extends TestCase
{
	@Test
	public void testTemplating () throws IOException, TemplateNotFoundException, BuildFailure
	{
		final VelocityTemplateEngine e = new VelocityTemplateEngine ( new ServiceContainer (), new JSONObject () );

		final ContinualTemplateContext ctc = e.createContext ();
		ctc.put ( "a", 123 );
		ctc.put ( "b", "cde" );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		e.renderTemplate ( ContinualTemplateSource.fromString ( "$a b $b" ), ctc, baos );
		baos.close ();
		final String out = new String ( baos.toByteArray (), StandardCharsets.UTF_8 );
		assertEquals ( "123 b cde", out );
	}

	@Test
	public void testTemplatingWithLoops () throws IOException, TemplateNotFoundException, BuildFailure
	{
		final VelocityTemplateEngine e = new VelocityTemplateEngine ( new ServiceContainer (), new JSONObject () );

		final ContinualTemplateContext ctc = e.createContext ();
		ctc.put ( "b", new Foo[] {
			new Foo ( "Fred" ),
			new Foo ( "Jack" ),
			new Foo ( "Bobby" )
		} );

		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		e.renderTemplate ( ContinualTemplateSource.fromString ( "#foreach($bb in $b)<b>$bb.name</b>#end" ), ctc, baos );
		final String out = new String ( baos.toByteArray (), StandardCharsets.UTF_8 );
		assertEquals ( "<b>Fred</b><b>Jack</b><b>Bobby</b>", out );
	}

	public static class Foo
	{
		public Foo ( String name ) { fName = name; }
		public String getName () { return fName; }
		private final String fName;
	}
}
