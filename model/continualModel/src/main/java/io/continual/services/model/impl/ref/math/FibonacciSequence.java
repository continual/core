package io.continual.services.model.impl.ref.math;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BaseRelationSelector;
import io.continual.services.model.impl.common.ReadOnlyModel;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class FibonacciSequence extends ReadOnlyModel
{
	@Override
	public void close () {}

	@Override
	public String getAcctId () { return "built-in"; }

	@Override
	public String getId () { return "FibonacciSequence"; }

	@Override
	public long getMaxPathLength () { return 100; }

	@Override
	public long getMaxRelnNameLength () { return 10; }

	@Override
	public long getMaxSerializedObjectLength () { return 1024; }

	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) 
	{
		try
		{
			return objectPath.isRootPath () || isFib ( getNumberFrom ( objectPath ) );
		}
		catch ( NumberFormatException x )
		{
			return false;
		}
	}

	@Override
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path parentPath )
	{
		if ( !parentPath.isRootPath () )
		{
			return ModelPathList.wrap ( new LinkedList<Path> () );
		}
		return new ModelPathList ()
		{
			@Override
			public Iterator<Path> iterator ()
			{
				return new Iterator<Path> ()
				{
					@Override
					public boolean hasNext ()
					{
						return false;
					}

					@Override
					public Path next ()
					{
						return null;
					}
				};
			}
		};
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
		// TODO Auto-generated method stub
		return null;
	}

	private static final AccessControlList kAcl = AccessControlList.builder ()
		.withEntry ( AccessControlEntry.builder ().permit ().operation ( AccessControlList.READ ).forAllUsers ().build () )
		.build ()
	;
	
	@Override
	public ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException
	{
		if ( !exists ( context, objectPath ) )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}

		if ( objectPath.isRootPath () )
		{
			return new FibModelObject ( objectPath.toString () );
		}
		else
		{
			final long num = getNumberFrom ( objectPath );
			return new FibModelObject ( objectPath.toString (), num );
		}		
	}

	@Override
	public RelationSelector selectRelations ( Path objectPath )
	{
		return new BaseRelationSelector<FibonacciSequence> ( this, objectPath )
		{
			@Override
			public ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException
			{
				final Path p = getObject ();

				final LinkedList<ModelRelationInstance> result = new LinkedList<> ();
				if ( exists ( context, p ) )
				{
					if ( wantInbound () )
					{
						if ( p.equals ( makePathFor ( 1 ) ) && nameMatches ( kFirst ) )
						{
							result.add ( ModelRelationInstance.from ( Path.getRootPath (), kFirst, p ) );
						}
						else if ( !p.isRootPath () && nameMatches ( kNext ))
						{
							result.add ( ModelRelationInstance.from ( makePathFor ( prevFibFrom ( getNumberFrom ( p ) ) ), kNext, p ) );
						}
					}

					if ( wantOutbound () )
					{
						if ( p.isRootPath () && nameMatches ( kFirst ) )
						{
							result.add ( ModelRelationInstance.from ( p, kFirst, makePathFor ( 1 ) ) );
						}
						else if ( !p.isRootPath () && nameMatches ( kNext ) )
						{
							result.add ( ModelRelationInstance.from ( p, kNext, makePathFor ( nextFibFrom ( getNumberFrom ( p ) ) ) ) );
						}
					}
				}
				return ModelRelationList.simpleListOfCollection ( result );
			}
		};
	};

	private Path makePathFor ( long fib )
	{
		return Path.getRootPath ().makeChildItem ( Name.fromString ( Long.toString ( fib ) ) );
	}

	private long getNumberFrom ( Path objectPath ) throws NumberFormatException
	{
		if ( objectPath.isRootPath () || objectPath.getSegmentList ().size () > 1 )
		{
			throw new NumberFormatException ( "Not formatted as /<number>" );
		}
		return Long.parseLong ( objectPath.getSegment ( 0 ).toString () );
	}

	private boolean isFib ( long n )
	{
		if ( n < 1 ) return false;

		long first = 1;
		long second = 1;
		while ( second <= n && second < Long.MAX_VALUE )
		{
			if ( second == n )
			{
				return true;
			}
			final long next = first + second;
			first = second;
			second = next;
		}

		return false;
	}

	private long nextFibFrom ( long n )
	{
		if ( n < 1 ) return 1;

		long first = 1;
		long second = 1;
		while ( second <= n && second < Long.MAX_VALUE )
		{
			if ( second == n )
			{
				return first + second;
			}
			final long next = first + second;
			first = second;
			second = next;
		}

		throw new IllegalArgumentException ();
	}

	private long prevFibFrom ( long n )
	{
		if ( n <= 1 ) return 1;

		long first = 1;
		long second = 1;
		while ( second <= n && second < Long.MAX_VALUE )
		{
			if ( second == n )
			{
				return first;
			}
			final long next = first + second;
			first = second;
			second = next;
		}

		throw new IllegalArgumentException ();
	}

	private static final String kFirst = "first";
	private static final String kNext = "next";

	private class FibModelObject implements ModelObject
	{
		private final Long fNumber;
		
		public FibModelObject ( String id )
		{
			this ( id, null );
		}
		
		public FibModelObject ( String id, Long num )
		{
			fNumber = num;
		}

		@Override
		public AccessControlList getAccessControlList () { return kAcl; }

		@Override
		public JSONObject toJson ()
		{
			if ( fNumber == null ) return new JSONObject ();
			return new JSONObject ()
				.put ( "number", fNumber )
			;
		}

		@Override
		public ModelObjectMetadata getMetadata ()
		{
			return new ModelObjectMetadata ()
			{
				@Override
				public JSONObject toJson () { return new JSONObject (); }

				@Override
				public AccessControlList getAccessControlList () { return kAcl; }

				@Override
				public Set<String> getLockedTypes () { return new TreeSet<>(); }

				@Override
				public long getCreateTimeMs () { return 0; }

				@Override
				public long getLastUpdateTimeMs () { return 0; }
			};
		}

		@Override
		public JSONObject getData ()
		{
			return toJson ();
		}

		@Override
		public void putData ( JSONObject data )
		{
		}

		@Override
		public void patchData ( JSONObject data )
		{
		}
	};

}
