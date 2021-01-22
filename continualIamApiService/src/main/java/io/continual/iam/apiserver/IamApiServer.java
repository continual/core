package io.continual.iam.apiserver;

import io.continual.services.Server;

public class IamApiServer
{
	public static void main ( String args[] ) throws Exception
	{
		Server.runServer ( "IAM API Server", args );
	}
}
