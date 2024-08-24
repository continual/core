package io.continual.services.processor.engine.library.sources;

public class StdInSource extends InputStreamSource
{
	public StdInSource ()
	{
		super ( System.in, false );
	}
}
