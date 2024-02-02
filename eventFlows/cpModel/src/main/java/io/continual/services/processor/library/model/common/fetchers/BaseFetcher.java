package io.continual.services.processor.library.model.common.fetchers;

import org.json.JSONObject;

import io.continual.services.model.core.ModelObject;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.util.naming.Path;

class BaseFetcher
{
	protected MessageAndRouting buildMessageAndRouting ( Path path, ModelObject mo, String pipeline )
	{
		final JSONObject modelObjJson = new JSONObject ()
			.put ( "id", path.toString () )
			.put ( "metadata", mo.getMetadata ().toJson () )
			.put ( "data", mo.getData () )
		;
		final Message msg = Message.adoptJsonAsMessage ( modelObjJson );
		return new MessageAndRouting ( msg, pipeline );
	}
}
