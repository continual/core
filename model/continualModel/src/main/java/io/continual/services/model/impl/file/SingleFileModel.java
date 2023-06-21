package io.continual.services.model.impl.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
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
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.services.model.impl.json.CommonJsonDbObject;
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
		rebuildReversals ();
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ();
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
	public void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
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
	
			final TreeSet<String> tos = new TreeSet<> ( JsonVisitor.arrayToList ( relnNode ) );
			tos.add ( reln.getTo ().toString () );
			fromNode.put ( reln.getName (), JsonVisitor.collectionToArray ( tos ) );
	
			flush ();
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
	public List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path fromObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		
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
						result.add ( ModelRelation.from ( fromObject, relnName, Path.fromString ( toPathText ) ) );
						return true;
					}
				} );
				return true;
			}
		} );

		return result;
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path toObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();

		final MultiMap<String,Path> revRelns = fReversals.get ( toObject );
		if ( revRelns != null )
		{
			for ( Path fromObj : revRelns.get ( named ) )
			{
				result.add ( ModelRelation.from ( fromObj, named, toObject ) );
			}
		}

		return result;
	}

	@Override
	protected ModelObject loadObject ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		JSONObject current = getDataRoot ();
		for ( Name name : objectPath.getSegments () )
		{
			current = current.optJSONObject ( name.toString () );
			if ( current == null ) return null;
		}
		return new CommonJsonDbObject ( objectPath.toString (), current );
	}

	@Override
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelRequestException, ModelServiceException
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
					throw new ModelRequestException ( objectPath.toString () + " parent path unavailable at " + name.toString () );
				}
			}
			current.put ( objectPath.getItemName ().toString (), o.toJson () );

			flush ( );
		}
		catch ( ModelRequestException | ModelServiceException x )
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
	
		final TreeSet<String> tos = new TreeSet<> ( JsonVisitor.arrayToList ( relnNode ) );
		boolean result = tos.remove ( reln.getTo ().toString () );
		fromNode.put ( reln.getName (), JsonVisitor.collectionToArray ( tos ) );
	
		// also remove from reversals
		final MultiMap<String,Path> revReln = fReversals.get ( reln.getTo () );
		if ( revReln != null )
		{
			revReln.remove ( reln.getName (), reln.getFrom () );
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
		public ModelObjectList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObject> result = new LinkedList<> ();

			for ( Path p : collectObjectsUnder ( getPathPrefix () ) )
			{
				final ModelObject mo = load ( context, p );
				if ( mo != null )
				{
					boolean match = true;
					for ( SimpleModelQuery.Filter filter : getFilters () )
					{
						if ( !filter.matches ( mo ) )
						{
							match = false;
							break;
						}
					}

					if ( match )
					{
						result.add ( mo );
					}
				}
			}

			// now sort our list
			final ModelObjectComparator orderBy = getOrdering ();
			if ( orderBy != null )
			{
				Collections.sort ( result, new java.util.Comparator<ModelObject> ()
				{
					@Override
					public int compare ( ModelObject o1, ModelObject o2 )
					{
						return orderBy.compare ( o1, o2 );
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
			return new ModelObjectList ()
			{
				@Override
				public Iterator<ModelObject> iterator ()
				{
					return result.iterator ();
				}
			};
		}
	}
}
