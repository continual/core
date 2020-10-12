/*
 *	Copyright 2019-2020, Continual.io
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
package io.continual.email;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import io.continual.services.Service;

/**
 * A service interface for sending email.
 */
public interface EmailService extends Service
{
	/**
	 * Mail status
	 */
	interface MailStatus
	{
		/**
		 * return true if the message was sent
		 * @return true/false
		 */
		boolean didSend ();

		/**
		 * return true if the message send failed
		 * @return true/false
		 */
		boolean didFail ();

		/**
		 * Get any error message associated with a failure. 
		 * @return an error message or null
		 */
		String getErrorMsg ();
	};

	interface MailBuilder
	{
		default MailBuilder to ( List<String> toAddrs )
		{
			for ( String to : toAddrs ) to ( to );
			return this;
		}

		default MailBuilder to ( String... toAddrs )
		{
			for ( String to : toAddrs ) to ( to );
			return this;
		}

		MailBuilder to ( String to );
		MailBuilder withSubject ( String subj );

		default MailBuilder withBodyPart ( String mimeType, String bodyText ) throws MessagingException
		{
			final MimeBodyPart part = new MimeBodyPart ();
			part.setContent ( bodyText, mimeType );
			return withBodyPart ( part );
		}

		default MailBuilder withBodyParts ( Map<String, String> render ) throws MessagingException
		{
			for ( Map.Entry<String,String> e : render.entrySet () )
			{
				withBodyPart ( e.getKey(), e.getValue () );
			}
			return this;
		}

		MailBuilder withBodyPart ( MimeBodyPart part );

		MailBuilder withSimpleText ( String text );
	}

	/**
	 * Create a mail message builder
	 * @return a new builder
	 */
	MailBuilder createMessage ();

	/**
	 * Mail a message based on the builder.
	 * @param builder
	 * @return a future mail status
	 */
	Future<MailStatus> mail ( MailBuilder builder );

	/**
	 * Mail a message to a single address. This is equivalent to mail with an address array with a single entry.
	 * @param to
	 * @param subject
	 * @param msg
	 * @return a future mail status
	 */
	default Future<MailStatus> mail ( String to, String subject, String msg )
	{
		try
		{
			return mail ( createMessage().to ( to ).withSubject ( subject ).withBodyPart ( "text/plain", msg ) );
		}
		catch ( MessagingException e )
		{
			return new InterfaceSendFailure ( e.getMessage () );
		}
	}

	/**
	 * Mail a message to multiple addresses.
	 * @param toAddrs
	 * @param subject
	 * @param msgBody
	 * @return a future mail status
	 */
	default Future<MailStatus> mail ( String[] toAddrs, String subject, String msgBody )
	{
		return mail ( Arrays.asList ( toAddrs ), subject, msgBody );
	}

	/**
	 * Mail a message to multiple addresses.
	 * @param toAddrs
	 * @param subject
	 * @param msgBody
	 * @return a future mail status
	 */
	default Future<MailStatus> mail ( Collection<String> toAddrs, String subject, String msgBody )
	{
		try
		{
			MailBuilder mb = createMessage().withSubject ( subject ).withBodyPart ( "text/plain", msgBody );
			for ( String to : toAddrs )
			{
				mb = mb.to ( to );
			}
			return mail ( mb );
		}
		catch ( MessagingException e )
		{
			return new InterfaceSendFailure ( e.getMessage () );
		}
	}

	/**
	 * Close the service.
	 */
	void close ();

	public static class InterfaceSendFailure implements Future<MailStatus>
	{
		public InterfaceSendFailure ( String msg ) { fMsg = msg; }

		@Override
		public boolean cancel ( boolean mayInterruptIfRunning ) { return false; }

		@Override
		public boolean isCancelled () { return false; }

		@Override
		public boolean isDone () { return true; }

		@Override
		public MailStatus get ()
		{
			return new MailStatus ()
			{
				@Override
				public boolean didSend () { return false; }

				@Override
				public boolean didFail () { return true; }

				@Override
				public String getErrorMsg () { return fMsg; }
			};
		}

		@Override
		public MailStatus get ( long timeout, TimeUnit unit ) { return get (); }

		private final String fMsg;
	}
}
