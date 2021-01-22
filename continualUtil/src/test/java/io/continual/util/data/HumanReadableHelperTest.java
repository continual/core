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

package io.continual.util.data;

import java.util.concurrent.TimeUnit;

import io.continual.util.time.Clock;
import io.continual.util.time.Clock.TestClock;
import junit.framework.TestCase;

public class HumanReadableHelperTest extends TestCase
{
	private static final String kCurrencyTests [][]=
	{
		new String[] { "123.45", "123.45"  },
		new String[] { "-123.45", "-123.45"  },
		new String[] { "-1234.56", "-1,234.56"  },
		new String[] { "123.04", "123.04"  },
	};

	public void testDollarsAndCents ()
	{
		for ( String[] test : kCurrencyTests )
		{
			final double in = Double.parseDouble ( test[0] );
			final String out = HumanReadableHelper.dollarValue ( in );
			assertEquals ( test[1], out );
		}
	}

	private static final String kTimeValueTests [][]=
	{
		new String[] { "12345", "1000", "12 s"  },
		new String[] { "12345", "60000", ""  },
	};

	public void testTimeValues ()
	{
		for ( String[] test : kTimeValueTests )
		{
			final long in = Long.parseLong ( test[0] );
			final long smallest = Long.parseLong ( test[1] );
			final String out = HumanReadableHelper.timeValue ( in, TimeUnit.MILLISECONDS, smallest );
			assertEquals ( test[2], out );
		}
	}

	private static final String kElapsedTimeTests [][]=
	{
		new String[] { "1399999998000", "1000", "-1", "2 s ago"  },
		new String[] { "1399999998000", "60000", "-1", "just now"  },
		new String[] { "3", "0", "-1", "44 yrs, 4 months, 3 wks, 2 days, 16 hrs, 53 m, 19 s, 997 ms ago"  },
		new String[] { "3", "0", "2", "44 yrs, 4 months ago"  },
		new String[] { "1399911440000", "0", "2", "1 day ago"  },
	};

	public void testElapsedTimeValues ()
	{
		final TestClock tc = Clock.useNewTestClock ();
		tc.set ( 1400000000000L );

		for ( String[] test : kElapsedTimeTests )
		{
			final long in = Long.parseLong ( test[0] );
			final long smallest = Long.parseLong ( test[1] );
			final int levels = Integer.parseInt ( test[2] );
			final String out = HumanReadableHelper.elapsedTimeSince ( in, smallest, levels );
			assertEquals ( test[3], out );
		}
	}
}
