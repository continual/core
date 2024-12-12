package io.continual.services.processor.library.email.sources;

import javax.activation.DataSource;
import javax.mail.Flags;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.model.Message;
import io.continual.util.data.json.JsonVisitor;

public class MimeMessageWrapper extends Message
{
	public MimeMessageWrapper ( long uid, MimeMessage msg ) throws BuildFailure
	{
		try
		{
			final Flags flags = msg.getFlags ();

			fMmp = new MimeMessageParser ( msg );
			fMmp.parse ();

			final JSONObject baseMsgJson = super.accessRawJson ()
				.put ( "uid", uid )
				.put ( "from", fMmp.getFrom() )
				.put ( "to", JsonVisitor.listToArray ( fMmp.getTo (), (addr -> { return addr.toString (); }) ) )
				.put ( "cc", JsonVisitor.listToArray ( fMmp.getCc (), (addr -> { return addr.toString (); }) ) )
				.put ( "bcc", JsonVisitor.listToArray ( fMmp.getBcc (), (addr -> { return addr.toString (); }) ) )
				.put ( "replyTo", fMmp.getReplyTo() )
				.put ( "subject", fMmp.getSubject() )
				.put ( "hasAttachments", fMmp.hasAttachments() )
				.put ( "attachments", JsonVisitor.listToArray ( fMmp.getAttachmentList (), ( ds -> { return ds.getName (); }) ) )
				.put ( "seen", flags.contains ( Flags.Flag.SEEN ) )
			;

			final boolean hasPlain = fMmp.hasPlainContent ();
			baseMsgJson.put ( "hasPlainContent", hasPlain );
			if ( hasPlain ) baseMsgJson.put ( "plainContent", fMmp.getPlainContent () );

			final boolean hasHtml = fMmp.hasPlainContent ();
			baseMsgJson.put ( "hasHtmlContent", hasHtml );
			if ( hasHtml ) baseMsgJson.put ( "htmlContent", fMmp.getHtmlContent () );

			// apache commons doesn't return personal name from address
			final JSONObject fromDetails = new JSONObject ();
			final javax.mail.Address[] addresses = msg.getFrom ();
			if ( addresses != null && addresses.length > 0 )
			{
				final InternetAddress ia = ((InternetAddress) addresses[0]);
				fromDetails
					.put ( "address", ia.getAddress () )
					.put ( "personal", ia.getPersonal () )
				;
			}
			baseMsgJson.put ( "fromAddr", fromDetails );
	    }
		catch ( Exception e )
		{
			throw new BuildFailure ( e );
		}
	}

	public DataSource getAttachment ( String named )
	{
		return fMmp.findAttachmentByName ( named );
	}
	
	private final MimeMessageParser fMmp;
}
