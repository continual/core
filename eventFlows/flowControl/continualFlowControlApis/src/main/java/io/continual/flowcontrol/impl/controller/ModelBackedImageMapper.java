package io.continual.flowcontrol.impl.controller;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.services.controller.ContainerImageMapper;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class ModelBackedImageMapper extends JsonDataImageMapper implements ContainerImageMapper
{
	public ModelBackedImageMapper ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		fModel = sc.getReqd ( sc.getExprEval ().evaluateText ( config.getString ( "model" ) ), Model.class );
	}

	@Override
	protected List<Rule> getMap () throws ServiceException
	{
		try ( final ModelRequestContext ctx = fModel.getRequestContextBuilder ().build ( )  )
		{
			final BasicModelObject bmo = fModel.load ( ctx, kImageMapPath );
			final ModelObject obj = bmo.getData ();
			final JSONObject json = JsonModelObject.modelObjectToJson ( obj );
			return readMapData ( json );
		}
		catch ( BuildFailure | JSONException | ServiceException | ModelServiceException | ModelRequestException e )
		{
			throw new ServiceException ( e );
		}
	}

	private final Model fModel;

    private static final Path kImageMapPath = Path.getRootPath ().makeChildItem ( Name.fromString ( "flowControlImageMapper" ) );
}
