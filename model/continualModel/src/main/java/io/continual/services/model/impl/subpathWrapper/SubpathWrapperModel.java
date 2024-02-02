package io.continual.services.model.impl.subpathWrapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.util.naming.Path;

/**
 * Given a base model and a path, present a model layer whose root is at the given path in the
 * base model.
 */
public class SubpathWrapperModel extends SimpleService implements Model
{
	/**
	 * Construct a subpath model service from a service container and configuration
	 * @param sc
	 * @param config
	 * @throws BuildFailure 
	 * @throws JSONException 
	 */
	public SubpathWrapperModel ( ServiceContainer sc, JSONObject config ) throws JSONException, BuildFailure
	{
		this (
			sc.getReqd ( config.getString ( "backingModel" ), Model.class ),
			Path.fromString ( config.getString ( "basePath" ) ),
			config.getString ( "modelId" )
		);
	}

	/**
	 * Construct a delegating model from basic information
	 * @param backingModel
	 * @param basePath
	 * @param modelId
	 * @throws BuildFailure 
	 */
	public SubpathWrapperModel ( Model backingModel, Path basePath, String modelId ) throws BuildFailure
	{
		fBackingModel = backingModel;
		fBasePath = basePath;
		fModelId = modelId;
	}

	@Override
	public String getAcctId () { return fBackingModel.getAcctId (); }

	@Override
	public String getId () { return fModelId; }

	@Override
	public long getMaxPathLength () { return fBackingModel.getMaxPathLength (); }

	@Override
	public long getMaxRelnNameLength () { return fBackingModel.getMaxRelnNameLength (); }

	@Override
	public long getMaxSerializedObjectLength () { return fBackingModel.getMaxSerializedObjectLength (); }

	@Override
	public void close () throws IOException
	{
		// we don't close the underlying model
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return fBackingModel.getRequestContextBuilder ();
	}

	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		return fBackingModel.exists ( context, userPathToBackingModel ( objectPath ) );
	}

	@Override
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
	{
		final ModelPathList backingList = fBackingModel.listChildrenOfPath ( context, userPathToBackingModel ( prefix ) );

		final LinkedList<Path> result = new LinkedList<>();
		for ( Path p : backingList )
		{
			result.add ( backingPathToUser ( p ) );
		}
		return ModelPathList.wrap ( result );
	}

	@Override
	public SubpathWrapperModel createIndex ( String field ) throws ModelRequestException, ModelServiceException
	{
		fBackingModel.createIndex ( field );
		return this;
	}

	@Override
	public ModelQuery startQuery () throws ModelRequestException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelTraversal startTraversal () throws ModelRequestException
	{
		return new SimpleTraversal ( this );
	}

	@Override
	public ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		return fBackingModel.load ( context, userPathToBackingModel ( objectPath ) );
	}

	@Override
	public ObjectUpdater createUpdate ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		return fBackingModel.createUpdate ( context, userPathToBackingModel ( objectPath ) );
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		return fBackingModel.remove ( context, userPathToBackingModel ( objectPath ) );
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		fBackingModel.setRelationType ( context, relnName, rt );
		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		return fBackingModel.relate ( context, userPathToBackingModel ( mr ) );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fBackingModel.unrelate ( context, userPathToBackingModel ( reln ) );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		return fBackingModel.unrelate ( context, relnId );
	}

	@Override
	public RelationSelector selectRelations ( Path objectPath )
	{
		final RelationSelector base = fBackingModel.selectRelations ( userPathToBackingModel ( objectPath ) );
		
		return new RelationSelector ()
		{
			@Override
			public RelationSelector named ( String name ) { base.named ( name ); return this; }

			@Override
			public RelationSelector inbound ( boolean wantInbound ) { base.inbound ( wantInbound ); return this; }

			@Override
			public RelationSelector outbound ( boolean wantOutbound ) { base.outbound ( wantOutbound ); return this; }

			@Override
			public ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException
			{
				final ModelRelationList result = base.getRelations ( context );
				return new ModelRelationList ()
				{
					@Override
					public Iterator<ModelRelationInstance> iterator ()
					{
						final Iterator<ModelRelationInstance> it = result.iterator ();
						return new Iterator<ModelRelationInstance> ()
						{
							@Override
							public boolean hasNext () { return it.hasNext (); }

							@Override
							public ModelRelationInstance next ()
							{
								return backingPathToUser ( it.next () );
							}
						};
					}
				};
			}
		};
	}

	private final Model fBackingModel;
	private final Path fBasePath;
	private final String fModelId;

	private Path userPathToBackingModel ( Path p )
	{
		return fBasePath.makeChildPath ( p );
	}

	private Path backingPathToUser ( Path p )
	{
		return p.makePathWithinParent ( fBasePath );
	}

	private ModelRelation userPathToBackingModel ( ModelRelation mr )
	{
		return ModelRelation.from (
			userPathToBackingModel ( mr.getFrom () ), 
			mr.getName (),
			userPathToBackingModel ( mr.getTo () )
		);
	}

	private ModelRelationInstance backingPathToUser ( ModelRelationInstance mr )
	{
		return ModelRelationInstance.from (
			backingPathToUser ( mr.getFrom () ), 
			mr.getName (),
			backingPathToUser ( mr.getTo () )
		);
	}
}
