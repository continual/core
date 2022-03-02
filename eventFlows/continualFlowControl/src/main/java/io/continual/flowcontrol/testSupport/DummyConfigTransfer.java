package io.continual.flowcontrol.testSupport;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.services.SimpleService;

public class DummyConfigTransfer extends SimpleService implements ConfigTransferService
{
	@Override
	public Map<String, String> deployConfiguration ( FlowControlJob job ) throws ServiceException
	{
		return new HashMap<> ();
	}

	@Override
	public InputStream fetch ( String byKey ) throws ServiceException
	{
		return null;
	}
}
