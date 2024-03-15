package io.continual.services.model.impl.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonDataTransfer;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.util.collections.MultiMap;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * A single file model in JSON
 * @author peter
 *
 */
public class SingleFileModel extends CommonJsonDbModel
{
	public SingleFileModel ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		this (
			sc.getExprEval ( config ).evaluateText ( config.getString ( "acctId" ) ),
			sc.getExprEval ( config ).evaluateText ( config.getString ( "modelId" ) ),
			new File ( sc.getExprEval ( config ).evaluateText ( config.getString ( "file" ) ) )
		);
	}

	public SingleFileModel ( String acctId, String modelId, File f ) throws BuildFailure
	{
		super ( acctId, modelId );

		fFile = f;
		if ( !fFile.exists () || fFile.length () == 0L )
		{
			fRoot = new JSONObject ();
		}
		else
		{
			try ( FileInputStream fis = new FileInputStream ( f ) )
			{
				fRoot = new JSONObject ( new CommentedJsonTokener ( fis ) );
			}
			catch ( IOException x )
			{
				throw new BuildFailure ( x );
			}
		}

		if ( null == fRoot.optJSONObject ( kObjectsNode ) )
		{
			fRoot.put ( kObjectsNode, new JSONObject () );
		}
		if ( null == fRoot.optJSONArray ( kRelnsNode ) )
		{
			fRoot.put ( kRelnsNode, new JSONObject () );
		}

		fReversals = new HashMap<Path,MultiMap<String,Path>> ();

		rebuildReversals ();
	}

	@Override
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path parentPath ) throws ModelServiceException, ModelRequestException
	{
		JSONObject current = getDataRoot ();
		for ( Name name : parentPath.getSegments () )
		{
			current = current.optJSONObject ( name.toString () );
			if ( current == null ) return null;
		}

		final LinkedList<Path> paths = new LinkedList<> ();
		for ( String key : current.keySet () )
		{
			if ( null != current.optJSONObject ( key ) )
			{
				paths.add ( parentPath.makeChildItem ( Name.fromString ( key ) ) );
			}
		}

		return ModelPathList.wrap ( paths );
	}

	@Override
	public ModelQuery startQuery () throws ModelRequestException
	{
		return new ModelQuery ();
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		// this implementation orders all relations
		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		// make a backup...
		final JSONObject rootCopy = JsonUtil.clone ( fRoot );
		try
		{
			final JSONObject relns = getRelnRoot ();

			JSONObject fromNode = relns.optJSONObject ( reln.getFrom ().toString () );
			if ( fromNode == null )
			{
				fromNode = new JSONObject ();
				relns.put ( reln.getFrom ().toString (), fromNode );
			}
	
			JSONArray relnNode = fromNode.optJSONArray ( reln.getName () );
			if ( relnNode == null )
			{
				relnNode = new JSONArray ();
				fromNode.put ( reln.getName (), relnNode );
			}
	
			final String target = reln.getTo ().toString ();
			final List<String> toList = JsonVisitor.arrayToList ( relnNode );
			final TreeSet<String> toSet = new TreeSet<> ( toList );
			if ( ! toSet.contains ( target ) )
			{
				toList.add ( target );
				fromNode.put ( reln.getName (), JsonVisitor.collectionToArray ( toList ) );

				flush ();
			}

			return ModelRelationInstance.from ( reln );
		}
		catch ( ModelServiceException x )
		{
			// restore...
			fRoot = rootCopy;
			throw x;
		}
		catch ( Exception x )
		{
			// restore...
			fRoot = rootCopy;
			throw new ModelServiceException ( x );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		// make a backup...
		final JSONObject rootCopy = JsonUtil.clone ( fRoot );
		try
		{
			final boolean result = removeReln ( reln );
			flush ();
			return result;
		}
		catch ( ModelServiceException x )
		{
			// restore...
			fRoot = rootCopy;
			throw x;
		}
		catch ( Exception x )
		{
			// restore...
			fRoot = rootCopy;
			throw new ModelServiceException ( x );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		try
		{
			final ModelRelationInstance mr = ModelRelationInstance.from ( relnId );
			return unrelate ( context, mr );
		}
		catch ( IllegalArgumentException x )
		{
			throw new ModelRequestException ( x );
		}
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path fromObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();
		
		JsonVisitor.forEachElement ( getRelnRoot ().optJSONObject ( fromObject.toString () ), new ObjectVisitor<JSONArray,JSONException> ()
		{
			@Override
			public boolean visit ( String relnName, JSONArray toList ) throws JSONException
			{
				JsonVisitor.forEachElement ( toList, new ArrayVisitor<String,JSONException> ()
				{
					@Override
					public boolean visit ( String toPathText ) throws JSONException
					{
						result.add ( ModelRelationInstance.from ( fromObject, relnName, Path.fromString ( toPathText ) ) );
						return true;
					}
				} );
				return true;
			}
		} );

		return result;
	}

	@Override
	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path toObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final MultiMap<String,Path> revRelns = fReversals.get ( toObject );
		if ( revRelns != null )
		{
			for ( Path fromObj : revRelns.get ( named ) )
			{
				result.add ( ModelRelationInstance.from ( fromObj, named, toObject ) );
			}
		}

		return result;
	}

	@Override
	protected ModelDataTransfer loadObject ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		JSONObject current = getDataRoot ();
		for ( Name name : objectPath.getSegments () )
		{
			current = current.optJSONObject ( name.toString () );
			if ( current == null ) throw new ModelItemDoesNotExistException ( objectPath );
		}
		return new CommonDataTransfer ( objectPath, current );
	}

	@Override
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelDataTransfer o ) throws ModelRequestException, ModelServiceException
	{
		// make a backup...
		final JSONObject rootCopy = JsonUtil.clone ( fRoot );
		try
		{
			JSONObject current = getDataRoot ();
			for ( Name name : objectPath.getParentPath ().getSegments () )
			{
				JSONObject next = current.optJSONObject ( name.toString () );
				if ( next == null )
				{
					next = new JSONObject ();
					current.put ( name.toString (), next );
				}
				current = next;
			}
			current.put ( objectPath.getItemName ().toString (), CommonDataTransfer.toDataObject ( o.getMetadata (), o.getObjectData () ) );

			flush ( );
		}
		catch ( ModelServiceException x )
		{
			// restore...
			fRoot = rootCopy;
			throw x;
		}
		catch ( Exception x )
		{
			// restore...
			fRoot = rootCopy;
			throw new ModelServiceException ( x );
		}
	}

	@Override
	protected boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		// make a backup...
		final JSONObject rootCopy = JsonUtil.clone ( fRoot );
		try
		{
			JSONObject current = getDataRoot ();
			for ( Name name : objectPath.getParentPath ().getSegments () )
			{
				current = current.optJSONObject ( name.toString () );
				if ( current == null )
				{
					return false;
				}
			}

			final String itemName = objectPath.getItemName ().toString ();
			if ( current.has ( itemName ) && null != current.optJSONObject ( itemName ) )
			{
				current.remove ( itemName );

				removeRelnsFor ( objectPath );

				flush ();

				return true;
			}
		}
		catch ( ModelServiceException x )
		{
			// restore...
			fRoot = rootCopy;
			throw x;
		}
		catch ( Exception x )
		{
			// restore...
			fRoot = rootCopy;
			throw new ModelServiceException ( x );
		}
		return false;
	}

	private void removeRelnsFor ( Path objectPath )
	{
		// remove relations on the "from" side
		getRelnRoot().remove ( objectPath.toString () );

		// lookup relation on "to" side
		final MultiMap<String,Path> revRelns = fReversals.get ( objectPath );
		if ( revRelns != null )
		{
			for ( Map.Entry<String,List<Path>> entry : revRelns.getValues ().entrySet () )
			{
				for ( Path from : entry.getValue () )
				{
					removeReln ( ModelRelation.from ( from, entry.getKey (), objectPath ) );
				}
			}
		}
	}

	private boolean removeReln ( ModelRelation reln )
	{
		final JSONObject relns = getRelnRoot ();
	
		JSONObject fromNode = relns.optJSONObject ( reln.getFrom ().toString () );
		if ( fromNode == null )
		{
			return false;
		}
	
		JSONArray relnNode = fromNode.optJSONArray ( reln.getName () );
		if ( relnNode == null )
		{
			return false;
		}

		boolean result = false;

		final String target = reln.getTo ().toString ();
		final List<String> toList = JsonVisitor.arrayToList ( relnNode );
		final TreeSet<String> tos = new TreeSet<> ( toList );
		if ( tos.contains ( target ) )
		{
			result = toList.remove ( target );	// must return true
			fromNode.put ( reln.getName (), JsonVisitor.collectionToArray ( tos ) );

			// also remove from reversals
			final MultiMap<String,Path> revReln = fReversals.get ( reln.getTo () );
			if ( revReln != null )
			{
				revReln.remove ( reln.getName (), reln.getFrom () );
			}
		}

		return result;
	}
	
	/*
		{
		    "relations":
		    {
		        "/foo/bar/baz":
		        {
		            "friendlyTo": [ "/foo/bar/mop" ]
		        }
		    },
		    "objects":
		    {
		        "foo":
		        {
		            "bar":
		            {
		                "baz":
		                {
		                    "color": "rose",
		                    "count": 123
		                },
	
		                "mop":
		                {
		                    "color": "brown",
		                    "count": 456
		                }
		            }
		        }
		    }
		}
	*/

	private final File fFile;
	private JSONObject fRoot;
	private HashMap<Path,MultiMap<String,Path>> fReversals;

	private static final String kObjectsNode = "objects";
	private static final String kRelnsNode = "relations";

	private JSONObject getDataRoot ()
	{
		return fRoot.getJSONObject ( kObjectsNode );
	}

	private JSONObject getRelnRoot ()
	{
		return fRoot.getJSONObject ( kRelnsNode );
	}

	private void rebuildReversals ()
	{
		fReversals.clear ();

		JsonVisitor.forEachElement ( getRelnRoot(), new ObjectVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( String fromPathStr, JSONObject relnData ) throws JSONException
			{
				final Path fromPath = Path.fromString ( fromPathStr );
				JsonVisitor.forEachElement ( relnData, new ObjectVisitor<JSONArray,JSONException> ()
				{
					@Override
					public boolean visit ( String relnName, JSONArray toPathList ) throws JSONException
					{
						JsonVisitor.forEachElement ( toPathList, new ArrayVisitor<String,JSONException> ()
						{
							@Override
							public boolean visit ( String pathText ) throws JSONException
							{
								MultiMap<String,Path> mm = fReversals.get ( fromPath );
								if ( mm == null )
								{
									mm = new MultiMap<> ();
									fReversals.put ( fromPath, mm );
								}
								mm.put ( relnName, Path.fromString ( pathText ) );
								return true;
							}
							
						} );
						return true;
					}
				} );

				return true;
			}
		} );
	}
	
	private void flush () throws ModelServiceException
	{
		try ( FileWriter fw = new FileWriter ( fFile ) )
		{
			fw.write ( fRoot.toString ( 4 ) );
		}
		catch ( IOException x )
		{
			throw new ModelServiceException ( x );
		}
	}

	private class ModelQuery extends SimpleModelQuery
	{
		private List<Path> collectObjectsUnder ( Path pathPrefix )
		{
			final LinkedList<Path> result = new LinkedList<> ();

			JSONObject current = getDataRoot ();
			for ( Name name : pathPrefix.getSegments () )
			{
				current = current.optJSONObject ( name.toString () );
				if ( current == null ) return result;
			}

			for ( String key : current.keySet () )
			{
				if ( null != current.optJSONObject ( key ) )
				{
					final Path pathHere = pathPrefix.makeChildItem ( Name.fromString ( key ) );
					result.add ( pathHere );
					result.addAll ( collectObjectsUnder ( pathHere ) );
				}
			}

			return result;
		}
		
		@Override
		public <T> ModelObjectList<T> execute ( ModelRequestContext context, ModelObjectFactory<T> factory, DataAccessor<T> accessor ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObjectAndPath<T>> result = new LinkedList<> ();

			for ( Path p : collectObjectsUnder ( getPathPrefix () ) )
			{
				final T mo = load ( context, p, factory );
				if ( mo != null )
				{
					boolean match = true;
					for ( SimpleModelQuery.Filter filter : getFilters () )
					{
						if ( !filter.matches ( accessor.getDataFrom ( mo ) ) )
						{
							match = false;
							break;
						}
					}

					if ( match )
					{
						result.add ( ModelObjectAndPath.from ( p, mo ) );
					}
				}
			}

			// now sort our list
			final Comparator<ModelObject> orderBy = getOrdering ();
			if ( orderBy != null )
			{
				Collections.sort ( result, new java.util.Comparator<ModelObjectAndPath<T>> ()
				{
					@Override
					public int compare ( ModelObjectAndPath<T> o1, ModelObjectAndPath<T> o2 )
					{
						return orderBy.compare (
							accessor.getDataFrom ( o1.getObject () ),
							accessor.getDataFrom ( o2.getObject () )
						);
					}
				} );
			}

			// just remove from both ends of our list
			final long startIndex = (long)getPageSize() * (long)getPageNumber();
			long toDump = startIndex;
			while ( toDump > 0L && result.size () > 0 )
			{
				result.removeFirst ();
			}
			while ( result.size () > getPageSize() )
			{
				result.removeLast ();
			}
			
			// wrap our result
			return new ModelObjectList<T> ()
			{
				@Override
				public Iterator<ModelObjectAndPath<T>> iterator ()
				{
					return result.iterator ();
				}
			};
		}
	}
}
