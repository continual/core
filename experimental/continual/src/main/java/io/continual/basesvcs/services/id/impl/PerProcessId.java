package io.continual.basesvcs.services.id.impl;

import java.util.UUID;

import org.json.JSONObject;

import io.continual.basesvcs.services.id.NodeIdService;
import io.continual.services.ServiceContainer;

public class PerProcessId extends HostNameId implements NodeIdService
{
	public PerProcessId ( ServiceContainer sc, JSONObject config )
	{
		super ( sc, config );

		fUuid = UUID.randomUUID ().toString ();
	}

	@Override
	public String getNodeId ()
	{
		return super.getNodeId () + ":" + fUuid;
	}

	private final String fUuid;
}
