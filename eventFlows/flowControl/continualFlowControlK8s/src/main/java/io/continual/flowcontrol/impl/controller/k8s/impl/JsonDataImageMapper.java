package io.continual.flowcontrol.impl.controller.k8s.impl;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.model.FlowControlDeploymentService.ServiceException;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.services.ServiceContainer;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public abstract class JsonDataImageMapper implements ContainerImageMapper
{
	public JsonDataImageMapper ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
	}

	@Override
	public String getImageName ( FlowControlRuntimeSpec rs ) throws RequestException, ServiceException
	{
		final String request = rs.getName () + ":" + rs.getVersion ();
		for ( Rule rule : getMap() )
		{
			if ( request.matches ( rule.fRegex ) )
			{
				return rule.fContainer;
			}
		}

		throw new RequestException ( "Couldn't map runtime specification " + request + " to a container image." );
	}

	protected abstract List<Rule> getMap () throws ServiceException;
	
	protected class Rule
	{
		public Rule ( String regex, String container )
		{
			fRegex = regex;
			fContainer = container;
		}

		public final String fRegex;
		public final String fContainer;
	}

	protected List<Rule> readMapData ( JSONObject data ) throws ServiceException
	{
		final LinkedList<Rule> map = new LinkedList<> ();
		
		try
		{
			JsonVisitor.forEachElement ( data.optJSONArray ( "rules" ), new ArrayVisitor <JSONObject,ServiceException> ()
			{
				@Override
				public boolean visit ( JSONObject ruleData ) throws JSONException, ServiceException
				{
					final String match = ruleData.getString ( "match" );
					final String image = ExpressionEvaluator.getStandardEvaluator ()
						.evaluateText ( ruleData.getString ( "image" ) )
					;
					map.add ( new Rule ( match, image ) );
					return true;
				}
				
			} );
		}
		catch ( JSONException e )
		{
			throw new ServiceException ( e );
		}

		return map;
	}
}
