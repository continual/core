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
package io.continual.util.console;

import io.continual.util.nv.NvReadable;


/**
 * A looper that does nothing but check for an exit state once in awhile
 */
public class BackgroundLooper implements ConsoleProgram.Looper
{
	public BackgroundLooper ( int freqMs )
	{
		fFreqMs = freqMs;
	}

	@Override
	public boolean loop ( NvReadable p )
	{
		try
		{
			Thread.sleep ( fFreqMs );
		}
		catch ( InterruptedException e )
		{
			// ignore
		}
		return stillRunning ();
	}

	@Override
	public boolean setup ( NvReadable p, CmdLinePrefs clp )
	{
		return true;
	}

	@Override
	public void teardown ( NvReadable p )
	{
	}

	/**
	 * Override this to write something that monitors a run state. The base
	 * class always reports that the program has ended.
	 * @return true if the program is still running.
	 */
	public boolean stillRunning () { return false; }

	private final int fFreqMs;
}
