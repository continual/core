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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelOperation;
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
import io.continual.services.model.impl.json.CommonJsonDbObjectContainer;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class FileSystemModel extends CommonJsonDbModel
{
	public FileSystemModel ( String acctId, String modelId, String baseDir ) throws BuildFailure
	{
		super ( acctId, modelId );

		fBaseDir = new File ( baseDir );
		if ( !fBaseDir.exists () && !fBaseDir.mkdir () )
		{
			throw new BuildFailure ( "Failed to create " + fBaseDir.toString () );
		}

		fRelnMgr = new FileSysRelnMgr ( new File ( fBaseDir, kRelnsDir ) );
	}

	public FileSystemModel ( String acctId, String modelId, File baseDir ) throws BuildFailure
	{
		this ( acctId, modelId, baseDir.getAbsolutePath () );
	}

	public FileSystemModel ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		this (
			sc.getExprEval ( config ).evaluateText ( config.getString ( "acctId" ) ),
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
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ();
	}

	@Override
	public ModelPathList listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<Path> result = new LinkedList<> ();

		final File objDir = getObjectDir ();
		
		// drill down to the proper containing folder
		final File container = pathToDir ( objDir, prefix );
		if ( !container.isDirectory () )
		{
			// if the obj dir hasn't been created...
			if ( container.equals ( objDir ) ) return ModelPathList.wrap ( new LinkedList<Path> () );

			// but normally...
			return null;
		}
		
		for ( File obj : container.listFiles () )
		{
			if ( obj.isFile () )
			{
				result.add ( prefix.makeChildItem ( Name.fromString ( obj.getName () ) ) );
			}
		}

		return new ModelPathList ()
		{
			@Override
			public Iterator<Path> iterator ()
			{
				return result.iterator ();
			}
		};
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
		public ModelObjectList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObject> result = new LinkedList<> ();

			final File objDir = getObjectDir ();
			final File container = pathToDir ( objDir, getPathPrefix() );
			if ( container.isDirectory () )
			{
				for ( Path p : collectObjectsUnder ( container, getPathPrefix () ) )
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
	
	@Override
	public FsModelQuery startQuery ()
	{
		return new FsModelQuery ();
	}

	@Override
	public void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		fRelnMgr.relate ( reln );
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.unrelate ( reln );
	}

	@Override
	public List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getInboundRelations ( forObject );
	}

	@Override
	public List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getOutboundRelations ( forObject );
	}

	@Override
	public List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getInboundRelationsNamed ( forObject, named );
	}

	@Override
	public List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return fRelnMgr.getOutboundRelationsNamed ( forObject, named );
	}

	private File getFileFor ( Path mop )
	{
		return new File ( getObjectDir(), mop.toString () );
	}

	private static final String kOldDataTag = "Ⓤ";
	
	protected ModelObject loadObject ( ModelRequestContext context, final Path objectPath ) throws ModelServiceException, ModelRequestException
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
				rawData.put ( "data", inner );
			}

			final CommonJsonDbObject loadedObj = new CommonJsonDbObject ( objectPath.toString (), rawData );
			if ( oldModel )
			{
				final AccessControlList acl = loadedObj.getAccessControlList ();
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
			return CommonJsonDbObjectContainer.createObjectContainer ( objectPath.toString (), result );
		}
		else if ( objectPath.isRootPath () )
		{
			// this is a special case because the object dir may not be created in a new model
			return CommonJsonDbObjectContainer.createObjectContainer ( objectPath.toString (), new LinkedList<Path> () );
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
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelRequestException, ModelServiceException
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
			fos.write ( o.toJson ().toString ().getBytes ( kUtf8 ) );
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
		return obj.delete ();
	}

	private final File fBaseDir;
	private final FileSysRelnMgr fRelnMgr;

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
