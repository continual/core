package io.continual.iam.tools;

import java.io.PrintStream;
import java.util.Vector;

import io.continual.iam.IamDb;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.zk.StdZkIamDb;

public class ZkIamDbTool extends IamDbTool<CommonJsonIdentity,CommonJsonGroup>
{
	public ZkIamDbTool ()
	{
		super ();
	}

	public static void main ( String[] args ) throws Exception
	{
		new ZkIamDbTool().runFromMain ( args );
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
		if ( args.size () != 2 )
		{
			outTo.println ( "usage: connect <zkConnection> <pathPrefix>" );
			throw new IamSvcException ( "Incorrect usage for connect." );
		}
		
		return new StdZkIamDb.Builder ()
			.connectingTo ( args.elementAt ( 0 ) )
			.withPathPrefix ( args.elementAt ( 1 ) )
			.usingAclFactory ( new IamDbAclFactory () )
			.build ()
		;
	}
}
