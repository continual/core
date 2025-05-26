package io.continual.services.processor.library.model.common.fetchers;

import io.continual.services.processor.engine.model.SimpleMessageAndRouting;
import org.json.JSONObject;

import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.util.naming.Path;

class BaseFetcher
{
	protected MessageAndRouting buildMessageAndRouting ( Path path, BasicModelObject mo, String pipeline )
	{
		final JSONObject modelObjJson = new JSONObject ()
			.put ( "id", path.toString () )
			.put ( "metadata", mo.getMetadata ().toJson () )
			.put ( "data", JsonModelObject.modelObjectToJson ( mo.getData () ) )
		;
		final Message msg = Message.adoptJsonAsMessage ( modelObjJson );
		return new SimpleMessageAndRouting ( msg, pipeline );
	}
}
