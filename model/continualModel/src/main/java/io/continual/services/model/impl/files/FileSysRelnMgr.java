package io.continual.services.model.impl.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRelnInstance;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.data.json.JsonVisitor.ValueReader;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * A simple relation manager for the file model.
 */
class FileSysRelnMgr
{
	public FileSysRelnMgr ( File relnDir ) throws BuildFailure
	{
		fRelnDir = relnDir;
		if ( !fRelnDir.exists () && !fRelnDir.mkdir () )
		{
			throw new BuildFailure ( "Failed to create " + relnDir.toString () );
		}

		if ( !fRelnDir.isDirectory () )
		{
			throw new BuildFailure ( relnDir.toString () + " exists and is not a directory." );
		}
	}

	public ModelRelationInstance relate ( ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		// not loving having an non-atomic write here, but we're going for simple, not production strength.
		addToRelnFile ( pathToObjOutDir ( mr.getFrom () ), mr.getName (), mr.getTo () );
		addToRelnFile ( pathToObjInDir ( mr.getTo () ), mr.getName (), mr.getFrom () );
		return new BasicModelRelnInstance ( mr );
	}

	public boolean unrelate ( ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final File outRelnDir = new File ( pathToObjOutDir ( reln.getFrom () ), reln.getName () );
		final File inRelnDir = new File ( pathToObjInDir ( reln.getTo () ), reln.getName () );

		final boolean out = removeFromRelnFile ( outRelnDir, reln.getTo () );
		final boolean in = removeFromRelnFile ( inRelnDir, reln.getFrom () );

		FileSystemModel.removeEmptyDirsUpTo ( outRelnDir, fRelnDir );
		FileSystemModel.removeEmptyDirsUpTo ( inRelnDir, fRelnDir );

		return out || in;
	}

	public void removeAllRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> relns = new LinkedList<> ();
		relns.addAll ( getInboundRelations ( forObject ) );
		relns.addAll ( getOutboundRelations ( forObject ) );
		for ( ModelRelation mr : relns )
		{
			unrelate ( mr );
		}
	}
	
	public List<ModelRelationInstance> getInboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final File objDir = pathToObjInDir ( forObject );
		if ( objDir.isDirectory () )
		{
			for ( File reln : objDir.listFiles () )
			{
				result.addAll ( getRelationsFrom ( forObject, reln, false ) );
			}
		}

		return result;
	}

	public List<ModelRelationInstance> getOutboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final File objDir = pathToObjOutDir ( forObject );
		if ( objDir.isDirectory () )
		{
			for ( File reln : objDir.listFiles () )
			{
				result.addAll ( getRelationsFrom ( forObject, reln, true ) );
			}
		}

		return result;
	}

	public List<ModelRelationInstance> getInboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( named == null ) return getInboundRelations ( forObject );

		final File reln = new File ( pathToObjInDir ( forObject ), named );
		if ( reln.exists () )
		{
			return getRelationsFrom ( forObject, reln, false );
		}
		return new LinkedList<ModelRelationInstance> ();
	}

	public List<ModelRelationInstance> getOutboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		if ( named == null ) return getOutboundRelations ( forObject );

		final File reln = new File ( pathToObjOutDir ( forObject ), named );
		if ( reln.exists () )
		{
			return getRelationsFrom ( forObject, reln, true );
		}
		return new LinkedList<ModelRelationInstance> ();
	}

	private final File fRelnDir;

	private List<ModelRelationInstance> getRelationsFrom ( Path forObject, File reln, boolean objIsFromSide ) throws ModelServiceException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final String relnName = reln.getName ();
		for ( Path to : loadToSet ( reln ) )
		{
			result.add ( new BasicModelRelnInstance ( new ModelRelation ()
			{
				@Override
				public Path getFrom () { return objIsFromSide ? forObject : to; }

				@Override
				public Path getTo () { return objIsFromSide ? to : forObject; }

				@Override
				public String getName () { return relnName; }
			} ) );
		}

		return result;
	}
	
	private Set<Path> loadToSet ( File relnFile ) throws ModelServiceException
	{
		if ( relnFile.exists () )
		{
			try ( FileInputStream fis = new FileInputStream ( relnFile ) )
			{
				final JSONArray ar = new JSONArray ( new CommentedJsonTokener ( fis ) );
				final List<Path> list = JsonVisitor.arrayToList ( ar, new ValueReader<String,Path> ()
				{
					@Override
					public Path read ( String val )
					{
						return Path.fromString ( val );
					}
				} );

				final TreeSet<Path> result = new TreeSet<> ();
				result.addAll ( list );
				return result;
			}
			catch ( FileNotFoundException x )
			{
				// ignore
			}
			catch ( JSONException | IOException x )
			{
				throw new ModelServiceException ( x );
			}
		}

		return new TreeSet<Path> ();
	}
	
	private void storeToFile ( File relnFile, Set<Path> list ) throws ModelServiceException
	{
		if ( list.size () > 0 )
		{
			final JSONArray arr = JsonVisitor.listToArray ( list, new ItemRenderer<Path,String> ()
			{
				@Override
				public String render ( Path t )
				{
					return t.toString ();
				}
			} );
	
			try ( FileWriter fw = new FileWriter ( relnFile ) )
			{
				fw.write ( arr.toString () );
			}
			catch ( IOException x )
			{
				throw new ModelServiceException ( x );
			}
		}
		else
		{
			// empty set; just remove the file
			relnFile.delete ();
		}
	}

	private File pathToObjDir ( Path from )
	{
		File result = fRelnDir;
		for ( Name component : from.getSegmentList () )
		{
			result = new File ( result, component.toString () );
		}
		return result;
	}

	private File pathToObjOutDir ( Path from )
	{
		return new File ( pathToObjDir ( from ), "out" );
	}

	private File pathToObjInDir ( Path from )
	{
		return new File ( pathToObjDir ( from ), "in" );
	}

	private void addToRelnFile ( File targetDir, String relnName, Path farSide ) throws ModelServiceException
	{
		if ( !targetDir.isDirectory () && !targetDir.mkdirs () )
		{
			throw new ModelServiceException ( "Couldn't create relation directory " + targetDir.toString () );
		}

		final File pathFile = new File ( targetDir, relnName );
		final Set<Path> farSideList = loadToSet ( pathFile );
		farSideList.add ( farSide );
		storeToFile ( pathFile, farSideList );
	}

	private boolean removeFromRelnFile ( File relnFile, Path farSide ) throws ModelServiceException
	{
		if ( relnFile.exists () )
		{
			final Set<Path> toList = loadToSet ( relnFile );
			if ( toList.contains ( farSide  ) )
			{
				toList.remove ( farSide );
				storeToFile ( relnFile, toList );
				return true;
			}
		}
		return false;
	}
}
