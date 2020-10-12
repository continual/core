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

package io.continual.services.processor.engine.library.sources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.time.Clock;

public abstract class QueuingSource extends BasicSource
{
	public boolean hasMessagesReady ( StreamProcessingContext spc ) throws IOException
	{
		reloadPending ();
		return fPending.size () > 0;
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc, long timeUnit, TimeUnit units ) throws IOException, InterruptedException
	{
		final long expireAtMs = Clock.now () + units.convert ( timeUnit, TimeUnit.MILLISECONDS );
		while ( !isEof () && !hasMessagesReady ( spc ) && Clock.now () < expireAtMs )
		{
			Thread.sleep ( 10 );
		}

		if ( !isEof () && hasMessagesReady ( spc ) )
		{
			return getNextPendingMessage ();
		}
		return null;
	}

	protected QueuingSource ( String defaultPipelineName )
	{
		super ( defaultPipelineName );

		fPending = new ArrayList<> ();
	}

	protected QueuingSource ( JSONObject config )
	{
		this ( config.getString ( "pipeline" ) );
	}

	protected MessageAndRouting getNextPendingMessage ()
	{
		return fPending.remove ( 0 );
	}

	protected List<MessageAndRouting> reload ()
	{
		return new ArrayList<> ();
	}

	private void reloadPending ()
	{
		fPending.addAll ( reload() );
	}

	private final ArrayList<MessageAndRouting> fPending;
}
