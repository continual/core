package io.continual.flowcontrol.impl.controller.k8s.impl;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlDeploymentService.ServiceException;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.time.Clock;

public class ModelBackedImageMapper extends JsonDataImageMapper implements ContainerImageMapper
{
	public static final String kSetting_ReadIntervalMs = "readIntervalMs";
	public static final long kDefault_ReadIntervalMs = 1000L * 60 * 5;
	
	public ModelBackedImageMapper ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		fModel = sc.getReqd ( "db", Model.class );
		fReadIntervalMs = sc.getExprEval ().evaluateTextToLong ( config.opt ( kSetting_ReadIntervalMs ), kDefault_ReadIntervalMs );
		fLastReadMs = 0;
		fMap = new LinkedList<> ();
	}

	@Override
	protected List<Rule> getMap () throws ServiceException
	{
		final long nowMs = Clock.now ();
		if ( nowMs > fLastReadMs + fReadIntervalMs )
		{
			readModelMap ();
			fLastReadMs = nowMs;
		}
		return fMap;
	}

	private final Model fModel;
	private final long fReadIntervalMs;

	private LinkedList<Rule> fMap;
	private long fLastReadMs;

	private static final Path skMapPath = Path.getRootPath ()
		.makeChildItem ( Name.fromString ( "imageMap" ) )
	;

	private void readModelMap () throws ServiceException
	{
		try
		{
			final ModelRequestContext mrc = fModel.getRequestContextBuilder ().build ();
			final BasicModelObject mo = fModel.load ( mrc, skMapPath );
			final JSONObject data = JsonModelObject.modelObjectToJson ( mo.getData () );

			fMap.clear ();
			fMap.addAll ( super.readMapData ( data ) );
		}
		catch ( BuildFailure | ModelServiceException | ModelRequestException e )
		{
			throw new ServiceException ( e );
		}
	}
}
