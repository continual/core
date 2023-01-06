package io.continual.builder.sources;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.continual.util.nv.impl.nvReadableTable;
import junit.framework.TestCase;

public class BuilderReadableDataSourceTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new BuilderReadableDataSource ( null ) );
	}

	@Test
	public void testGetClassNameFromData ()
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( "class" , "BuilderReadableDataSource" );
		assertEquals ( "BuilderReadableDataSource" , 
				new BuilderReadableDataSource ( new nvReadableTable ( keyVal ) ).getClassNameFromData () );
	}
}
