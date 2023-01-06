package io.continual.builder.sources;

import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;

public class BuilderStringDataSourceTest
{
	@Test
	public void testConstructor ()
	{
		Assert.assertNotNull ( new BuilderStringDataSource ( null ) );
	}

	@Test ( expected = BuildFailure.class )
	public void testGetClassNameFromData () throws BuildFailure
	{
		new BuilderStringDataSource ( null ).getClassNameFromData ();
	}
}
