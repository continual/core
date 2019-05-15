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
package io.continual.util.legal;

import java.util.ArrayList;
import java.util.List;

public class CopyrightGenerator
{
	public static CopyrightGenerator getStandardNotice ()
	{
		return new CopyrightGenerator ();
	}

	public CopyrightGenerator addHolder ( String holder, int fromYear )
	{
		fHolders.add ( new HolderInfo ( holder, fromYear ) );
		return this;
	}

	public List<String> getCopyrightNotices ()
	{
		final ArrayList<String> result = new ArrayList<String> ();
		for ( HolderInfo hi : fHolders )
		{
			result.add ( getCopyrightNotice ( hi.fHolder, hi.fFromYr ) );
		}
		result.add ( getCopyrightNotice ( StdCopyrightInfo.kHolder, StdCopyrightInfo.kStartYear ) );
		return result;
	}

	public static String getCopyrightNotice ()
	{
		return getCopyrightNotice ( StdCopyrightInfo.kHolder, StdCopyrightInfo.kStartYear );
	}

	public static String getCopyrightNotice ( String holder, int fromYear )
	{
		return getCopyrightNotice ( holder, fromYear, thisYear () );
	}

	public static String getCopyrightNotice ( String holder, int fromYear, int toYear )
	{
		final StringBuffer sb = new StringBuffer ();
		sb.append ( "(c) " );
		sb.append ( getYearRange ( fromYear, toYear ) );
		sb.append ( ", " );
		sb.append ( holder );
		return sb.toString ();
	}

	public static String getYearRange ( int fromYear, int toYear )
	{
		final StringBuffer sb = new StringBuffer ();
		sb.append ( fromYear );
		if ( toYear > fromYear )
		{
			sb.append ( "-" );
			sb.append ( "" + toYear );
		}
		return sb.toString ();
	}

	private static int thisYear ()
	{
		return StdCopyrightInfo.kBuildYear;
	}

	private class HolderInfo
	{
		public HolderInfo ( String holder, int fromYr )
		{
			fHolder = holder;
			fFromYr = fromYr;
		}
		public final String fHolder;
		public final int fFromYr;
	};
	private final ArrayList<HolderInfo> fHolders = new ArrayList<HolderInfo> ();
}
