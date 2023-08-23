package io.continual.services.model.impl.mem;

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
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRelnInstance;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.services.model.impl.json.CommonJsonDbObject;
import io.continual.util.collections.MultiMap;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class InMemoryModel extends CommonJsonDbModel
{
	public InMemoryModel ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		this (
			sc.getExprEval ( config ).evaluateText ( config.getString ( "acctId" ) ),
			sc.getExprEval ( config ).evaluateText ( config.getString ( "modelId" ) )
		);
	}

	public InMemoryModel ( String acctId, String modelId ) throws BuildFailure
	{
		super ( acctId, modelId );

		fRoot = new JSONObject ();
		fRoot.put ( kObjectsNode, new JSONObject () );
		fRoot.put ( kRelnsNode, new JSONObject () );

		fReversals = new HashMap<Path,MultiMap<String,Path>> ();
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
	
			final TreeSet<String> tos = new TreeSet<> ( JsonVisitor.arrayToList ( relnNode ) );
			tos.add ( reln.getTo ().toString () );
			fromNode.put ( reln.getName (), JsonVisitor.collectionToArray ( tos ) );

			return new BasicModelRelnInstance ( reln );
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
			return removeReln ( reln );
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
			final BasicModelRelnInstance mr = BasicModelRelnInstance.fromId ( relnId );
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
						result.add ( new BasicModelRelnInstance ( fromObject, relnName, Path.fromString ( toPathText ) ) );
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
				result.add ( new BasicModelRelnInstance ( fromObj, named, toObject ) );
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
			if ( current == null ) throw new ModelItemDoesNotExistException ( objectPath );
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
		}
		catch ( ModelRequestException x )
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

				return true;
			}
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
