/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam.tools;

import java.util.Vector;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.CommonJsonGroup;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.iam.impl.s3.S3IamDb;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram;

import io.continual.util.nv.NvReadable;

public class S3MigrateToNewGroups extends ConsoleProgram
{
	public S3MigrateToNewGroups ()
	{
		super ();
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs cmdLine )
	{
		final Vector<String> args = cmdLine.getFileArguments ();
		if ( args.size() < 4 )
		{
			System.err.print ( "usage: " + this.getClass ().getSimpleName() + " <awsKey> <awsSecret> <bucket> <prefix>" );
			return null;
		}

		try
		{
			final S3IamDb db = new S3IamDb.Builder ()
				.withAccessKey ( args.get ( 0 ) )
				.withSecretKey ( args.get ( 1 ) )
				.withBucket ( args.get ( 2 ) )
				.withPathPrefix ( args.get ( 3 ) )
				.build ()
			;
			for ( String user : db.getAllUsers () )
			{
				System.out.println ( user );
				final CommonJsonIdentity ii = db.loadUser ( user );
				for ( String groupId : ii.getGroupIds () )
				{
					System.out.println ( "\t" + groupId );
					final CommonJsonGroup g = db.loadGroup ( groupId );
					g.addUser ( user );
				}
			}
		}
		catch ( IamSvcException | BuildFailure e )
		{
			System.err.println ( e.getMessage () );
		}
		
		return null;
	}

	public static void main ( String[] args )
	{
		try
		{
			new S3MigrateToNewGroups ().runFromMain ( args );
		}
		catch ( Exception e )
		{
			System.err.println ( e.getMessage () );
		}
	}
}
