package io.continual.flowcontrol;

import java.util.Collection;

public interface FlowControlApi
{
	class FlowControlApiException extends Exception
	{
		public FlowControlApiException ( String msg ) { super(msg); }
		public FlowControlApiException ( Throwable t ) { super(t); }
		public FlowControlApiException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}
	
	FlowControlJobBuilder createJobBuilder () throws FlowControlApiException;

	FlowControlApi registerJob ( FlowControlJob job ) throws FlowControlApiException;

	Collection<FlowControlJob> getAllJobs () throws FlowControlApiException;

	FlowControlJob getJob ( String name ) throws FlowControlApiException;

	FlowControlApi updateJob ( FlowControlJob job ) throws FlowControlApiException;

	FlowControlApi removeJob ( FlowControlJob job ) throws FlowControlApiException;
}
