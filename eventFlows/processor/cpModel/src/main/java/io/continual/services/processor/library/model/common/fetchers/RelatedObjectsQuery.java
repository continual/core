package io.continual.services.processor.library.model.common.fetchers;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.model.common.ObjectFetcher;

public class RelatedObjectsQuery implements ObjectFetcher
{
	public RelatedObjectsQuery ( ServiceContainer sc, JSONObject config )
	{
	}

	@Override
	public boolean isEof ()
	{
		return true;
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, Model model, long waitAtMost,  TimeUnit waitAtMostTimeUnits, String pipeline ) throws ModelRequestException, ModelServiceException
	{
		return null;
	}
}
