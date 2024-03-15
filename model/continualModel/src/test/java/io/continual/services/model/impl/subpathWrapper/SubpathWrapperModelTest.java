package io.continual.services.model.impl.subpathWrapper;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.mem.InMemoryModel;
import io.continual.services.model.util.TestIdentity;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class SubpathWrapperModelTest extends TestCase
{
	@Test
	public void testSimpleWrapper () throws BuildFailure, ModelSchemaViolationException, JSONException, ModelRequestException, ModelServiceException, IOException
	{
		final InMemoryModel mem = new InMemoryModel ( "acct", "name" );

		final ModelRequestContext context = mem.getRequestContextBuilder ()
			.forUser ( new TestIdentity ( "tester" ) )
			.build ()
		;
		mem.createUpdate ( context, Path.fromString ( "/foo" ) )
			.overwrite ( new JsonModelObject ( new JSONObject () ) )
			.execute ()
		;
		mem.createUpdate ( context, Path.fromString ( "/foo/bar" ) )
			.overwrite ( new JsonModelObject ( new JSONObject () ) )
			.execute ()
		;
		mem.createUpdate ( context, Path.fromString ( "/foo/bar/baz" ) )
			.overwrite ( new JsonModelObject ( new JSONObject ()
				.put ( "status", "expired" )
			) )
			.execute ()
		;

		final BasicModelObject bmo = mem.load ( context, Path.fromString ( "/foo/bar/baz" ) );
		assertNotNull ( bmo );
		assertEquals ( "expired", bmo.getData ().get ( "status" ) );
		
		try ( final SubpathWrapperModel spwm = new SubpathWrapperModel ( mem, Path.fromString ( "/foo/bar" ), "sub" ) )
		{
	
			final ModelRequestContext subctx = spwm.getRequestContextBuilder ()
				.forUser ( new TestIdentity ( "tester" ) )
				.build ()
			;
	
			final BasicModelObject mo = spwm.load ( subctx, Path.fromString ( "/baz") );
			assertEquals ( "expired", mo.getData ().getString ( "status" ) );
		}
	}
}
