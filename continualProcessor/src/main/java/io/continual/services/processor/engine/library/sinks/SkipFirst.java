package io.continual.services.processor.engine.library.sinks;

import java.io.IOException;

import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;

public class SkipFirst implements Sink
{
	public SkipFirst ( Sink thenTo )
	{
		fThenTo = thenTo;
		fSkipped = false;
	}

	@Override
	public void close () throws IOException
	{
		fThenTo.close ();
	}

	@Override
	public void init ()
	{
		fThenTo.init ();
	}

	@Override
	public void flush ()
	{
		fThenTo.flush ();
	}

	@Override
	@Deprecated
	public void process ( Message msg )
	{
		if ( !fSkipped )
		{
			fSkipped = true;
			return;
		}
		fThenTo.process ( msg );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( !fSkipped )
		{
			fSkipped = true;
			return;
		}
		fThenTo.process ( context );
	}

	private final Sink fThenTo;
	private boolean fSkipped;
}
