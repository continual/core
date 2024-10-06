package io.continual.services;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.Service.FailedToStart;
import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.nv.impl.nvReadableTable;

public class ServiceContainerTest
{
	@Test
	public void testBuild1 ()
	{
		try {
			final ServiceContainer sc = ServiceContainer.build ( new nvReadableTable () , true );
			sc.stopAll ();
		} catch ( StartupFailureException sfe ) {
			Assert.fail ( "Expected to execute but failed. Exception - " + sfe.getMessage () );
		}
	}

	@Test
	public void testBuild2 ()
	{
		try {
			ServiceContainer.build ( 
					ServiceContainer.class.getClassLoader ().getResourceAsStream ( "services.json" ) 
					, new String[] { "default" } , false );
		} catch ( StartupFailureException sfe ) {
			Assert.fail ( "Expected to execute but failed. Exception - " + sfe.getMessage () );
		}
	}

	@Test ( expected = StartupFailureException.class )
	public void testBuild_Exception1 () throws StartupFailureException
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( ServiceContainer.kServices , "serv.json" );

		ServiceContainer.build ( new nvReadableTable ( keyVal ) , false );
	}

	@Test ( expected = StartupFailureException.class )
	public void testBuild_Exception2 () throws StartupFailureException
	{
		ServiceContainer.build ( null , new String[] { "default" } , false );
	}

	@Test
	public void testAdd ()
	{
		final ServiceContainer sc = new ServiceContainer ();
		sc.add ( null , null );	// No Name
		Assert.assertTrue ( sc.getServiceNames ().isEmpty () );

		sc.add ( "serv1" , null );
		sc.add ( "serv1" , null ); // Same instance twice
		Assert.assertEquals ( 1 , sc.getServiceNames ().size () );
	}

	@Test
	public void testGet ()
	{
		try {
			final ServiceContainer sc = ServiceContainer.build ( new nvReadableTable () , false );
			Assert.assertNotNull ( sc.get ( "SimpleService1" , SimpleService.class ) );	// Valid
			Assert.assertNull ( sc.get ( "invalid" , null ) );	// Invalid service name
			Assert.assertNull ( sc.get ( "SimpleService1" , ServiceContainer.class ) );	// Invalid service class
		} catch ( StartupFailureException sfe ) {
			Assert.fail ( "Expected to execute but failed. Exception - " + sfe.getMessage () );
		}
	}

	@Test
	public void testAwaitTermination ()
	{
		try {
			final ServiceContainer sc = ServiceContainer.build ( new nvReadableTable () , false );
			sc.awaitTermination ();
		} catch ( StartupFailureException | InterruptedException e ) {
			Assert.fail ( "Expected to execute but failed. Exception - " + e.getMessage () );
		}
	}

	@Test
	public void testGetExprEval ()
	{
		final ServiceContainer sc = new ServiceContainer ();
		Assert.assertNotNull ( sc.getExprEval () );
	}

	@Test ( expected = FailedToStart.class )
	public void testStartAll_Exception () throws FailedToStart
	{
		final ServiceContainer sc = new ServiceContainer ();
		sc.add ( "TempService" , new TestSimpleServiceException () );
		sc.startAll ();
	}

	private static class TestSimpleServiceException extends SimpleService
	{
		@Override
		public synchronized void start () throws FailedToStart
		{
			throw new FailedToStart ( new Throwable ( "FailedToStart" ) );
		}
	}

	@Test
	public void getReqdTest1() throws BuildFailure
	{
		final ServiceContainer sc = new ServiceContainer ();
		try
		{
			sc.getReqd ( String.class );
			Assert.fail ( "Expected getReqd to fail" );
		}
		catch ( BuildFailure x )
		{
			// expected
		}
	}
	
	@Test
	public void getReqdTest2() throws BuildFailure
	{
		final ServiceContainer sc = new ServiceContainer ();
		Assert.assertEquals ( null, sc.get ( String.class ) );
	}
}
