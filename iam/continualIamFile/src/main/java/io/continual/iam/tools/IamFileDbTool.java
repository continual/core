package io.continual.iam.tools;

import java.io.File;
import java.io.PrintStream;
import java.util.Vector;

import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.file.IamFileDb;

public class IamFileDbTool extends IamDbTool<CommonJsonIdentity,CommonJsonGroup>
{
	public IamFileDbTool ()
	{
		super ();
	}

	public static void main ( String[] args ) throws Exception
	{
		new IamFileDbTool().runFromMain ( args );
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
	protected IamDb<CommonJsonIdentity, CommonJsonGroup> createDb ( Vector<String> args, PrintStream outTo ) throws IamSvcException
	{
		final int argCount = args.size ();
		if ( argCount < 1 || argCount > 2 )
		{
			outTo.println ( "usage: connect <filename> [<password>]" );
			throw new IamSvcException ( "Wrong usage." );
		}

		return new IamFileDb.Builder ()
			.usingFile ( new File ( args.elementAt ( 0 ) ) )
			.forWrites ()
			.withPassword ( argCount > 1 ? args.elementAt ( 1 ) : null )
			.usingAclFactory ( new IamDbAclFactory () )
			.build ();
	}
}
