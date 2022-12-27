package io.continual.iam.tools;

import org.junit.Test;

import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.file.IamFileDb;
import io.continual.util.db.file.BlockFile;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.json.JSONObject;
import org.junit.Assert;

public class IamFileDbToolTest
{
	@Test ( expected = IamSvcException.class )
	public void testCreateDb_Exception1 () throws IamSvcException
	{
		final IamFileDbTool ifdt = new IamFileDbTool ();
		ifdt.createDb ( new Vector<String> () , System.out );
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateDb_Exception2 () throws IamSvcException
	{
		final IamFileDbTool ifdt = new IamFileDbTool ();
		final Vector<String> vect = new Vector<String> ();
		vect.add ( "1" ); vect.add ( "2" ); vect.add ( "3" );
		ifdt.createDb ( vect , System.out );
	}

	@Test
	public void testCreateDb ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "key" , "value" );

		try {
			// File Preparation
			final File file = File.createTempFile ( "iamUnitTest", ".db" );
			BlockFile.initialize ( file , jsonObj.toString ().length () );
			final BlockFile bf = new BlockFile ( file , true );
			bf.create ( jsonObj.toString ().getBytes () );
			bf.close ();
			
			final IamFileDbTool ifdt = new IamFileDbTool ();
			// Args
			final Vector<String> vect = new Vector<>();
			vect.add ( file.getAbsolutePath () );
			final IamFileDb ifd = (IamFileDb) ifdt.createDb ( vect , System.out );
			Assert.assertNotNull ( ifd );
			Assert.assertNotNull ( ifd.getAclFor( new Resource () {
				@Override public String getId() { return "resId"; }
			} ) );
		} catch (IOException | IamSvcException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testMain ()
	{
		try {
			IamFileDbTool.main ( new String [] { "temp.txt" } );
		} catch (Exception e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}
}
