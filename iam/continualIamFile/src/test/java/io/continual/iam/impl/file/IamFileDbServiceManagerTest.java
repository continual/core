package io.continual.iam.impl.file;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.ServiceContainer;
import io.continual.util.db.file.BlockFile;
import junit.framework.TestCase;

public class IamFileDbServiceManagerTest extends TestCase
{
	private final String issuer = "continual" , signingKey = "12345678901234567890123456789012345678901234567890";

	@Test
	public void testConstructor ()
	{
		// JWT
		final JSONObject jwtJsonObj = new JSONObject ();
		jwtJsonObj.put ( "issuer" , issuer );
		jwtJsonObj.put ( "sha256Key" , signingKey );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "jwt" , jwtJsonObj );

		try {
			// Create Block File with data
			final File file = File.createTempFile ( "iamUnitTest", ".db" );
			BlockFile.initialize ( file , jwtJsonObj.toString ().length () );
			final BlockFile bf = new BlockFile ( file , true );
			bf.create ( jwtJsonObj.toString ().getBytes () );
			bf.close ();

			jsonObj.put ( "file" , file.getAbsolutePath () );
			final IamFileDbServiceManager ifdsm = new IamFileDbServiceManager ( 
					new ServiceContainer () , jsonObj );

			// Basic test
			assertNotNull ( ifdsm.getIdentityDb () );
			assertNotNull ( ifdsm.getAccessDb () );
			assertNotNull ( ifdsm.getIdentityManager () );
			assertNotNull ( ifdsm.getAccessManager () );
			assertNotNull ( ifdsm.getTagManager () );

			// Acl coverage
			final IamFileDb ifd = (IamFileDb)ifdsm.getIdentityDb();
			assertNotNull ( ifd.getAclFor( new Resource () {
				@Override public String getId() { return "resId"; }
			} ) );
		} catch (IamSvcException | BuildFailure | IOException e) {
			fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testConstructor_Null ()
	{
		final JSONObject jsonObj = new JSONObject ();
		// JWT
		final JSONObject jwtJsonObj = new JSONObject ();
		jwtJsonObj.put ( "issuer" , issuer );
		jwtJsonObj.put ( "sha256Key" , signingKey );

		try {
			// Create Block File with data
			final File file = File.createTempFile ( "iamUnitTest", ".db" );
			BlockFile.initialize ( file , jwtJsonObj.toString ().length () );
			final BlockFile bf = new BlockFile ( file , true );
			bf.create ( jwtJsonObj.toString ().getBytes () );
			bf.close ();

			jsonObj.put ( "file" , file.getAbsolutePath () );
			// JWT null
			assertNotNull ( new IamFileDbServiceManager ( new ServiceContainer () , jsonObj ) );
			// JWT not null. But no issuer & secret
			jsonObj.put ( "jwt" , new JSONObject () );
			assertNotNull ( new IamFileDbServiceManager ( new ServiceContainer () , jsonObj ) );
		} catch (IamSvcException | BuildFailure | IOException e) {
			fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}
}
