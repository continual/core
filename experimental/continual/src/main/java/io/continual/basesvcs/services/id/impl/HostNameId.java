package io.continual.basesvcs.services.id.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONObject;

import io.continual.basesvcs.services.id.NodeIdService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class HostNameId extends SimpleService implements NodeIdService
{
	public HostNameId ( ServiceContainer sc, JSONObject config )
	{
		super ( sc, config );
	}

	@Override
	public String getNodeId ()
	{
		return sfHostname;
	}

	private static String sfHostname;
	static
	{
		try
		{
			sfHostname = InetAddress.getLocalHost().getHostName();
		}
		catch ( UnknownHostException e )
		{
			sfHostname = "error";
		}
	}
}
