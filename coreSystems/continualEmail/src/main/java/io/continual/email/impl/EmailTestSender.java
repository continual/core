package io.continual.email.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.MessagingException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.email.EmailService.MailStatus;
import io.continual.services.Service.FailedToStart;
import io.continual.services.ServiceContainer;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class EmailTestSender extends ConfiguredConsole
{
	private static final String kTo = "to";

	@Override
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kTo, "t" );
		
		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		// load config
		final String to = clp.getString ( kTo );
		if ( to == null || to.length () < 1 )
		{
			throw new MissingReqdSettingException ( kTo );
		}

		// pull settings for the email service from env
		final JSONObject config = new JSONObject ()
			.put ( SimpleEmailService.kSetting_SmtpServer, "${SMTP_HOST}" )
			.put ( SimpleEmailService.kSetting_SmtpServerPort, "${SMTP_PORT}" )
			.put ( SimpleEmailService.kSetting_SmtpServerUseAuth, "${SMTP_USEAUTH}" )
			.put ( SimpleEmailService.kSetting_SmtpServerSsl, "${SMTP_SSL}" )
			.put ( SimpleEmailService.kSetting_MailLogin, "${SMTP_USERNAME}" )
			.put ( SimpleEmailService.kSetting_MailPassword, "${SMTP_PASSWORD}" )
			.put ( SimpleEmailService.kSetting_MailFromEmail, "${SMTP_FROM_EMAIL}" )
			.put ( SimpleEmailService.kSetting_MailFromName, "${SMTP_FROM_NAME}" )
		;

		// build and run the email service
		final ServiceContainer sc = new ServiceContainer ();
		final SimpleEmailService svc = new SimpleEmailService ( sc, config );
		try
		{
			svc.start ();
			log.info ( "Service started." );

			final Future<MailStatus> status = svc.mail ( svc.createMessage ()
				.to ( to )
				.withSubject ( "Test Email" )
				.withSimpleText ( "This is a test message from the continual.io SimpleEmailService." )
			);
			log.info ( "Mail submitted." );

			final MailStatus ms = status.get ( 30, TimeUnit.SECONDS );
			log.info ( "Mail status: " + stringFor ( ms ) );
		}
		catch ( FailedToStart | MessagingException | InterruptedException | ExecutionException | TimeoutException e )
		{
			throw new StartupFailureException ( e );
		}
		finally
		{
			try
			{
				log.info ( "Service stopping." );
				svc.requestFinishAndWait ( 30, TimeUnit.SECONDS );
				log.info ( "Service finished." );
			}
			catch ( InterruptedException e )
			{
				log.warn ( "Service shutdown wait interrupted." );
			}
		}

		return null;
	}

	private static String stringFor ( MailStatus ms )
	{
		if ( ms.didSend () )
		{
			return "sent";
		}
		else if ( ms.didFail () )
		{
			final String msg = ms.getErrorMsg ();
			return msg == null ? "failed" : "failed: " + msg;
		}
		else
		{
			return "unknown";
		}
	}

	public static void main ( String[] args ) throws InterruptedException
	{
		try
		{
			final EmailTestSender ets = new EmailTestSender ();
			ets.runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			System.err.println ( e.getMessage () );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( EmailTestSender.class );
}
