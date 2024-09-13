package io.continual.flowcontrol.impl.config;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.flowcontrol.impl.common.JsonDeploymentSpec;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.flowcontrol.services.config.ConfigFetcher;
import io.continual.resources.ResourceLoader;
import io.continual.util.data.json.CommentedJsonTokener;

public class StandardConfigFetcher implements ConfigFetcher
{
	public StandardConfigFetcher ()
	{
	}

	@Override
	public FlowControlDeploymentSpec fetchDeployment ( String configToken ) throws IOException, ConfigFormatException
	{
		try ( InputStream is = ResourceLoader.load ( configToken ) )
		{
			if ( is == null )
			{
				log.info ( "Couldn't find resource [{}]", configToken );
				return null;
			}

			return new JsonDeploymentSpec ( new JSONObject ( new CommentedJsonTokener ( is ) ) );
		}
		catch ( JSONException x )
		{
			throw new ConfigFormatException ( x );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( StandardConfigFetcher.class );
}
