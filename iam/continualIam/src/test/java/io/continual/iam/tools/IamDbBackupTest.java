package io.continual.iam.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.jsondoc.JsonDocDb;

public class IamDbBackupTest
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testBackupTo ()
	{
		try {
			final JsonDocDb jdd = new JsonDocDb () ;
			jdd.createUser ( "user1" );
			jdd.createGroup ( "groupid" , "groupdesc" );
			final IamDbBackup idb = new IamDbBackup ( jdd );

			final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			idb.backupTo ( baos );
			Assert.assertTrue ( baos.size() > 0 );
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testrestoreFrom ()
	{
		try {
			final JsonDocDb jdd = new JsonDocDb () ;
			jdd.createUser ( "user1" );
			jdd.createGroup ( "groupid" , "groupdesc" );
			final IamDbBackup idb = new IamDbBackup ( jdd );

			final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			idb.backupTo ( baos );
			final ByteArrayInputStream bais = new ByteArrayInputStream ( baos.toByteArray() );
			idb.restoreFrom ( bais );
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}
}
