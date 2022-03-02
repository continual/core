package io.continual.flowcontrol.testSupport;

import java.util.Collection;

import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.jobapi.FlowControlJob;
import io.continual.flowcontrol.jobapi.FlowControlJobDb;
import io.continual.iam.access.AccessException;
import io.continual.services.SimpleService;

public class DummyJobDb extends SimpleService implements FlowControlJobDb
{
	@Override
	public Builder createJob ( FlowControlCallContext fccc ) throws ServiceException
	{
		return null;
	}

	@Override
	public Collection<FlowControlJob> getJobsFor ( FlowControlCallContext fccc ) throws ServiceException
	{
		return null;
	}

	@Override
	public FlowControlJob getJob ( FlowControlCallContext ctx, String name ) throws ServiceException, AccessException
	{
		return null;
	}

	@Override
	public FlowControlJob getJobAsAdmin ( String name ) throws ServiceException
	{
		return null;
	}

	@Override
	public void storeJob ( FlowControlCallContext ctx, String name, FlowControlJob job ) throws ServiceException, AccessException, RequestException
	{
	}

	@Override
	public void removeJob ( FlowControlCallContext ctx, String name ) throws ServiceException, AccessException, RequestException
	{
	}
}
