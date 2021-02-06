package io.continual.client;

import java.net.MalformedURLException;

import io.continual.client.events.EventClient;
import io.continual.client.events.impl.StdEventClient;
import io.continual.client.model.ModelClient;
import io.continual.client.model.impl.StdModelClient;

/**
 * Various client builders
 * @author peter@rathravane.com
 */
public class ClientBuilders
{
	public static class EventClientBuilder
	{
		public EventClientBuilder asUser ( String user, String password )
		{
			fUser = user;
			fPassword = password;
			return this;
		}

		public EventClientBuilder usingApiKey ( String apiKey, String apiSecret )
		{
			fApiKey = apiKey;
			fApiSecret = apiSecret;
			return this;
		}

		public EventClient build () throws MalformedURLException
		{
			return new StdEventClient ( fUser, fPassword, fApiKey, fApiSecret );
		}

		private String fUser = null;
		private String fPassword = null;
		private String fApiKey = null;
		private String fApiSecret = null;
	}

	public static class ModelClientBuilder
	{
		public ModelClientBuilder usingUrl ( String url )
		{
			fUrl = url;
			return this;
		}

		public ModelClientBuilder asUser ( String user, String password )
		{
			fUser = user;
			fPassword = password;
			return this;
		}

		public ModelClient build () throws MalformedURLException
		{
			return new StdModelClient ( fUrl, fUser, fPassword );
		}

		private String fUrl = "https://model.continual.io";
		private String fUser = null;
		private String fPassword = null;
	}
}
