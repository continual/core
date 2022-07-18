package io.continual.services.processor.library.model.common;

import java.util.concurrent.TimeUnit;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;

public interface ObjectFetcher
{
	/**
	 * Is this fetcher at EOF?
	 * @return true or false
	 */
	boolean isEof ();

	/**
	 * Get the next message
	 * @param spc
	 * @param model
	 * @param waitAtMost
	 * @param waitAtMostTimeUnits
	 * @param pipeline 
	 * @return a message or null 
	 */
	MessageAndRouting getNextMessage ( StreamProcessingContext spc, Model model, long waitAtMost, TimeUnit waitAtMostTimeUnits, String pipeline ) throws ModelRequestException, ModelServiceException;
}
