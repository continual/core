package io.continual.services.model.impl.common;

import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

public abstract class ReadOnlyModel extends SimpleService implements Model
{
	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ();
	}

	@Override
	public ObjectUpdater createUpdate ( final ModelRequestContext context, final Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		failReadOnly ();
		return null;
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException
	{
		failReadOnly ();
		return false;
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelRequestException
	{
		failReadOnly ();
		return null;
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelRequestException
	{
		failReadOnly ();
		return false;
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelRequestException
	{
		failReadOnly ();
		return false;
	}

	@Override
	public Model createIndex ( String field ) throws ModelRequestException
	{
		failReadOnly ();
		return null;
	}

	private void failReadOnly () throws ModelRequestException
	{
		throw new ModelRequestException ( "This is a read-only model." );
	}
}
