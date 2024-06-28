
package io.continual.iam.examples;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.MultiSourceDb;
import io.continual.iam.impl.auth0.Auth0IamDb;
import io.continual.iam.impl.s3.S3IamDb;

/**
 * An example of using IAM components outside of the Continual Assembly framework. 
 */
public class NonAssemblyIamUse
{
	/**
	 * entry point that expects a username and password
	 * @param args
	 */
	public static void main ( String[] args )
	{
		// get the credentials as authentication input
		if ( args.length != 2 )
		{
			System.err.println ( "usage: <username> <password>" );
			return;
		}
		final String username = args[0];
		final String password = args[1];

		try
		{
			// setup our IAM database (normally done once per process at startup, not per request!)
			final IamDb<?,?> db = useS3Db(); //useMultiSrcDb ();

			// run our authentication call
			final Identity user = db.authenticate ( new UsernamePasswordCredential ( username, password ) );
			if ( user != null )
			{
				System.out.println ( "Authenticated " + username );
			}
			else
			{
				System.err.println ( "Could not authenticate " + username );
			}
		}
		catch ( BuildFailure | IamSvcException x )
		{
			System.err.println ( x.getMessage () );
		}
	}

	//
	//	connect to an S3 IAM DB implementation
	//
	private static S3IamDb useS3Db () throws IamSvcException, BuildFailure
	{
		// we need some info from the environment
		final String awsAccessKey = System.getenv ( "AWS_IAM_ACCESS_KEY" );
		final String awsSecretKey = System.getenv ( "AWS_IAM_ACCESS_SECRET" );
		final String bucketName = System.getenv ( "AWS_IAM_BUCKET" );
		String pathPrefix = System.getenv ( "AWS_IAM_PATHPREFIX" );
		if ( pathPrefix != null && pathPrefix.length () == 0 ) pathPrefix = null;

		return new S3IamDb.Builder ()
			.withAccessKey ( awsAccessKey )
			.withSecretKey ( awsSecretKey )
			.withBucket ( bucketName )
			.withPathPrefix ( pathPrefix )
			.build ()
		;
	}

	//
	//	connect to an Auth0 IAM DB implementation
	//
	private static Auth0IamDb useAuth0Db () throws IamSvcException, BuildFailure
	{
		return Auth0IamDb.fromJson ( new JSONObject ()
			.put ( "domain", System.getenv ( "AUTH0_DOMAIN" ) )
			.put ( "clientId", System.getenv ( "AUTH0_CLIENTID" ) )
			.put ( "clientSecret", System.getenv ( "AUTH0_CLIENTSECRET" ) )
		);
	}

	//
	//	Connect to a "multisrc-db" implementation. Here we're suppressing the raw type
	//	related warnings because the two databases use different internal identity classes.
	//	Our client code uses the top-level interfaces only.
	//
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static IamDb<?,?> useMultiSrcDb () throws IamSvcException, BuildFailure
	{
		final MultiSourceDb db = new MultiSourceDb ();
		db.addDatabase ( useAuth0Db () );
		db.addDatabase ( useS3Db () );
		return db;
	}
}

