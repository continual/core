package io.continual.flowcontrol;

import io.continual.services.Service;

/**
 * The flow control service is a controller for event processors.
 * 
 */
public interface FlowControlService extends Service
{
	FlowControlCallContextBuilder createtContextBuilder ();
	
	FlowControlApi getApiFor ( FlowControlCallContext ctx );
}
