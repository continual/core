package io.continual.flowcontrol;

import io.continual.services.Server;

public class FlowControlServer
{
	public static void main ( String args[] ) throws Exception
	{
		Server.runServer ( "FlowControl Server", args );
	}
}
