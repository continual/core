package io.continual.flowcontrol.impl.common;

import org.json.JSONObject;

import io.continual.flowcontrol.model.FlowControlJobVersion;

public class JsonJobVersion implements FlowControlJobVersion, Comparable<JsonJobVersion>
{
	public JsonJobVersion ( JSONObject versionData )
	{
		this ( versionData.getString ( kVersion ) );
	}

	public JsonJobVersion ( String v )
	{
		fVersion = v;

		// semver parse when possible...

		// ignore anything after a +
		String verstr = fVersion;
		final int plus = verstr.indexOf ( '+' );
		if ( plus >= 0 )
		{
			verstr = verstr.substring ( 0, plus );
		}

		final int minus = verstr.indexOf ( '-' );
		if ( minus >= 0 )
		{
			fLabel = verstr.substring ( minus + 1 );
			verstr = verstr.substring ( 0, minus );
		}
		else
		{
			fLabel = null;
		}

		final String[] parts = verstr.split ( "\\." );
		fPatch = readPart ( parts, 2 );
		fMinor = readPart ( parts, 1 );
		fMajor = readPart ( parts, 0 );

		fIsSemVer = fVersion.startsWith ( fMajor + "." + fMinor + "." + fPatch + ( fLabel == null ? "" : ( "-" + fLabel ) ) );
	}

	@Override
	public JSONObject toJson ()
	{
		return new JSONObject ()
			.put ( kVersion, fVersion )
		;
	}

	@Override
	public String toString ()
	{
		return fVersion;
	}

	@Override
	public int compareTo ( JsonJobVersion that )
	{
		if ( !this.fIsSemVer || !that.fIsSemVer )
		{
			return this.fVersion.compareTo ( that.fVersion );
		}
		
		int i = compareSegment ( this.fMajor, that.fMajor );
		if ( i != 0 ) return i;

		i = compareSegment ( this.fMinor, that.fMinor );
		if ( i != 0 ) return i;

		i = compareSegment ( this.fPatch, that.fPatch );
		if ( i != 0 ) return i;

		// each segment is equal... if one has a label, and the other doesn't, it's less
		if ( this.fLabel == null && that.fLabel == null ) return 0;
		if ( this.fLabel != null && that.fLabel == null ) return -1;
		if ( this.fLabel == null && that.fLabel != null ) return -1;
		return this.fLabel.compareTo ( that.fLabel );
	}

	private final String fVersion;

	private final int fMajor;
	private final int fMinor;
	private final int fPatch;
	private final String fLabel;
	private final boolean fIsSemVer;

	private static final String kVersion = "version";

	private int compareSegment ( int i, int j )
	{
		return Integer.compare ( i, j );
	}

	private int readPart ( String[] parts, int index )
	{
		if ( parts.length < index+1 ) return 0;
		final String part = parts[index];
		try
		{
			final int p = Integer.valueOf ( part );
			return p < 0 ? 0 : p;
		}
		catch ( NumberFormatException x )
		{
			return 0;
		}
	}
}
