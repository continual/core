package io.continual.services.model.client;

import java.io.IOException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.Model.ObjectUpdater;
import io.continual.services.model.core.Model.RelationType;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.subpathWrapper.SubpathWrapperModel;
import io.continual.util.naming.Path;

/**
 * A model session wraps a model with session information such as the operator ID.
 */
public class ModelConnection
{
	public ModelConnection ( Model model, Identity user ) throws BuildFailure
	{
		this ( model, Path.getRootPath (), user );
	}

	public ModelConnection ( Model model, Path subPath, Identity user ) throws BuildFailure
	{
		fOperator = user;
		fContext = null;

		fModel = subPath.isRootPath () ? model : new SubpathWrapperModel ( model, subPath, model.getId () );
	}

	public static ModelConnection subPathFrom ( ModelConnection mc, Path subPath ) throws BuildFailure
	{
		return new ModelConnection ( mc.fModel, subPath, mc.fOperator );
	}

	public Identity getOperator () { return fOperator; }
	
	public String getAcctId () { return fModel.getAcctId (); }

	public String getId () { return fModel.getId (); }

	public long getMaxPathLength () { return fModel.getMaxPathLength (); }

	public long getMaxRelnNameLength () { return fModel.getMaxRelnNameLength (); }

	public long getMaxSerializedObjectLength () { return fModel.getMaxSerializedObjectLength (); }

	public void close () throws IOException { fModel.close (); }

	public boolean exists ( Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		return fModel.exists ( getContext(), objectPath );
	}

	public ModelPathList listChildrenOfPath ( Path parentPath ) throws ModelServiceException, ModelItemDoesNotExistException, ModelRequestException
	{
		return fModel.listChildrenOfPath ( getContext(), parentPath );
	}

	public ModelQuery startQuery () throws ModelRequestException, ModelServiceException
	{
		return fModel.startQuery ();
	}

	public ModelTraversal startTraversal () throws ModelRequestException
	{
		return fModel.startTraversal ();
	}

	public ModelConnection createIndex ( String field ) throws ModelRequestException, ModelServiceException
	{
		fModel.createIndex ( field );
		return this;
	}

	public BasicModelObject load ( Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		return fModel.load ( getContext(), objectPath );
	}

	public <T> T load ( Path objectPath, Class<T> clazz ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		return fModel.load ( getContext(), objectPath, clazz );
	}

	public <T> T load ( Path objectPath, ModelObjectFactory<T> factory ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		return fModel.load ( getContext(), objectPath, factory );
	}

	/**
	 * Store an object to the model.
	 * @param <T>
	 * @param obj
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	public <T extends ModelObjectWriter> void store ( T obj ) throws ModelRequestException, ModelServiceException
	{
		final JsonModelObject data = new JsonModelObject ();
		obj.serializeTo ( data );
		
		createUpdate ( obj.getId () )
			.overwrite ( data )
			.execute ()
		;
	}

	public ObjectUpdater createUpdate ( Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		return fModel.createUpdate ( getContext(), objectPath );
	}

	public boolean remove ( Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		return fModel.remove ( getContext(),  objectPath );
	}

	public ModelConnection setRelationType ( String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		fModel.setRelationType ( getContext(), relnName, rt );
		return this;
	}

	public ModelRelationInstance relate ( ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fModel.relate ( getContext(), reln );
	}

	public boolean unrelate ( ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fModel.unrelate ( getContext(), reln );
	}

	public boolean unrelate ( String relnId ) throws ModelServiceException, ModelRequestException
	{
		return fModel.unrelate ( getContext(), relnId );
	}

	/**
	 * Relation Selector
	 */
	public interface RelationSelector 
	{
		/**
		 * Select relations with any name
		 * @return this selector
		 */
		default RelationSelector withAnyName () { return named ( null ); }

		/**
		 * Select relations with the given name only
		 * @param name Use null to mean any name
		 * @return this selector
		 */
		RelationSelector named ( String name );

		/**
		 * Select inbound relations only
		 * @return this selector
		 */
		default RelationSelector inboundOnly () { return inbound(true).outbound(false); } 

		/**
		 * Select inbound relations if the parameter is true 
		 * @param wantInbound
		 * @return this selector
		 */
		RelationSelector inbound ( boolean wantInbound );

		/**
		 * Select outbound relations only
		 * @return this selector
		 */
		default RelationSelector outboundOnly () { return inbound(false).outbound(true); }

		/**
		 * Select outbound relations if the parameter is true
		 * @param wantOutbound
		 * @return this selector
		 */
		RelationSelector outbound ( boolean wantOutbound );

		/**
		 * Select both inbound and outbound relations
		 * @return this selector
		 */
		default RelationSelector inboundAndOutbound () { return inbound(true).outbound (true); }

		/**
		 * Get the requested relations
		 * @return a model relation list
		 * @throws ModelServiceException
		 * @throws ModelRequestException
		 */
		ModelRelationList getRelations ( ) throws ModelServiceException, ModelRequestException;
	};

	public RelationSelector selectRelations ( Path objectPath )
	{
		final Model.RelationSelector mrs = fModel.selectRelations ( objectPath );
		return new RelationSelector ()
		{
			@Override
			public RelationSelector named ( String name ) { mrs.named ( name ); return this; }

			@Override
			public RelationSelector inbound ( boolean wantInbound ) { mrs.inbound ( wantInbound ); return this; }

			@Override
			public RelationSelector outbound ( boolean wantOutbound ) { mrs.outbound ( wantOutbound ); return this; }

			@Override
			public ModelRelationList getRelations () throws ModelServiceException, ModelRequestException
			{
				return mrs.getRelations ( getContext() );
			}
		};
	}

	private final Model fModel;
	private final Identity fOperator;
	private ModelRequestContext fContext;

	private ModelRequestContext getContext () throws ModelServiceException
	{
		try
		{
			if ( fContext == null )
			{
				fContext = fModel.getRequestContextBuilder ()
					.forUser ( fOperator )
					.build ()
				;
			}
			return fContext;
		}
		catch ( BuildFailure x )
		{
			throw new ModelServiceException ( x );
		}
	}
}
