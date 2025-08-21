
package io.continual.messaging;

import java.io.IOException;

public class MessagePublishException
	extends IOException
{
	public MessagePublishException ()
	{
		super ();
	}

	public MessagePublishException ( Throwable t )
	{
		super ( t );
	}

	public MessagePublishException ( String msg )
	{
		super ( msg );
	}

	public MessagePublishException ( String msg, Throwable t )
	{
		super ( msg, t );
	}

	private static final long serialVersionUID = 1L;
}
