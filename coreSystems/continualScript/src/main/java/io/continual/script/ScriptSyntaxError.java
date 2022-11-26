package io.continual.script;

public class ScriptSyntaxError extends Exception
{
	public enum ErrorCode
	{
		ILLEGAL_TOKEN { @Override public String toString () { return "Invalid token."; } },
		EXTRA_TOKENS { @Override public String toString () { return "Extra tokens at end of input."; } },
		NOT_ENOUGH_TOKENS { @Override public String toString () { return "Expected more tokens in input stream."; } },
		ILLEGAL_SIMPLE_NODE { @Override public String toString () { return "The value is not a simple identifier/value"; } },
		MISSING_THEN_CONSTRUCT { @Override public String toString () { return "Missing \"then\" clause"; } },
		MISSING_ELSE_CONSTRUCT { @Override public String toString () { return "Missing \"else\" clause"; } },
		MISSING_CLOSING_BRACE { @Override public String toString () { return "Expected } not found"; } },
		MISSING_CLOSING_PARENTHESIS { @Override public String toString () { return "Expected ) not found"; } },
		MISSING_OPENING_PARENTHESIS { @Override public String toString () { return "Expected ( not found"; } },
		MISSING_IF_CONDITION { @Override public String toString () { return "Missing \"if\" boolean condition"; } },
		MISSING_CLOSING_DOLLARSIGN { @Override public String toString () { return "Missing closing $ symbol"; } },
		MISSING_LEFT_BRACE { @Override public String toString () { return "Expected \"{\" not found"; } },
		MISSING_ASSIGN_RVALUE { @Override public String toString () { return "Expected an expression to the right of an assignment operator"; } },
		MISSING_EXPRESSION { @Override public String toString () { return "Expected an expression."; } }
	}

	public ScriptSyntaxError ( ErrorCode code )
	{
		this ( code, "" );
	}

	public ScriptSyntaxError ( ErrorCode code, String msg )
	{
		super ( code.toString() +
			(( msg != null && msg.length()>0 ) ? " " + msg : ""	)
		);
		fErrCode = code;
	}

	public ErrorCode getErrorCode ()
	{
		return fErrCode;
	}

	private ErrorCode fErrCode;
	static final long serialVersionUID = 1L;
};
