package io.continual.modeltest;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.client.ModelClient;
import io.continual.util.naming.Path;
import io.continual.util.time.Clock;

public class ModelTest
{
	public static void main ( String[] args ) throws BuildFailure, ModelServiceException, ModelRequestException, IOException
	{
		log.info ( "starting." );
		
		final JSONObject config = new JSONObject ()
			.put ( "acctId", "foo" )
			.put ( "modelId", "test" )
			.put ( "username", "${CONTINUAL_USER}" )
			.put ( "password", "${CONTINUAL_PASSWORD}" )
		;
		try ( final ModelClient mc = new ModelClient ( new ServiceContainer (), config ) )
		{
			final ModelRequestContext mrc = mc.getRequestContextBuilder ()
				.build ()
			;
			for ( Path p : mc.listChildrenOfPath ( mrc, Path.getRootPath () ) )
			{
				System.out.println ( p.toString () );
			}

			final Path foo = Path.fromString ( "/foo" );
			final Path bar = Path.fromString ( "/bar" );

			final BasicModelObject mo = mc.load ( mrc, foo );
			final String data = mo.getData ().toString ();
			log.info ( "/foo: " + data );

			mc.createUpdate ( mrc, foo )
				.merge ( new JsonModelObject ( new JSONObject ().put ( "ModelTestClient", Clock.now () ) ) )
				.merge ( new JsonModelObject ( new JSONObject ().put ( "MTC_artist",
					new JSONObject ()
						.put ( "playback", true )
						.put ( "composer", "Jóhann Jóhannsson" )
						.put ( "track", "Odi et Amo" )
				) ) )
				.execute ()
			;

			mc.createUpdate ( mrc, bar )
				.overwrite ( new JsonModelObject ( new JSONObject ()
					.put ( "details", "this is a target for relation testing" )
					.put ( "writetime", Clock.now () )
				) )
				.execute ()
			;

			final ModelRelationInstance mri = mc.relate ( mrc, ModelRelation.from ( foo, "fubar", bar ) );
			mc.unrelate ( mrc, mri.getId () );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( ModelTest.class );
}
