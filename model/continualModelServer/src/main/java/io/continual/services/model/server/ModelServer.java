package io.continual.services.model.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.Server;
import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class ModelServer extends Server<ServiceContainer>
{
	protected ModelServer ()
	{
		super ( "ModelServer", new StdFactory () );
	}

	public static void main ( String[] args )
	{
		try
		{
			new ModelServer ()
				.runFromMain ( args )
			;
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			log.error ( e.getMessage (), e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( ModelServer.class );
}
