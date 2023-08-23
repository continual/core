package io.continual.services.model.impl.delegator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.ModelUpdater;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.services.model.impl.json.CommonJsonDbObjectContainer;
import io.continual.util.naming.Path;

public class DelegatingModel extends SimpleService implements Model
{
	public DelegatingModel ( ServiceContainer sc, JSONObject config )
	{
		this ( config.getString ( "acctId" ), config.getString ( "modelId" ) );
	}

	public DelegatingModel ( String acctId, String modelId )
	{
		fAcctId = acctId;
		fModelId = modelId;
		fUserMountTable = new LinkedList<>();
	}

	/**
	 * Mount a model. The model mount instance is read-only, allowing the caller to mount shared/global models.
	 * @param mm
	 * @return this model
	 */
	public DelegatingModel mount ( ModelMount mm )
	{
		fUserMountTable.add ( mm );
		return this;
	}

	@Override
	public String getAcctId ()
	{
		return fAcctId;
	}

	@Override
	public String getId ()
	{
		return fModelId;
	}

	@Override
	public long getMaxPathLength ()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMaxRelnNameLength ()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMaxSerializedObjectLength ()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close () throws IOException
	{
		for ( ModelMount mm : fUserMountTable )
		{
			final Model m = mm.getModel ();
			log.info ( "Closing " + m.getAcctId () + "/" + m.getId () );
			m.close ();
		}
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ();
	}

	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		try
		{
			final ModelMount mm = getModelForPath ( objectPath );
			if ( mm.getModel () == this )
			{
				for ( ModelMount um : fUserMountTable )
				{
					final Path mp = um.getMountPoint ();

					// is this mount point below the given path prefix?
					if ( mp.startsWith ( objectPath ) )
					{
						return true;
					}
				}
				return false;
			}
			else
			{
				return mm.getModel ().exists ( context, mm.getPathWithinModel ( objectPath ) );
			}
		}
		catch ( ModelRequestException e )
		{
			return false;
		}
	}

	@Override
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
	{
		final ModelMount mm = getModelForPath ( prefix );
		if ( mm.getModel () == this )
		{
			final LinkedList<Path> result = new LinkedList<>();
			for ( ModelMount um : fUserMountTable )
			{
				final Path mp = um.getMountPoint ();

				// is this mount point below the given path prefix?
				if ( mp.startsWith ( prefix ) )
				{
					// return just the next segment from the requested segment
					final Path childPart = mp.makePathWithinParent ( prefix );
					result.add ( Path.getRootPath ().makeChildItem ( childPart.getSegments ()[0] ) );
				}
			}
			return ModelPathList.wrap ( result );
		}
		else
		{
			return mm.getModel ().listChildrenOfPath ( context, mm.getPathWithinModel ( prefix ) );
		}
	}

	@Override
	public DelegatingModel createIndex ( String field ) throws ModelRequestException, ModelServiceException
	{
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
		final ModelMount mm = getModelForPath ( objectPath );
		if ( mm.getModel () == this )
		{
			// might be "/", but also could be "/foo" where "/foo/bar" is a mount point. We just return an object container either way.

			final LinkedList<Path> result = new LinkedList<>();
			for ( ModelMount um : fUserMountTable )
			{
				final Path mp = um.getMountPoint ();

				// is this mount point below the given path prefix?
				if ( mp.startsWith ( objectPath ) )
				{
					// return just the next segment from the requested segment
					final Path childPart = mp.makePathWithinParent ( objectPath );
					result.add ( Path.getRootPath ().makeChildItem ( childPart.getSegments ()[0] ) );
				}
			}

			return CommonJsonDbObjectContainer.createObjectContainer ( objectPath.toString (), result );
		}
		else
		{
			return mm.getModel ().load ( context, mm.getPathWithinModel ( objectPath ) );
		}
	}

	@Override
	public void store ( ModelRequestContext context, Path objectPath, ModelUpdater ... updates ) throws ModelRequestException, ModelServiceException
	{
		final ModelMount mm = getModelForPath ( objectPath );
		if ( mm.getModel () == this ) throw new ModelRequestException ( "Cannot store here." );

		mm.getModel ().store ( context, mm.getPathWithinModel ( objectPath ), updates );
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		final ModelMount mm = getModelForPath ( objectPath );
		if ( mm.getModel () == this ) throw new ModelRequestException ( "Cannot remove here." );

		return mm.getModel ().remove ( context, mm.getPathWithinModel ( objectPath ) );
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		// for each relation, we check if both sides are in the same model. If so, let the model handle the relation. If not,
		// we store it in our "local" store.

		final Path from = mr.getFrom ();
		final ModelMount mmFrom = getModelForPath ( from );

		final Path to = mr.getTo ();
		final ModelMount mmTo = getModelForPath ( to );

		if ( mmFrom == mmTo )	// same instance
		{
			return mmFrom.getModel ().relate ( context, new ModelRelation ()
			{
				@Override
				public Path getFrom () { return mmFrom.getPathWithinModel ( from ); }

				@Override
				public Path getTo () { return mmTo.getPathWithinModel ( to ); }

				@Override
				public String getName () { return mr.getName (); }
			} );
		}
		else
		{
			// store it in this model's backing data
			// (FIXME)
			throw new ModelServiceException ( "not yet implemented across models" );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		// for each relation, we check if both sides are in the same model. If so, let the model handle the relation. If not,
		// we store it in our "local" store.

		final Path from = reln.getFrom ();
		final ModelMount mmFrom = getModelForPath ( from );

		final Path to = reln.getTo ();
		final ModelMount mmTo = getModelForPath ( to );

		if ( mmFrom == mmTo )	// same instance
		{
			return mmFrom.getModel ().unrelate ( context, new ModelRelation ()
			{
				@Override
				public Path getFrom () { return mmFrom.getPathWithinModel ( from ); }

				@Override
				public Path getTo () { return mmTo.getPathWithinModel ( to ); }

				@Override
				public String getName () { return reln.getName (); }
			} );
		}
		else
		{
			// store it in this model's backing data
			// (FIXME)
			throw new ModelServiceException ( "not yet implemented across models" );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		for ( ModelMount mountEntry : fUserMountTable )
		{
			if ( mountEntry.getModel ().unrelate ( context, relnId ) )
			{
				return true;
			}
		}

		// FIXME: check cross-model relations

		return false;
	}

	@Override
	public List<ModelRelationInstance> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		// we need to check our own data source as well as the model hosting the object

		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final ModelMount mm = getModelForPath ( forObject );

		try
		{
			final ModelRequestContext mrc = mm.getModel().getRequestContextBuilder ()
				.forUser ( context.getOperator () )
				.build ()
			;

			for ( ModelRelationInstance mrInternal : mm.getModel ().getInboundRelations ( mrc, mm.getPathWithinModel ( forObject ) ) )
			{
				result.add ( new ToGlobalMapper ( mm, mrInternal ) );
			}
		}
		catch ( BuildFailure e )
		{
			throw new ModelServiceException ( e );
		}

		return result;
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		// we need to check our own data source as well as the model hosting the object

		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final ModelMount mm = getModelForPath ( forObject );

		try
		{
			final ModelRequestContext mrc = mm.getModel().getRequestContextBuilder ()
				.forUser ( context.getOperator () )
				.build ()
			;

			for ( ModelRelationInstance mrInternal : mm.getModel ().getOutboundRelations ( mrc, mm.getPathWithinModel ( forObject ) ) )
			{
				result.add ( new ToGlobalMapper ( mm, mrInternal ) );
			}
		}
		catch ( BuildFailure e )
		{
			throw new ModelServiceException ( e );
		}

		return result;
	}

	@Override
	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		// we need to check our own data source as well as the model hosting the object

		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final ModelMount mm = getModelForPath ( forObject );

		try
		{
			final ModelRequestContext mrc = mm.getModel().getRequestContextBuilder ()
				.forUser ( context.getOperator () )
				.build ()
			;

			for ( ModelRelationInstance mrInternal : mm.getModel ().getInboundRelationsNamed ( mrc, mm.getPathWithinModel ( forObject ), named ) )
			{
				result.add ( new ToGlobalMapper ( mm, mrInternal ) );
			}
		}
		catch ( BuildFailure e )
		{
			throw new ModelServiceException ( e );
		}

		return result;
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		// we need to check our own data source as well as the model hosting the object

		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final ModelMount mm = getModelForPath ( forObject );

		try
		{
			final ModelRequestContext mrc = mm.getModel().getRequestContextBuilder ()
				.forUser ( context.getOperator () )
				.build ()
			;

			for ( ModelRelationInstance mrInternal : mm.getModel ().getOutboundRelationsNamed ( mrc, mm.getPathWithinModel ( forObject ), named ) )
			{
				result.add ( new ToGlobalMapper ( mm, mrInternal ) );
			}
		}
		catch ( BuildFailure e )
		{
			throw new ModelServiceException ( e );
		}

		return result;
	}

	private final String fAcctId;
	private final String fModelId;
	private final LinkedList<ModelMount> fUserMountTable;

	private static class ToGlobalMapper implements ModelRelationInstance
	{
		public ToGlobalMapper ( ModelMount mount, ModelRelationInstance internal )
		{
			fModelMount = mount;
			fInternalReln = internal;
		}

		@Override
		public String getId () { return fInternalReln.getId (); }

		@Override
		public Path getFrom () { return fModelMount.getGlobalPath ( fInternalReln.getFrom () ); }

		@Override
		public Path getTo () { return fModelMount.getGlobalPath ( fInternalReln.getTo () ); }

		@Override
		public String getName () { return fInternalReln.getName (); }

		private final ModelRelationInstance fInternalReln;
		private final ModelMount fModelMount;
	}
	
	private ModelMount getModelForPath ( Path modelPath )
	{
		for ( ModelMount mountEntry : fUserMountTable )
		{
			if ( mountEntry.contains ( modelPath ) )
			{
				return mountEntry;
			}
		}

		return new ModelMount ()
		{
			@Override
			public JSONObject toJson () { return new JSONObject (); }

			@Override
			public Path getMountPoint () { return Path.getRootPath (); }

			@Override
			public boolean contains ( Path path ) { return true; }

			@Override
			public Model getModel () { return DelegatingModel.this; }

			@Override
			public Path getPathWithinModel ( Path absolutePath ) { return absolutePath; }

			@Override
			public Path getGlobalPath ( Path from ) { return null; }
		};
	}

	private static final Logger log = LoggerFactory.getLogger ( DelegatingModel.class );
}
