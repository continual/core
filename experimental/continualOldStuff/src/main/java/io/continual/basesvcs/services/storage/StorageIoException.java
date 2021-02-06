package io.continual.basesvcs.services.storage;

import java.io.IOException;

public class StorageIoException extends IOException
{
	public StorageIoException ( String msg ) { super(msg); }
	public StorageIoException ( Throwable t ) { super(t); }
	private static final long serialVersionUID = 1L;
}
