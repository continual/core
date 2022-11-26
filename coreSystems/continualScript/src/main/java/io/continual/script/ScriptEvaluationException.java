package io.continual.script;

public class ScriptEvaluationException extends Exception
{
	public ScriptEvaluationException ( String t )
	{
		super ( t );
		fReason = t;
	}

	public ScriptEvaluationException ( String t, Throwable cause )
	{
		super ( t, cause );
		fReason = t;
	}

	public String toString ()
	{
		return "Evaluation error: " + fReason;
	}

	private String fReason;
	static final long serialVersionUID = 0L;
};
