package io.continual.util.console;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.console.ConsoleProgram.UsageException;

public class CmdLineParserTest
{
	private final String word = "option" , singleChar = "o" , defValue = "0";
	private final String[] allowed = new String[] { "0" , "1" , "2" , "3" , "-1" };

	@Test
	public void testRegisterOptionWithValue1 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( word );
		Assert.assertTrue( result.hasArg( word ) );
	}

	@Test
	public void testRegisterOptionWithValue2 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( word , singleChar );
		Assert.assertTrue( result.hasArg( word ) );
	}

	@Test
	public void testRegisterOptionWithValue3 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		Assert.assertEquals( defValue , result.getArgFor( word ) );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterOptionWithValue_IllegalArgumentException1 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		clp.registerOptionWithValue( null ); 
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterOptionWithValue_IllegalArgumentException2 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		clp.registerOptionWithValue( word , "opt" ); 
	}

	@Test
	public void testRegisterOnOffOption1 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOnOffOption( word , singleChar.charAt(0) , true ); 
		Assert.assertTrue( result.isSet( word ) );
	}

	@Test
	public void testRegisterOnOffOption2 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOnOffOption( word , null , true ); 
		Assert.assertTrue( result.hasArg( word ) );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterOnOffOption_Exception ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		clp.registerOnOffOption( null , null , true ); 
	}

	@Test
	public void testRequireNoFileArguments ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.requireNoFileArguments();
		Assert.assertNotNull( result );
	}

	@Test
	public void testRequireOneFileArgument ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.requireOneFileArgument();
		Assert.assertNotNull( result );
	}

	@Test
	public void testRequireMinFileArguments ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.requireMinFileArguments( 1 );
		Assert.assertNotNull( result );
	}

	@Test
	public void testRequireFileArguments1 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.requireFileArguments( 2 );
		Assert.assertNotNull( result );
	}

	@Test
	public void testRequireFileArguments2 ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.requireFileArguments( 1 , 5 );
		Assert.assertNotNull( result );
	}

	@Test
	public void testHasArgFalse ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		Assert.assertFalse( clp.hasArg( word ) );
	}

	@Test
	public void testIsSetFalse ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		Assert.assertFalse( clp.isSet( word ) );
	}

	@Test
	public void testGetArgForEmpty ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		Assert.assertEquals( "" , clp.getArgFor( word ) );
	}

	@Test
	public void testProcessArgs_DoubleDashNoWord ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		try {
			clp.processArgs( new String[] { "--" } );
		} catch (UsageException e) {
			Assert.fail( "Expected to complete execution" );
		}
	}

	@Test
	public void testProcessArgs_DoubleDashWithWord ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		try {
			result.processArgs( new String[] { "--"+word , allowed[0] } );
		} catch (UsageException e) {
			Assert.fail( "Expected to complete execution" );
		}
	}

	@Test
	public void testProcessArgs_SingleDash ()
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		try {
			result.processArgs( new String[] { "-"+singleChar , allowed[0] } );
		} catch (UsageException e) {
			Assert.fail( "Expected to complete execution" );
		}
	}

	@Test
	public void testProcessArgs_SingleDashWithTwoOptions ()
	{
		CmdLineParser clp = new CmdLineParser ();
		clp = clp.registerOnOffOption( word , singleChar.charAt(0) , true );
		clp = clp.registerOnOffOption( "verbose" , 'v' , true );
		try {
			clp.processArgs( new String[] { "-"+singleChar+"v" } );
		} catch (UsageException e) {
			Assert.fail( "Expected to complete execution" );
		}
	}

	@Test
	public void testProcessArgs_PlainWord ()
	{
		CmdLineParser clp = new CmdLineParser ();
		clp = clp.registerOnOffOption( word , singleChar.charAt(0) , true );
		try {
			clp.processArgs( new String[] { word } );
		} catch (UsageException e) {
			Assert.fail( "Expected to complete execution" );
		}
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception1 () throws UsageException
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		result.processArgs( new String[] { "--"+word } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception2 () throws UsageException
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		result.processArgs( new String[] { "-" } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception3 () throws UsageException
	{
		CmdLineParser clp = new CmdLineParser ();
		clp = clp.registerOnOffOption( word , singleChar.charAt(0) , true );
		clp = clp.requireNoFileArguments();
		clp.processArgs( new String[] { word } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception4 () throws UsageException
	{
		CmdLineParser clp = new CmdLineParser ();
		clp = clp.registerOnOffOption( word , singleChar.charAt(0) , true );
		clp = clp.requireFileArguments( 3 );
		clp.processArgs( new String[] { word } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception5 () throws UsageException
	{
		CmdLineParser clp = new CmdLineParser ();
		clp = clp.registerOnOffOption( word , singleChar.charAt(0) , true );
		clp = clp.requireFileArguments( 2 , 3 );
		clp.processArgs( new String[] { word } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception6 () throws UsageException
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		result.processArgs( new String[] { "--verbose" , allowed[0] } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception7 () throws UsageException
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		result.processArgs( new String[] { "-v" , allowed[0] } );
	}

	@Test(expected = UsageException.class)
	public void testProcessArgs_Exception8 () throws UsageException
	{
		final CmdLineParser clp = new CmdLineParser ();
		final CmdLineParser result = clp.registerOptionWithValue( 
				word , singleChar , defValue , allowed );
		result.processArgs( new String[] { "-vc" , allowed[0] } );
	}
}
