package io.continual.services.processor.library.email.sources.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.activation.DataSource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.library.sources.CsvSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;

public class AttachmentDataLoader implements DataLoader
{
	public AttachmentDataLoader ()
	{
		
	}

	@Override
	public List<Message> getMessages ( long uid, MimeMessage msg ) throws BuildFailure, IOException
	{
		final LinkedList<Message> result = new LinkedList<>();

		final MimeMessageParser mmp;
		try
		{ 
			mmp = new MimeMessageParser ( msg );
			mmp.parse ();
		}
		catch ( Exception x )
		{
			throw new BuildFailure ( x );
		}

		if ( mmp.hasAttachments () )
		{
			for ( DataSource ds : mmp.getAttachmentList () )
			{
				final String contentType = ds.getContentType ();
				final Source src = buildSource ( contentType, ds.getInputStream () );
				while ( !src.isEof () )
				{
					try
					{
						final MessageAndRouting mar = src.getNextMessage ( null, 50, TimeUnit.MILLISECONDS );
						result.add ( mar.getMessage () );
					}
					catch ( IOException | InterruptedException e )
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		return result;
	}

	private Source buildSource ( String contentType, InputStream is ) throws BuildFailure
	{
		if ( contentType.equalsIgnoreCase ( "text/csv" ) )
		{
			final CsvSource src = new CsvSource ( new JSONObject () );
			src.setResource ( is );
		}
		else if ( contentType.equalsIgnoreCase ( "application/vnd.ms-excel" ) )
		{
		}

		throw new BuildFailure ( "Unknown content type: " + contentType );
	}
}
