package io.continual.services.model.impl.common;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.Model.RelationSelector;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

public abstract class BaseRelationSelector<M extends Model> implements Model.RelationSelector
{
	public BaseRelationSelector ( M model, Path obj )
	{
		fModel = model;
		fObject = obj;
	}

	@Override
	public RelationSelector named ( String name )
	{
		fNameFilter = name;
		return this;
	}

	@Override
	public RelationSelector inbound ( boolean wantInbound )
	{
		fInbound = wantInbound;
		return this;
	}

	@Override
	public RelationSelector outbound ( boolean wantOutbound )
	{
		fOutbound = wantOutbound;
		return this;
	}

	@Override
	public abstract ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException;

	private final M fModel;
	private final Path fObject;
	private String fNameFilter = null;
	private boolean fInbound = true;
	private boolean fOutbound = true;

	protected M getModel () { return fModel; }
	protected Path getObject () { return fObject; }
	protected String getNameFilter () { return fNameFilter; }
	protected boolean wantInbound () { return fInbound; }
	protected boolean wantOutbound () { return fOutbound; }
	protected boolean nameMatches ( String name ) { return fNameFilter == null || fNameFilter.equals ( name ); }
}
