package io.continual.services.model.core.updaters;

import org.json.JSONObject;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelUpdater;
import io.continual.util.data.json.JsonUtil;

public class DataMerge implements ModelUpdater
{
	public DataMerge ( JSONObject jsonData )
	{
		fData = JsonUtil.clone ( jsonData );
	}

	@Override
	public ModelOperation[] getAccessRequired ()
	{
		return new ModelOperation[] { ModelOperation.UPDATE };
	}

	@Override
	public void update ( ModelRequestContext context, ModelObject o )
	{
		o.patchData ( fData );
	}

	private final JSONObject fData;
}
