package io.continual.flowcontrol.model;

import org.junit.Test;

import io.continual.flowcontrol.impl.common.JsonJobVersion;
import junit.framework.TestCase;

public class FlowControlJobVersionTest extends TestCase
{
	@Test
	public void testNonSemVer ()
	{
		final JsonJobVersion v1 = new JsonJobVersion ( "foobar" ); 
		final JsonJobVersion v2 = new JsonJobVersion ( "gonzo" );
		assertTrue ( v1.compareTo ( v2 ) < 0 );
	}

	@Test
	public void testSameMajorIncMinor ()
	{
		final JsonJobVersion v1 = new JsonJobVersion ( "1.0.0" ); 
		final JsonJobVersion v2 = new JsonJobVersion ( "1.11.0-SNAPSHOT" );
		assertTrue ( v1.compareTo ( v2 ) < 0 );
	}
}
