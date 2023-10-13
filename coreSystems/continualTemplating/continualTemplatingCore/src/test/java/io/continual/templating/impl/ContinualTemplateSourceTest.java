package io.continual.templating.impl;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import io.continual.templating.ContinualTemplateSource;
import io.continual.util.data.StreamTools;
import junit.framework.TestCase;

public class ContinualTemplateSourceTest extends TestCase
{
	@Test
	public void testCombinedStreams () throws Exception
	{
		try ( final ContinualTemplateSource src = ContinualTemplateSource.combinedStreams (
			ContinualTemplateSource.fromString ( "abc" ),
			ContinualTemplateSource.fromString ( "def" )
		) )
		{
			final String s = new String ( StreamTools.readBytes ( src.getTemplate () ), StandardCharsets.UTF_8 );
	
			assertEquals ( "abcdef", s );
			assertEquals ( "abc + def", src.getName () );
		}
	}

	@Test
	public void testEmptyCombinedStreams () throws Exception
	{
		try ( final ContinualTemplateSource src = ContinualTemplateSource.combinedStreams () )
		{
			final String s = new String ( StreamTools.readBytes ( src.getTemplate () ), StandardCharsets.UTF_8 );
			assertEquals ( "", s );
			assertEquals ( "", src.getName () );
		}
	}
}
