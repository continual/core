package io.continual.flowcontrol.impl.controller.k8s.impl;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.controller.k8s.ContainerImageMapper;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.services.ServiceContainer;

public class InlineImageMapper extends JsonDataImageMapper implements ContainerImageMapper
{
	public InlineImageMapper ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );
		try
		{
			fRules = readMapData ( config );
		}
		catch ( JSONException | ServiceException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	protected List<Rule> getMap () { return fRules; }

	private final List<Rule> fRules;
}
