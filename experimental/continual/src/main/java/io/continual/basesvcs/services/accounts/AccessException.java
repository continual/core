package io.continual.basesvcs.services.accounts;

public class AccessException extends Exception
{
	public AccessException ( String msg ) { super(msg); }
	public AccessException ( Throwable t ) { super(t); }
	private static final long serialVersionUID = 1L;
}
