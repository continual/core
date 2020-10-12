package io.continual.monitor.daemon;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobDetail;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.resources.ResourceLoader;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.DaemonConsole;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.NvWriteable;

public class ContinualMonitorDaemon extends DaemonConsole
{
	public ContinualMonitorDaemon ()
	{
		super ( "ContinualMonitorDaemon" );
	}

	@Override
	protected ContinualMonitorDaemon setupDefaults ( NvWriteable pt )
	{
		super.setupDefaults ( pt );

		pt.set ( "monitors", "monitors.json" );

		return this;
	}

	@Override
	protected ContinualMonitorDaemon setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( "monitors", "m", null, null );
		
		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException 
	{
		// setup scheduler
		try
		{
			fScheduler = StdSchedulerFactory.getDefaultScheduler ();
			fScheduler.getContext ().put ( "systemSettings", p );
			fScheduler.getContext ().put ( "sharedData", new java.util.HashMap<String,JSONObject> () );
			fScheduler.start ();
		}
		catch ( SchedulerException e )
		{
			throw new StartupFailureException ( e );
		}

		try
		{
			// load the monitor set
			final String monFile = p.getString ( "monitors" );
			if ( monFile == null )
			{
				throw new NvReadable.MissingReqdSettingException ( "monitors" );
			}

			final InputStream monStream = ResourceLoader.load ( monFile );
			if ( monStream == null )
			{
				throw new StartupFailureException ( "Couldn't load [" + monFile + "]" );
			}

			final JSONObject mons = JsonUtil.readJsonObject ( monStream );
			JsonVisitor.forEachElement ( mons.optJSONObject ( "jobs" ),
				new ObjectVisitor<JSONObject,SchedulerException>()
				{
					@Override
					public boolean visit ( String name, JSONObject job ) throws JSONException, SchedulerException
					{
						if ( !job.optBoolean ( "enabled", true ) )
						{
							log.info ( "Job " + name + " is disabled." );
							return true;
						}

						final ScheduleBuilder<? extends Trigger> sb;

						final JSONObject sched = job.getJSONObject ( "schedule" );
						final String schedType = sched.getString ( "type" );
						if ( schedType.equalsIgnoreCase ( "cron" ) )
						{
							final String cron = sched.getString ( "expression" );
							sb = cronSchedule ( cron );
						}
						else if ( schedType.equalsIgnoreCase ( "once" ) )
						{
							sb = SimpleScheduleBuilder.repeatSecondlyForTotalCount ( 1 );
						}
						else
						{
							throw new SchedulerException ( "unrecognized schedule type " + schedType );
						}

						// transfer the user's job json to the wrapper job class as a string
						final JobDetail jd = newJob ( ContinualMonitorWrapper.class )
							.withIdentity ( name, "nebbyMonitorJobs" )
							.usingJobData ( "jobJson", job.getJSONObject ( "job" ).put("name",name).toString () )
							.build ()
						;

						final Trigger trigger = newTrigger ()
							.withIdentity ( "trigger:" + name, "nebbyMonitorJobs" )
							.startNow ()
							.withSchedule ( sb )
							.build ()
						;

						// Tell quartz to schedule the job using our trigger
						fScheduler.scheduleJob ( jd, trigger );
						log.info ( "Scheduled job: " + name );

						return true;
					}
				}
			);
		}
		catch ( SchedulerException | IOException | RuntimeException e )	// FIXME: cronSchedule throws RuntimeException on bad format (???)
		{
			try
			{
				fScheduler.shutdown ( false );
			}
			catch ( SchedulerException e2 )
			{
				log.warn ( "Problem shutting down scheduler during exception. " + e2.getMessage (), e2 );
			}
			throw new StartupFailureException ( e );
		}

		// run the daemon's init and background loop
		return super.init ( p, clp );
	}

	private int getJobCount () throws SchedulerException
	{
		return fScheduler.getJobKeys ( GroupMatcher.anyGroup () ).size ();
	}

	@Override
	protected boolean daemonStillRunning ()
	{
		try
		{
			final boolean isShutdown = fScheduler.isShutdown ();
			final boolean hasJobsLeft = getJobCount() > 0;
			if ( isShutdown || !hasJobsLeft )
			{
				log.warn ( "Stopping daemon: " + (isShutdown?"scheduler is shutdown":"scheduler is up") + " and " + (hasJobsLeft?"there are jobs":"there are no jobs") );
				return false;
			}
			return true;
		}
		catch ( SchedulerException e )
		{
			log.warn ( "Probem checking scheduler state: " + e.getMessage (), e );
			return false;
		}
	}

	@Override
	protected void daemonShutdown ()
	{
		try
		{
			fScheduler.shutdown ( false );
		}
		catch ( SchedulerException e )
		{
			log.warn ( "Problem shutting down scheduler during shutdown. " + e.getMessage (), e );
		}
	}

	public static void main ( String[] args )
	{
		try
		{
			final ContinualMonitorDaemon daemon = new ContinualMonitorDaemon ();
			daemon.runFromMain ( args );
		}
		catch ( Exception e )
		{
			System.err.println ( e.getMessage () );
			e.printStackTrace ( System.err );
		}
	}

	private Scheduler fScheduler;

	private static final Logger log = LoggerFactory.getLogger ( ContinualMonitorDaemon.class );
}
