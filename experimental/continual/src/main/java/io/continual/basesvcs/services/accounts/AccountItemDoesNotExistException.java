package io.continual.basesvcs.services.accounts;

public class AccountItemDoesNotExistException extends Exception
{
	public AccountItemDoesNotExistException ( String msg )
	{
		super ( msg );
	}

	public AccountItemDoesNotExistException ( Throwable t )
	{
		super ( t );
	}

	private static final long serialVersionUID = 1L;
}
