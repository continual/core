package io.continual.services.model.impl.json;

import java.util.LinkedList;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BaseRelationSelector;
import io.continual.util.naming.Path;

public class CommonRelationSelector extends BaseRelationSelector<CommonJsonDbModel> implements Model.RelationSelector
{
	public CommonRelationSelector ( CommonJsonDbModel model, Path obj )
	{
		super ( model, obj );
	}

	@SuppressWarnings("deprecation")
	@Override
	public ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException
	{
		final String named = getNameFilter ();
		
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();
		if ( wantInbound() ) result.addAll ( getModel().getInboundRelationsNamed ( context, getObject(), named ) );
		if ( wantOutbound() ) result.addAll ( getModel().getOutboundRelationsNamed ( context, getObject(), named ) );

		return ModelRelationList.simpleListOfCollection ( result );
	}
}
