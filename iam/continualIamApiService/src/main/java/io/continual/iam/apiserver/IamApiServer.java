package io.continual.iam.apiserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.Server;
import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class IamApiServer extends Server<ServiceContainer>
{
	protected IamApiServer ()
	{
		super ( "IAM API Server", new StdFactory () );
	}

	public static void main ( String[] args )
	{
		try
		{
			new IamApiServer ()
				.runFromMain ( args )
			;
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			log.error ( e.getMessage (), e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( IamApiServer.class );
}
