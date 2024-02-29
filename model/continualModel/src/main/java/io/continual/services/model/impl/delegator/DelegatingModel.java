package io.continual.services.model.impl.delegator;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelItemList;
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
import io.continual.services.model.impl.common.BaseRelationSelector;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.services.model.impl.json.CommonJsonDbObjectContainer;
import io.continual.services.model.impl.mem.InMemoryModel;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * The delegating model provides a top-level model with other models presented at mount points.
 * The top-level model is read-only. For example, if you have a model mounted at "/foo" (and no
 * other mounts) you cannot write to "/bar".
 */
public class DelegatingModel extends SimpleService implements Model
{
	/**
	 * Construct a delegating model service from a service container and configuration
	 * @param sc
	 * @param config
	 * @throws BuildFailure 
	 * @throws JSONException 
	 */
	public DelegatingModel ( ServiceContainer sc, JSONObject config ) throws JSONException, BuildFailure
	{
		this ( config.getString ( "acctId" ), config.getString ( "modelId" ), sc.get ( "backingModel", Model.class ) );
	}

	/**
	 * Construct a delegating model from basic information
	 * @param acctId
	 * @param modelId
	 * @throws BuildFailure 
	 */
	public DelegatingModel ( String acctId, String modelId, Model backingModel ) throws BuildFailure
	{
		fAcctId = acctId;
		fModelId = modelId;
		fUserMountTable = new LinkedList<>();
		fBackingModel = backingModel == null ? new InMemoryModel ( acctId, modelId ) : backingModel;
	}

	/**
	 * Mount a model. The model mount instance is read-only, allowing the caller to mount shared/global models.
	 * That is, the same ModelMount instance can be used to mount a given model into multiple delegating models.
	 * @param mm a model mount specification
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
		fBackingModel.close ();
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
			// the root path always exists
			if ( objectPath.isRootPath () ) return true;

			// get the model...
			final ModelMount mm = getModelForPath ( objectPath );

			// if the requested path is in a delegated model, just forward the request
			if ( mm.getModel () != this )
			{
				return mm.getModel ().exists ( context, mm.getPathWithinModel ( objectPath ) );
			}

			// or if it's part of the backing model
			if ( !objectPath.isRootPath () && fBackingModel.exists ( context, objectPath ) )
			{
				return true;
			}

			// here, the path is a partial path that may contain model mounts (but not including "/", dealt with above)
			//
			//	/
			//		foo
			//		weather/
			//			us/		(mount)
			//			europe/	(mount)
			//		scores/
			//			premiereleague/		(mount)
			//
			//

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

			// and also check the backing model
			final ModelPathList backing = fBackingModel.listChildrenOfPath ( context, prefix );
			for ( Path p : backing )
			{
				result.add ( p );
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

		// if the requested path is in a delegated model, just forward the request
		if ( mm.getModel () != this )
		{
			return mm.getModel ().load ( context, mm.getPathWithinModel ( objectPath ) );
		}

		// or if it's part of the backing model
		if ( !objectPath.isRootPath () && fBackingModel.exists ( context, objectPath ) )
		{
			return fBackingModel.load ( context, objectPath );
		}

		// here, the path is a partial path that may contain model mounts (including "/")
		//
		//	/
		//		foo
		//		weather/
		//			us/		(mount)
		//			europe/	(mount)
		//		scores/
		//			premiereleague/		(mount)
		//
		//

		final TreeSet<Path> result = new TreeSet<>();
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

		// if this is top-level, include the top-level paths in the backing model
		if ( objectPath.isRootPath () )
		{
			for ( Path p : fBackingModel.listChildrenOfPath ( context, objectPath ) )
			{
				result.add ( p );
			}
		}

		return CommonJsonDbObjectContainer.createObjectContainer ( objectPath.toString (), result );
	}

	@Override
	public ObjectUpdater createUpdate ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		final ModelMount mm = getModelForPath ( objectPath );
		if ( mm.getModel () == this )
		{
			return fBackingModel.createUpdate ( context, objectPath );
		}
		else
		{
			return mm.getModel ().createUpdate ( context, mm.getPathWithinModel ( objectPath ) );
		}
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		final ModelMount mm = getModelForPath ( objectPath );
		if ( mm.getModel () == this )
		{
			return fBackingModel.remove ( context, objectPath );
		}
		else
		{
			return mm.getModel ().remove ( context, mm.getPathWithinModel ( objectPath ) );
		}
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		// we don't know which model(s) to report this relation type to, so we just report to all of them

		// tell the backing model
		fBackingModel.setRelationType ( context, relnName, rt );

		// tell the mounted models
		for ( ModelMount mountEntry : fUserMountTable )
		{
			mountEntry.getModel ().setRelationType ( context, relnName, rt );
		}

		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		// for each relation, we check if both sides are in the same model. If so, let the model handle the relation. If not,
		// we store it in our backing model

		final Path from = mr.getFrom ();
		final ModelMount mmFrom = getModelForPath ( from );

		final Path to = mr.getTo ();
		final ModelMount mmTo = getModelForPath ( to );

		if ( mmFrom == mmTo )	// same instance
		{
			return mmFrom.getModel ().relate ( context, ModelRelation.from ( mmFrom.getPathWithinModel ( from ), mr.getName (), mmTo.getPathWithinModel ( to ) ) );
		}
		else
		{
			return fBackingModel.relate ( context, mr );
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
			return mmFrom.getModel ().unrelate ( context, ModelRelation.from ( mmFrom.getPathWithinModel ( from ), reln.getName (), mmTo.getPathWithinModel ( to ) ) );
		}
		else
		{
			return fBackingModel.unrelate ( context, reln );
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

		fBackingModel.unrelate ( context, relnId );

		return false;
	}

	private static final String kChild = "child";

	@Override
	public RelationSelector selectRelations ( Path objectPath )
	{
		return new BaseRelationSelector<DelegatingModel> ( this, objectPath )
		{
			@Override
			public ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException
			{
				final ModelRequestContext dc = getDerivedContext ( fBackingModel, context );
				
				final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

				// some setup
				final Path objectPath = getObject ();
				final ModelMount mm = getModelForPath ( objectPath );
				final String relnNameFilter = getNameFilter ();

				// if the requested path is in a delegated model, forward the request to that model, then convert the 
				// results to their global names
				if ( mm.getModel () != DelegatingModel.this )
				{
					final Path pathInModel = mm.getPathWithinModel ( objectPath );
					final ModelRelationList relnList = mm.getModel ().selectRelations ( pathInModel )
						.named ( relnNameFilter )
						.inbound ( wantInbound () )
						.outbound ( wantOutbound () )
						.getRelations ( getDerivedContext ( mm.getModel (), context ) )
					;
					final List<ModelRelationInstance> relns =  ModelItemList.iterateIntoList ( relnList );
					for ( ModelRelationInstance mri : relns )
					{
						result.add ( ModelRelationInstance.from (
							mm.getGlobalPath ( mri.getFrom () ),
							mri.getName (),
							mm.getGlobalPath ( mri.getTo () )
						) );
					}
				}

				// the object may also have relations in the delegating model
				result.addAll ( ModelItemList.iterateIntoList ( fBackingModel.selectRelations ( objectPath )
					.named ( relnNameFilter )
					.inbound ( wantInbound () )
					.outbound ( wantOutbound () )
					.getRelations ( dc )
				));

				// finally, we establish parent/child hierarchy via path names
				if ( wantInbound () && !objectPath.isRootPath () && ( relnNameFilter == null || relnNameFilter.equals ( kChild ) )  )
				{
					result.add ( ModelRelationInstance.from ( objectPath.getParentPath (), kChild, objectPath ) );
				}
				if ( wantOutbound () && ( relnNameFilter == null || relnNameFilter.equals ( kChild ) )  )
				{
					TreeSet<Path> children = new TreeSet<>();

					// find the paths below this one in the mount space
					for ( ModelMount mmm : fUserMountTable )
					{
						// mount /foo/bar; object path /foo, then we want /foo/bar
						// mount /foo/bar; object path /foo/bar, then we have to ask the model for /'s children
						// mount /foo/bar; object path /foo/bar/baz, then we have to ask the model for /bar's children
						
						if ( mmm.getMountPoint ().startsWith ( objectPath ) && !mmm.getMountPoint ().equals ( objectPath ) )
						{
							final Path pInP = mmm.getMountPoint ().makePathWithinParent ( objectPath );
							final Name childName = pInP.getSegment ( 0 );
							final Path asChild = objectPath.makeChildItem ( childName );
							children.add ( asChild );
						}
						else if ( objectPath.startsWith ( mmm.getMountPoint () ) )
						{
							for ( Path child : mmm.getModel ().listChildrenOfPath (
								getDerivedContext ( mmm.getModel (), context ),
								mmm.getPathWithinModel ( objectPath ) ) )
							{
								children.add ( mmm.getGlobalPath ( child ) );
							}
						}
						// else: no overlap
					}

					// also find any child objects in the backing store
					for ( Path p : fBackingModel.listChildrenOfPath ( dc, objectPath ) )
					{
						children.add ( p );
					}

					// build relations
					for ( Path child : children )
					{
						result.add ( ModelRelationInstance.from ( objectPath, kChild, child ) );
					}
				}

				return ModelRelationList.simpleListOfCollection ( result );
			}
		};
	}

	private final String fAcctId;
	private final String fModelId;
	private final LinkedList<ModelMount> fUserMountTable;
	private final Model fBackingModel;

	// get the model that owns the given path, which may be the top-level delegating model
	private ModelMount getModelForPath ( Path modelPath )
	{
		for ( ModelMount mountEntry : fUserMountTable )
		{
			if ( mountEntry.contains ( modelPath ) )
			{
				return mountEntry;
			}
		}

		// this path is in the top-level mount
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
			public Path getGlobalPath ( Path from ) { return from; }
		};
	}

	private static final Logger log = LoggerFactory.getLogger ( DelegatingModel.class );

	private ModelRequestContext getDerivedContext ( Model targetModel, ModelRequestContext mrc ) throws ModelRequestException
	{
		try
		{
			return targetModel.getRequestContextBuilder ()
				.forUser ( mrc.getOperator () )
				.build ()
			;
		}
		catch ( BuildFailure e )
		{
			throw new ModelRequestException ( e );
		}
	}
}
