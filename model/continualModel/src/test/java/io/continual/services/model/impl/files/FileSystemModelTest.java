package io.continual.services.model.impl.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class FileSystemModelTest extends TestCase
{
	@Test
	public void testEmptyDirRemoval () throws IOException, BuildFailure, ModelSchemaViolationException, ModelRequestException, ModelServiceException
	{
		try
		{
			final java.nio.file.Path baseDir = Files.createTempDirectory ( "continualModelTest-" );
			log.info ( "test base dir: " + baseDir.toString () );

			try ( final FileSystemModel model = new FileSystemModel ( "test", "test", baseDir ) )
			{
				final ModelRequestContext mrc = model.getRequestContextBuilder ()
					.forUser ( new TestIdentity() )
					.build ()
				;
				model.store ( mrc, Path.fromString ( "/foo/bar" ), new JSONObject () );
				model.store ( mrc, Path.fromString ( "/foo/baz" ), new JSONObject () );

				model.relate ( mrc, ModelRelation.from ( Path.fromString ( "/foo/bar" ), "testWith", Path.fromString ( "/foo/baz" ) ) );

				model.remove ( mrc, Path.fromString ( "/foo/bar" ) );
				model.remove ( mrc, Path.fromString ( "/foo/baz" ) );

				assertTrue ( baseDir.toFile ().exists () );
				assertTrue ( new File ( baseDir.toFile (), "objects" ).exists () );
				assertFalse ( new File ( new File ( baseDir.toFile (), "objects" ), "foo" ).exists () );
			}
		}
		catch ( IOException e )
		{
			throw e;
		}
	}

	private static class TestIdentity extends CommonJsonIdentity 
	{
		public TestIdentity ( )
		{
			super ( "test", CommonJsonIdentity.initializeIdentity (), null );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( FileSystemModelTest.class );
}
