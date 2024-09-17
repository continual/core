package io.continual.flowcontrol.impl.jobdb.model;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.impl.common.JsonJob;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.util.data.json.JsonUtil;

class ModelJob extends JsonJob
{
	public ModelJob ( ModelObjectFactory.ObjectCreateContext<?> ctx )
	{
		super ( JsonModelObject.modelObjectToJson ( ctx.getData () ) );
	}
	
	public ModelJob ( JSONObject data )
	{
		super ( data );
	}

	public JSONObject toJson ()
	{
		return JsonUtil.clone ( super.directDataAccess () );
	}

	protected static class ModelJobBuilder extends JsonJob.JsonJobBuilder
	{
		public ModelJobBuilder ( FlowControlCallContext fccc, Encryptor enc )
		{
			super ( fccc, enc );
		}

		@Override
		public ModelJob build () throws BuildFailure
		{
			return new ModelJob ( getBuildingData () );
		}
	}
}
