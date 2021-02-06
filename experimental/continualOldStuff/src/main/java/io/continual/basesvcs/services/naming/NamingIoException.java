package io.continual.basesvcs.services.naming;

import java.io.IOException;

public class NamingIoException extends IOException
{
	public NamingIoException ( String msg ) { super(msg); }
	public NamingIoException ( Throwable t ) { super(t); }
	private static final long serialVersionUID = 1L;
}
