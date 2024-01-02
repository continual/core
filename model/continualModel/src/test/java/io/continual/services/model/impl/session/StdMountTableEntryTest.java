package io.continual.services.model.impl.session;

import java.io.IOException;

import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class StdMountTableEntryTest extends TestCase
{
	@Test
	public void testPathMatching () throws IOException, BuildFailure, ModelSchemaViolationException, ModelRequestException, ModelServiceException
	{
		final StdMountTableEntry e = new StdMountTableEntry ( Path.fromString ( "/foo" ), null );
		final Path global = e.getGlobalPath ( Path.fromString ( "/bar/1" ) );
		assertEquals ( "/foo/bar/1", global.toString () );
	}
}
