package io.continual.services.messaging.impl.kafka.tools;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.notify.ContinualNotifier;
import io.continual.services.messaging.impl.kafka.tools.KafkaClusterStatus.Group;
import io.continual.services.messaging.impl.kafka.tools.KafkaClusterStatus.KafkaClusterStatusException;
import io.continual.services.messaging.impl.kafka.tools.KafkaClusterStatus.Topic;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.data.StringUtils;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.SpecialFnsDataSource;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class KafkaClusterReporter extends ConsoleProgram
{
	public interface KafkaClusterReport extends JsonSerialized
	{
		Map<String,KafkaClusterStatus.Topic> getTopics ();
		Map<String,KafkaClusterStatus.Group> getGroups ();

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "topics", JsonVisitor.mapOfJsonToObject ( getTopics () ) )
				.put ( "groups", JsonVisitor.mapOfJsonToObject ( getGroups () ) )
			;
		}
	}

	public interface ReportSender extends Closeable
	{
		void send ( KafkaClusterReport report );
	}
	
	@Override
	protected KafkaClusterReporter setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( ConfiguredConsole.kConfigFile, ConfiguredConsole.kConfigFileChar, null, null );

		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		final String configFile = p.get ( ConfiguredConsole.kConfigFile );
		if ( StringUtils.isEmpty ( configFile ) ) throw new MissingReqdSettingException ( ConfiguredConsole.kConfigFile );

		try
		{
			final JSONObject config = loadConfig ( configFile );

			// setup kafka admin client
			final JSONObject kafkaConfig = config.getJSONObject ( "kafka" );
			final Map<String,Object> configMap = JsonVisitor.objectToMap ( kafkaConfig, new ItemRenderer<Object,Object> ()
			{
				@Override
				public Object render ( Object val ) { return val; }
			} );

			// run the report
			final KafkaClusterReport report;
			try ( final AdminClient kafka = KafkaAdminClient.create ( configMap ) )
			{
				report = generateKafkaReport ( kafka );
			}
			catch ( StartupFailureException e )
			{
				System.err.println ( e.getMessage () );
				return null;
			}

			// setup reporter
			final ReportSender sender;
			final JSONObject reporter = config.optJSONObject ( "reporter" );
			if ( reporter != null )
			{
				try
				{
					sender = Builder.fromJson ( ReportSender.class, reporter );
				}
				catch ( BuildFailure e )
				{
					throw new StartupFailureException ( e );
				}
			}
			else
			{
				sender = new ReportSender ()
				{
					@Override
					public void send ( KafkaClusterReport report )
					{
						for ( Topic t : report.getTopics ().values () )
						{
							new ContinualNotifier ()
								.inBackground ()
								.onSubject ( Path.getRootPath ()
									.makeChildItem ( Name.fromString ( "kafka" ) )
									.makeChildItem ( Name.fromString ( "topics" ) )
									.makeChildItem ( Name.fromString ( t.getName () ) )
								)
								.withAddlData ( "topic", t.toJson () )
								.send ()
							;
						}

						for ( Group g : report.getGroups ().values () )
						{
							new ContinualNotifier ()
								.inBackground ()
								.onSubject ( Path.getRootPath ()
									.makeChildItem ( Name.fromString ( "kafka" ) )
									.makeChildItem ( Name.fromString ( "consumers" ) )
									.makeChildItem ( Name.fromString ( g.getId () ) )
								)
								.withAddlData ( "group", g.toJson () )
								.send ()
							;
						}
					}

					@Override
					public void close () throws IOException
					{
						try
						{
							ContinualNotifier.closeAndWaitForBackgroundSends ( 30000 );
						}
						catch ( InterruptedException e )
						{
							throw new IOException ( e );
						}
					}
				};
			}

			log.info ( report.toString () );
			sender.send ( report );
			sender.close ();

			return null;
		}
		catch ( JSONException | IOException x )
		{
			throw new StartupFailureException ( x );
		}
	}

	private KafkaClusterReport generateKafkaReport ( AdminClient kafka ) throws StartupFailureException
	{
		try ( final KafkaClusterStatus status = new KafkaClusterStatus ( kafka ) )
		{
			final Map<String,KafkaClusterStatus.Topic> topics = status.getTopicsReport ();
			final Map<String,KafkaClusterStatus.Group> groups = status.getConsumersReport ();
			status.calcLags ( topics, groups );

			return new KafkaClusterReport ()
			{
				@Override
				public Map<String, Topic> getTopics () { return topics; }

				@Override
				public Map<String, Group> getGroups () { return groups; }
			};
		}
		catch ( KafkaClusterStatusException x )
		{
			throw new StartupFailureException ( x );
		}
	}
	
	public static void main ( String[] args )
	{
		try
		{
			new KafkaClusterReporter ().runFromMain ( args );
		}
		catch ( Exception e )
		{
			System.err.println ( e.getMessage () );
		}
	}

	private JSONObject loadConfig ( String configFile ) throws InvalidSettingValueException
	{
		final File cf = new File ( configFile );
		if ( !cf.exists () )
		{
			throw new InvalidSettingValueException ( "Couldn't load configuration file [" + configFile + "]." );
		}

		try ( FileInputStream fis = new FileInputStream ( cf ) )
		{
			final JSONObject rawConfig = new JSONObject ( new CommentedJsonTokener ( fis ) );

			final ExprDataSourceStack stack = new ExprDataSourceStack (
				new EnvDataSource (),
				new SpecialFnsDataSource ()
			);
			final ExpressionEvaluator ee = new ExpressionEvaluator ( stack );

			return ee.evaluateJsonObject ( rawConfig );
		}
		catch ( JSONException | IOException e )
		{
			throw new InvalidSettingValueException ( ConfiguredConsole.kConfigFile, e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( KafkaClusterReporter.class );
}
