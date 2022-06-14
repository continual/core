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
package io.continual.email.impl;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import io.continual.email.EmailService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.exprEval.ExpressionEvaluator;

/**
 * A simple service for sending email.
 */
public class SimpleEmailService extends SimpleService implements EmailService
{
	public SimpleEmailService ( ServiceContainer sc, JSONObject config )
	{
		fSenders = Executors.newFixedThreadPool ( config.optInt ( "threadCount", 1 ) );

		final ExpressionEvaluator ee = sc.getExprEval ( config );

		fMailProps = new Properties ();
		fMailProps.put ( "mail.smtp.host", ee.evaluateText ( config.optString ( kSetting_SmtpServer, "smtp.gmail.com" ) ) );
		fMailProps.put ( "mail.smtp.port", "" + ee.evaluateTextToInt ( config.opt ( kSetting_SmtpServerPort ), 587 ) );
		fMailProps.put ( "mail.smtp.socketFactory.fallback", "false" );
		fMailProps.put ( "mail.smtp.quitwait", "false" );
		fMailProps.put ( "mail.smtp.auth", "" + ee.evaluateTextToBoolean ( config.opt ( kSetting_SmtpServerUseAuth ), true ) ); 
		fMailProps.put ( "mail.smtp.starttls.enable", "" + ee.evaluateTextToBoolean ( config.opt ( kSetting_SmtpServerSsl ), true ) );

		fUser = ee.evaluateText ( config.optString ( kSetting_MailLogin, null ) );
		fPassword = ee.evaluateText ( config.optString ( kSetting_MailPassword, null ) );

		fFromAddr = ee.evaluateText ( config.optString ( kSetting_MailFromEmail, "hello@continual.io" ) );
		fFromName = ee.evaluateText ( config.optString ( kSetting_MailFromName, "Continual.io" ) );
	}

	private class MailBuilderImpl implements MailBuilder
	{
		@Override
		public MailBuilder to ( String to )
		{
			fTos.add ( to );
			return this;
		}

		@Override
		public MailBuilder withSubject ( String subj )
		{
			fSubj = subj;
			return this;
		}

		public boolean isMultipart ()
		{
			return fParts.size () > 0;
		}

		@Override
		public MailBuilder withBodyPart ( MimeBodyPart part )
		{
			fParts.add ( part );
			return this;
		}

		@Override
		public MailBuilder withSimpleText ( String text )
		{
			fText = text;
			return this;
		}

		public String getText () { return fText; }

		private final TreeSet<String> fTos = new TreeSet<> ();
		private String fSubj = "";
		private final LinkedList<MimeBodyPart> fParts = new LinkedList<> ();
		private String fText = "";
	}
	
	@Override
	public MailBuilder createMessage ()
	{
		return new MailBuilderImpl ();
	}

	@Override
	public Future<MailStatus> mail ( MailBuilder b )
	{
		final MailBuilderImpl builder = (MailBuilderImpl) b;

		// allow a no-op send
		if ( builder.fTos.size()== 0 ) return kNoopStatus;

		return fSenders.submit ( new MailTask ( builder ) );
	}

	@Override
	public void close ()
	{
		try
		{
			// shutdown our executors
			fSenders.shutdown ();
			fSenders.awaitTermination ( 30, TimeUnit.SECONDS );
		}
		catch ( InterruptedException x )
		{
			log.warn ( "SimpleEmailService shutdown took too long." );
			Thread.currentThread ().interrupt ();
		}
	}

	@Override
	protected void onStopRequested ()
	{
		close ();
	}

	private final Properties fMailProps;

	private final String fUser;
	private final String fPassword;

	private final String fFromAddr;
	private final String fFromName;

	public static final String kSetting_MailLogin = "mailLogin";
	public static final String kSetting_MailPassword = "mailPassword";
	public static final String kSetting_MailFromEmail = "mailFromEmail";
	public static final String kSetting_MailFromName = "mailFromName";
	public static final String kSetting_SmtpServer = "mailSmtpServer";
	public static final String kSetting_SmtpServerPort = "mailSmtpServerPort";
	public static final String kSetting_SmtpServerSsl = "mailSmtpServerSsl";
	public static final String kSetting_SmtpServerUseAuth = "mailSmtpServerUseAuth";

	private class MailTask implements Callable<MailStatus>
	{
		public MailTask ( MailBuilderImpl builder )
		{
			fBuilder = builder;
		}

		@Override
		public MailStatus call () 
		{
			final StringBuffer sbLog = new StringBuffer ();
			sbLog.append ( "sending mail to (" );
			final StringBuffer addrList = new StringBuffer ();
			for ( String to : fBuilder.fTos )
			{
				if ( addrList.length () > 0 )
				{
					addrList.append ( ", " );
				}
				addrList.append ( to );
			}
			sbLog.append ( addrList.toString () );
			sbLog.append ( ") \"" );
			sbLog.append ( fBuilder.fSubj );
			sbLog.append ( "\"" );
			log.info ( sbLog.toString () );

			try
			{
				final Session session = Session.getDefaultInstance ( fMailProps );

				final Message msg = new MimeMessage ( session );

				// subject
				msg.setSubject ( fBuilder.fSubj );

				// addressing
				final InternetAddress from = new InternetAddress ( fFromAddr, fFromName );
				msg.setFrom ( from );
				msg.setReplyTo ( new InternetAddress[] { from } );
				for ( String toAddr : fBuilder.fTos )
				{
					final InternetAddress to = new InternetAddress ( toAddr );
					msg.addRecipient ( Message.RecipientType.TO, to );
				}

				if ( fBuilder.isMultipart () )
				{
					// message content alternatives
					final Multipart contentAlternatives = new MimeMultipart ( "alternative" );
					for ( MimeBodyPart bodyPart : fBuilder.fParts )
					{
						contentAlternatives.addBodyPart ( bodyPart );
					}
	
					// top-level...
					final MimeBodyPart altsBodyPart = new MimeBodyPart ();
					altsBodyPart.setContent ( contentAlternatives );
	
					final Multipart mixedMultipart = new MimeMultipart ( "mixed" );
					mixedMultipart.addBodyPart ( altsBodyPart );
					msg.setContent ( mixedMultipart );
				}
				else
				{
					msg.setText ( fBuilder.getText () );
				}
					
				final Transport transport = session.getTransport ( "smtp" );
				transport.connect ( fUser, fPassword );
				transport.sendMessage ( msg, msg.getAllRecipients () );
				transport.close ();

				log.info ( "Mail sent." );

				return new SimpleMailStatus ();
			}
			catch ( MessagingException | UnsupportedEncodingException e )
			{
				log.warn ( "Error sending email: {}", e.getMessage(), e );
				return new SimpleMailStatus ( e.getMessage () );
			}
		}

		private final MailBuilderImpl fBuilder;
	}

	private final ExecutorService fSenders;
	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( SimpleEmailService.class );

	private static class SimpleMailStatus implements MailStatus
	{
		public SimpleMailStatus () { this ( null ); }

		public SimpleMailStatus ( String err ) { fErr = err; }

		@Override
		public boolean didSend () { return !didFail(); }

		@Override
		public boolean didFail () { return fErr != null; }

		@Override
		public String getErrorMsg () { return fErr; }

		private final String fErr;
	}

	private static final Future<MailStatus> kNoopStatus = new Future<MailStatus> ()
	{
		@Override
		public boolean cancel ( boolean mayInterruptIfRunning ) { return true; }

		@Override
		public boolean isCancelled () { return false; }

		@Override
		public boolean isDone () { return true; }

		@Override
		public MailStatus get () { return new SimpleMailStatus (); }

		@Override
		public MailStatus get ( long timeout, TimeUnit unit ) { return get (); }
	};
}
