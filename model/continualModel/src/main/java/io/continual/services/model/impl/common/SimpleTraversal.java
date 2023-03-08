package io.continual.services.model.impl.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectFilter;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

/**
 * A simple, non-optimized traversal implementation
 */
public class SimpleTraversal implements ModelTraversal
{
	public SimpleTraversal ( Model m )
	{
		fModel = m;
		fStart = null;
		fSteps = new LinkedList<>();
	}

	@Override
	public ModelTraversal startAt ( Path p )
	{
		fStart = p;
		return this;
	}

	@Override
	public ModelTraversal traverseOutbound ( String relation )
	{
		fSteps.add ( new Step ()
		{
			public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException
			{
				final TreeSet<Path> result = new TreeSet<> ();
				for ( ModelRelation mr : fModel.getOutboundRelationsNamed ( sc.fMrc, fStart, relation ) )
				{
					result.add ( mr.getTo () );
				}
				sc.replaceSet ( result );
			}
		} );
		return this;
	}

	@Override
	public ModelTraversal traverseInbound ( String relation )
	{
		fSteps.add ( new Step ()
		{
			public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException
			{
				final TreeSet<Path> result = new TreeSet<> ();
				for ( ModelRelation mr : fModel.getInboundRelationsNamed ( sc.fMrc, fStart, relation ) )
				{
					result.add ( mr.getFrom () );
				}
				sc.replaceSet ( result );
			}
		} );
		return this;
	}

	@Override
	public ModelTraversal labelSet ( String label )
	{
		fSteps.add ( new Step ()
		{
			public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException
			{
				final TreeSet<Path> capture = new TreeSet<> ();
				capture.addAll ( sc.fCurrentSet );
				sc.fCaptures.put ( label, capture );
			}
		} );
		return this;
	}

	@Override
	public ModelTraversal excludeSet ( String label )
	{
		fSteps.add ( new Step ()
		{
			public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException
			{
				final TreeSet<Path> captured = sc.fCaptures.get ( label );
				if ( captured != null )
				{
					final TreeSet<Path> newSet = new TreeSet<> ();
					for ( Path p : sc.fCurrentSet )
					{
						if ( !captured.contains ( p ) )
						{
							newSet.add ( p );
						}
					}
					sc.replaceSet ( newSet );
				}
			}
		} );
		return this;
	}

	@Override
	public ModelTraversal filterSet ( ModelObjectFilter filter )
	{
		fSteps.add ( new Step ()
		{
			public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException
			{
				final TreeSet<Path> newSet = new TreeSet<> ();
				for ( Path p : sc.fCurrentSet )
				{
					final ModelObject mo = fModel.load ( sc.fMrc, p );
					if ( filter.matches ( mo ) )
					{
						newSet.add ( p );
					}
				}
				sc.replaceSet ( newSet );
			}
		} );
		return this;
	}

	@Override
	public ModelPathList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
	{
		if ( fStart == null ) throw new ModelRequestException ( "Traversal has no start node." );

		final StepContext sc = new StepContext ( context );
		sc.fCurrentSet.add ( fStart );

		for ( Step s : fSteps )
		{
			s.execute ( sc );
			if ( sc.fCurrentSet.size () == 0 )
			{
				// short circuit this traversal
				break;
			}
		}

		return ModelPathList.wrap ( sc.fCurrentSet );
	}


	private final Model fModel;
	private Path fStart;
	private LinkedList<Step> fSteps;

	private class StepContext
	{
		public StepContext ( ModelRequestContext mrc ) { fMrc = mrc; }
		public void replaceSet ( TreeSet<Path> set )
		{
			fCurrentSet = set;
		}
		
		final ModelRequestContext fMrc;
		TreeSet<Path> fCurrentSet = new TreeSet<> ();
		HashMap<String,TreeSet<Path>> fCaptures = new HashMap<>();
	};
	
	private interface Step
	{
		public void execute ( StepContext sc ) throws ModelRequestException, ModelServiceException;
	}
}
