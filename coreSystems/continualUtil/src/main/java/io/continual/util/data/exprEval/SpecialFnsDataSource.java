package io.continual.util.data.exprEval;

import java.util.UUID;

import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.time.Clock;

public class SpecialFnsDataSource implements ExprDataSource
{
	@Override
	public Object eval ( String label )
	{
		if ( label.equals ( "now" ) || label.equals ( "nowMs" ) )
		{
			return Clock.now ();
		}
		else if ( label.equals ( "nowSec") )
		{
			return Clock.now () / 1000L;
		}
		else if ( label.equals ( "uuid") )
		{
			return UUID.randomUUID ().toString ();
		}
		else if ( label.equals ( "ulid") )
		{
			return UniqueStringGenerator.createUlid ();
		}
		return null;
	}
}
