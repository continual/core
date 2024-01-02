package io.continual.iam.tools;

import java.io.PrintStream;
import java.util.Vector;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.iam.impl.s3.S3IamDb;

public class S3IamDbTool extends IamDbTool<CommonJsonIdentity,CommonJsonGroup>
{
	public S3IamDbTool ()
	{
		super ();
	}

	public static void main ( String[] args ) throws Exception
	{
		new S3IamDbTool().runFromMain ( args );
	}

	private class IamDbAclFactory implements AclFactory
	{
		@Override
		public AccessControlList createDefaultAcl ( AclUpdateListener acll )
		{
			return new AccessControlList ( acll );
		}
	}

	@Override
	protected IamDb<CommonJsonIdentity, CommonJsonGroup> createDb ( Vector<String> args, PrintStream outTo ) throws IamSvcException, BuildFailure
	{
		final int argc = args.size ();
		if ( argc != 4 && argc != 6 )
		{
			outTo.println ( "usage: connect <accessKey> <secret> <bucket> <pathPrefix> [<jwtIssuerName> <jwtSigningKey>]" );
			throw new IamSvcException ( "Incorrect usage for connect." );
		}

		JwtProducer jwt = null;
		if ( argc > 4 )
		{
			jwt = new JwtProducer.Builder ()
				.withIssuerName ( args.elementAt ( 4 ) )
				.usingSigningKey ( args.elementAt ( 5 ) )
	//			.lasting ( int seconds )
				.build ()
			;
		}
		
		return new S3IamDb.Builder ()
			.withAccessKey ( args.elementAt ( 0 ) )
			.withSecretKey ( args.elementAt ( 1 ) )
			.withBucket ( args.elementAt ( 2 ) )
			.withPathPrefix ( args.elementAt ( 3 ) )
			.usingAclFactory ( new IamDbAclFactory () )
			.withJwtProducer ( jwt )
			.build ();
	}
}
