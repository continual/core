package io.continual.client.examples;

import java.io.IOException;

import io.continual.client.ClientBuilders;
import io.continual.client.model.ModelClient;
import io.continual.client.model.ModelClient.ModelServiceException;
import io.continual.client.model.ModelObjectLocator;
import io.continual.client.model.ModelReference;

public class ModelClientExample
{
	public static void main ( String[] args )
	{
		try
		{
			final ModelClient mc = new ClientBuilders.ModelClientBuilder ()
				.usingUrl ( "http://localhost:3172" )
				.asUser ( "peter@rathravane.com", "some password" )
				.build ()
			;
			for ( String model : mc.getModels ( "peter" ) )
			{
				System.out.println ( model );

				final ModelReference mr = mc.getObject ( new ModelObjectLocator ( "peter", model, "bee" ) );

				mr.putData ( "{'foo':'bar'}" );

				final String data = mr.getData ();
				System.out.println ( data );
			}
		}
		catch ( IOException | ModelServiceException e )
		{
			System.err.println ( e.getMessage () );
		}
	}
}
