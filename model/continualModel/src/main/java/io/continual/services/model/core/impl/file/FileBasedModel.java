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

package io.continual.services.model.core.impl.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectFilter;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.core.impl.commonJsonDb.BasicModelRequestContext;
import io.continual.services.model.core.impl.commonJsonDb.CommonJsonDbModel;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class FileBasedModel extends CommonJsonDbModel
{
	public FileBasedModel ( String acctId, String modelId, File baseDir )
	{
		super ( acctId, modelId );

		fBaseDir = baseDir;
	}

	public FileBasedModel ( ServiceContainer sc, JSONObject config )
	{
		super ( sc, config );

		fBaseDir = new File ( config.getString ( "baseDir" ) );
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new ModelRequestContextBuilder ()
		{
			@Override
			public ModelRequestContextBuilder forUser ( Identity user )
			{
				fUser = user;
				return this;
			}

			@Override
			public ModelRequestContext build ()
			{
				return new BasicModelRequestContext ( fUser );
			}

			private Identity fUser = null;
		};
	}

	@Override
	public List<Path> listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<Path> result = new LinkedList<> ();

		final File objDir = getObjectDir ();
		
		// drill down to the proper containing folder
		final File container = pathToDir ( objDir, prefix );
		if ( container.isDirectory () )
		{
			for ( File obj : container.listFiles () )
			{
				if ( obj.isFile () )
				{
					result.add ( prefix.makeChildItem ( Name.fromString ( obj.getName () ) ) );
				}
			}
		}

		return result;
	}

	@Override
	public List<ModelObject> queryModelForObjects ( ModelRequestContext context, Path prefix, ModelObjectComparator orderBy, ModelObjectFilter ... filters )
		throws ModelServiceIoException, ModelServiceRequestException
	{
		final LinkedList<ModelObject> result = new LinkedList<> ();

		final File objDir = getObjectDir ();
		final File container = pathToDir ( objDir, prefix );
		if ( container.isDirectory () )
		{
			for ( File obj : container.listFiles () )
			{
				if ( obj.isFile () )
				{
					final Path p = prefix.makeChildItem ( Name.fromString ( obj.getName () ) );
					final ModelObject mo = load ( context, p );
					if ( mo != null )
					{
						boolean match = true;
						for ( ModelObjectFilter filter : filters )
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
		}

		// now sort our list
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

		return result;
	}

	private File getFileFor ( ModelObjectPath mop )
	{
		return new File ( getObjectDir(), mop.getObjectPath().toString () );
	}

	protected boolean objectExists ( ModelRequestContext context, ModelObjectPath objectPath )
	{
		return getFileFor ( objectPath ).exists ();
	}

	protected ModelObject loadObject ( ModelRequestContext context, final ModelObjectPath objectPath ) throws ModelServiceIoException, ModelServiceRequestException
	{
		final File obj = getFileFor ( objectPath );
		if ( !obj.exists () || !obj.isFile () )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}

		final JSONObject rawData;
		try ( final FileInputStream fis = new FileInputStream ( obj ) )
		{
			 rawData = new JSONObject ( new CommentedJsonTokener ( fis ) );
		}
		catch ( JSONException x )
		{
			throw new ModelServiceRequestException ( "The object data is corrupt." );
		}
		catch ( IOException x )
		{
			throw new ModelServiceIoException ( x );
		}

		return new FileModelObject ( objectPath.toString (), rawData );
	}

	@Override
	protected void internalStore ( ModelRequestContext context, ModelObjectPath objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException
	{
		final File obj = getFileFor ( objectPath );
		if ( obj.exists () && !obj.isFile () )
		{
			throw new ModelServiceRequestException ( objectPath.toString () + " exists as a container." );
		}

		final JSONObject toWrite = new JSONObject ()
			.put ( kUserDataTag, new JSONObject ( new CommentedJsonTokener ( o.asJson () ) ) )
		;

		if ( !obj.getParentFile ().mkdirs () && !obj.getParentFile ().isDirectory () )
		{
			throw new ModelServiceRequestException ( objectPath.toString () + " parent path unavailable." );
		}
		try ( final FileOutputStream fos = new FileOutputStream ( obj ) )
		{
			fos.write ( toWrite.toString ().getBytes () );
		}
		catch ( IOException x )
		{
			throw new ModelServiceIoException ( x );
		}
	}


	private final File fBaseDir;

	private File getObjectDir ()
	{
		return new File ( new File ( fBaseDir, getAcctId () ), "objects" );
	}

//	private File getSchemaDir ()
//	{
//		return new File ( new File ( fBaseDir, getAcctId () ), "schemas" );
//	}

	private File pathToDir ( File base, Path p )
	{
		if ( p.isRootPath () ) return base;
		return new File ( pathToDir ( base, p.getParentPath () ), p.getItemName ().toString () );
	}
}
