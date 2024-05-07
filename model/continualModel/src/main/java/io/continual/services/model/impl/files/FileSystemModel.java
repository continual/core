/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.model.impl.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelPathListPage;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.PageRequest;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonDataTransfer;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.services.model.impl.json.CommonJsonDbObjectContainer;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class FileSystemModel extends CommonJsonDbModel
{
	public FileSystemModel ( String modelId, String baseDir ) throws BuildFailure
	{
		super ( modelId );

		fBaseDir = new File ( baseDir );
		if ( !fBaseDir.exists () && !fBaseDir.mkdir () )
		{
			throw new BuildFailure ( "Failed to create " + fBaseDir.toString () );
		}

		fRelnMgr = new FileSysRelnMgr ( new File ( fBaseDir, kRelnsDir ) );
	}

	public FileSystemModel ( String acctId, String modelId, File baseDir ) throws BuildFailure
	{
		this ( modelId, baseDir.getAbsolutePath () );
	}

	public FileSystemModel ( String acctId, String modelId, java.nio.file.Path path ) throws BuildFailure
	{
		this ( acctId, modelId, path.toFile () );
	}

	public FileSystemModel ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		this (
			sc.getExprEval ( config ).evaluateText ( config.getString ( "modelId" ) ),
			sc.getExprEval ( config ).evaluateText ( config.getString ( "baseDir" ) )
		);
	}

	@Override
	public long getMaxPathLength ()
	{
		// this is tough to determine... typical FAT16 seems to allow 32 levels of 255 chars. 
		return 32L * 255L;
	}

	@Override
	public long getMaxRelnNameLength ()
	{
		return 255L;
	}

	@Override
	public long getMaxSerializedObjectLength ()
	{
		// file systems differ in their limits. For simplicity, we just choose a number that the mainstream
		// systems we know of will support. If this doesn't work for someone, it's easy enough to override this
		// class.

		// FAT 16 is probably the most restrictive system in widespread use...
		return ( 4L * 1024L * 1024L * 1024L ) - 1L;
	}

	@Override
	public ModelPathListPage listChildrenOfPath ( ModelRequestContext context, Path prefix, PageRequest pr ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<Path> result = new LinkedList<> ();

		final File objDir = getObjectDir ();

		// drill down to the proper containing folder
		final File container = pathToDir ( objDir, prefix );

		if ( container.isFile () )
		{
			// this is an object; it has no children
			return ModelPathListPage.wrap ( new LinkedList<Path> (), pr );
		}

		// if the directory doesn't exist, 
		if ( !container.isDirectory () )
		{
			// if the obj dir hasn't been created...
			if ( container.equals ( objDir ) ) return ModelPathListPage.wrap ( new LinkedList<Path> (), pr );

			// otherwise, this is a path into nowhere... 
			throw new ModelItemDoesNotExistException ( prefix );
		}

		for ( File obj : container.listFiles () )
		{
			result.add ( prefix.makeChildItem ( Name.fromString ( obj.getName () ) ) );
		}

		return ModelPathListPage.wrap ( result, pr );
	}

	private class FsModelQuery extends SimpleModelQuery
	{
		private List<Path> collectObjectsUnder ( File dir, Path pathPrefix )
		{
			final LinkedList<Path> result = new LinkedList<> ();

			for ( File f : dir.listFiles () )
			{
				final String namePart = f.getName ();
				final Path p = pathPrefix.makeChildItem ( Name.fromString ( namePart ) );

				if ( f.isFile () )
				{
					result.add ( p );
				}
				else if ( f.isDirectory () )
				{
					result.addAll ( collectObjectsUnder ( f, p ) );
				}
			}

			return result;
		}
		
		@Override
		public <T,K> ModelObjectList<T> execute ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObjectAndPath<T>> result = new LinkedList<> ();

			final File objDir = getObjectDir ();
			final File container = pathToDir ( objDir, getPathPrefix() );
			if ( container.isDirectory () )
			{
				for ( Path p : collectObjectsUnder ( container, getPathPrefix () ) )
				{
					final T mo = load ( context, p, factory, userContext );
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
						return orderBy.compare ( accessor.getDataFrom ( o1.getObject () ), accessor.getDataFrom ( o2.getObject () ) );
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

	@Override
	public FsModelQuery startQuery ()
	{
		return new FsModelQuery ();
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		fRelnMgr.setRelationType ( relnName, rt );
		return this;
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.relate ( reln );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.unrelate ( reln );
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
	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getInboundRelationsNamed ( forObject, named );
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getOutboundRelationsNamed ( forObject, named );
	}

	private File getFileFor ( Path mop )
	{
		return new File ( getObjectDir(), mop.toString () );
	}

	private static final String kOldDataTag = "Ⓤ";
	
	protected CommonDataTransfer loadObject ( ModelRequestContext context, final Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		final File obj = getFileFor ( objectPath );
		if ( obj.isFile () )
		{
			final JSONObject rawData;
			try ( final FileInputStream fis = new FileInputStream ( obj ) )
			{
				 rawData = new JSONObject ( new CommentedJsonTokener ( fis ) );
			}
			catch ( JSONException x )
			{
				throw new ModelRequestException ( "The object data is corrupt." );
			}
			catch ( IOException x )
			{
				throw new ModelServiceException ( x );
			}
			
			// an older version of this system put data into a field called "Ⓤ"
			final JSONObject inner = rawData.optJSONObject ( kOldDataTag );
			final boolean oldModel = ( inner != null );
			if ( oldModel )
			{
				rawData.remove ( kOldDataTag );
				rawData.put ( CommonDataTransfer.kDataTag, inner );
			}

			final CommonDataTransfer loadedObj = new CommonDataTransfer ( objectPath, rawData );
			if ( oldModel )
			{
				final AccessControlList acl = loadedObj.getMetadata ().getAccessControlList ();
				if ( acl.getEntries ().size () == 0 )
				{
					acl
						.setOwner ( "_updated_" )
						.permit ( AccessControlEntry.kAnyUser, ModelOperation.kAllOperationStrings )
					;
				}
			}
			return loadedObj;
		}
		else if ( obj.isDirectory () )
		{
			final LinkedList<Path> result = new LinkedList<>();
			for ( String child : obj.list () )
			{
				result.add ( Path.getRootPath ().makeChildItem ( Name.fromString ( child ) ) );
			}
			return CommonJsonDbObjectContainer.createObjectContainer ( objectPath, result );
		}
		else if ( objectPath.isRootPath () )
		{
			// this is a special case because the object dir may not be created in a new model
			return CommonJsonDbObjectContainer.createObjectContainer ( objectPath, new LinkedList<Path> () );
		}
		else if ( !obj.exists () )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}
		else
		{
			throw new ModelServiceException ( "Path is corrupt: " + objectPath.toString () );
		}
	}

	@Override
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelDataTransfer o ) throws ModelRequestException, ModelServiceException
	{
		final File obj = getFileFor ( objectPath );
		if ( obj.exists () && !obj.isFile () )
		{
			throw new ModelRequestException ( objectPath.toString () + " exists as a container." );
		}

		final File parentDir = obj.getParentFile ();
		if ( parentDir.exists () && !parentDir.isDirectory () )
		{
			throw new ModelRequestException ( "Parent " + objectPath.getParentPath ().toString () + " is an object." );
		}
		
		if ( !parentDir.exists () && !parentDir.mkdirs () )
		{
			throw new ModelRequestException ( objectPath.toString () + " parent path unavailable." );
		}

		try ( final FileOutputStream fos = new FileOutputStream ( obj ) )
		{
			fos.write ( CommonDataTransfer.toDataObject ( o.getMetadata (), o.getObjectData () ).toString ().getBytes ( kUtf8 ) );
		}
		catch ( IOException x )
		{
			throw new ModelServiceException ( x );
		}
	}


	@Override
	protected boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		final File obj = getFileFor ( objectPath );
		if ( !obj.exists () ) return false;

		if ( obj.exists () && !obj.isFile () )
		{
			throw new ModelRequestException ( objectPath.toString () + " exists as a container." );
		}

		fRelnMgr.removeAllRelations ( objectPath );
		final boolean removed = obj.delete ();
		log.info ( "Removed object {} file {}", objectPath, obj );

		removeEmptyParents ( obj );

		return removed;
	}

	static void removeEmptyDirsUpTo ( File from, File limit )
	{
		final File parentDir = from.getParentFile ();
		if ( parentDir.exists () && parentDir.isDirectory () && !parentDir.equals ( limit ) && parentDir.list ().length == 0 )
		{
			log.info ( "Removing empty dir {}", parentDir );
			parentDir.delete ();
			removeEmptyDirsUpTo ( parentDir, limit );
		}
	}
	
	private void removeEmptyParents ( File from )
	{
		removeEmptyDirsUpTo ( from, getObjectDir () );
	}

	private final File fBaseDir;
	private final FileSysRelnMgr fRelnMgr;
	private static final Logger log = LoggerFactory.getLogger ( FileSystemModel.class );

	private File getObjectDir ()
	{
		return new File ( fBaseDir, "objects" );
	}

	private static final String kRelnsDir = "relations";
	
//	private File getSchemaDir ()
//	{
//		return new File ( new File ( fBaseDir, getAcctId () ), "schemas" );
//	}

	private File pathToDir ( File base, Path p )
	{
		if ( p.isRootPath () ) return base;
		return new File ( pathToDir ( base, p.getParentPath () ), p.getItemName ().toString () );
	}

	private static final Charset kUtf8 = Charset.forName ( "UTF8" );
}
