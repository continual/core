package io.continual.services.processor.engine.library.sources;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.HumanReadableHelper;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.time.Clock;

/**
 * The JMX source connects to a JMX source and queries for a set of attributes.
 */
public class JmxSource extends BasicSource
{
	public JmxSource ( final ConfigLoadContext sc, JSONObject rawConfig ) throws BuildFailure
	{
		super ( rawConfig );

		try
		{
			final JSONObject config = sc.getServiceContainer ().getExprEval ().evaluateJsonObject ( rawConfig );

			//
			//	How often to poll? Use duration values like "6h" or "10d"
			//
			final String pollEvery = config.optString ( "pollEvery", null );
			fPollFreqMs = HumanReadableHelper.parseDuration ( pollEvery );
			if ( fPollFreqMs <= 0 )
			{
				throw new BuildFailure ( "Set pollEvery to be a positive value." );
			}
			fNextPollAtMs = Clock.now ();

			// optionally configure this source to run once and signal the end of stream
			fRunOnce = config.optBoolean ( "runOnce", false );

			//
			// connection info...
			//
			final String host = config.optString ( "host", "localhost" );
			final int port = config.getInt ( "port" );
			fUrl = new JMXServiceURL ( "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi" );

			// if key hierarchy is set, data is built into an object hierarchy based on the jmx key
			fKeyHierarchy = config.optBoolean ( "jmxKeyHierarchy", true );

			// mbeans can contain a lot of data. optionally generate separate messages per mbean
			fSeparateMsgsPerMBean = config.optBoolean ( "separateMsgsPerPollItem", true );
			
			// read a list of fetch specifications...
			fFetchSpecs = new LinkedList<> ();
			final JSONArray specificQueries = config.optJSONArray ( "pollList" );
			if ( specificQueries != null )
			{
				JsonVisitor.forEachElement ( specificQueries, new ArrayVisitor<JSONObject,JSONException> ()
				{
					@Override
					public boolean visit ( JSONObject querySpec ) throws JSONException
					{
						fFetchSpecs.add ( new JmxFetchSpec ( querySpec.optString ( "objectName", null ) ) );
						return true;
					}
				} );
			}
			else
			{
				// nothing specified = query everything
				fFetchSpecs.add ( new JmxFetchSpec ( null ) );
			}

			fPending = new LinkedList<> ();
		}
		catch ( MalformedURLException | JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc )
		throws IOException, InterruptedException
	{
		// any pending msgs left?
		if ( fPending.size () > 0 )
		{
			return fPending.removeFirst ();
		}

		// have we already polled on a run-once setup and set the next poll time to -1?
		if ( fNextPollAtMs < 0 )
		{
			noteEndOfStream ();
			return null;
		}

		// is it time to poll?
		if ( Clock.now () < fNextPollAtMs )
		{
			return null;
		}

		// generate next message(s)
		for ( JSONObject msgData : populateMsgData () )
		{
			fPending.add ( makeDefRoutingMessage ( Message.adoptJsonAsMessage ( msgData ) ) );

		}

		// next poll?
		fNextPollAtMs = fRunOnce ? -1L : Clock.now () + fPollFreqMs;

		// return first next msg
		return fPending.size () > 0 ? fPending.removeFirst () : null;
	}

	private final long fPollFreqMs;
	private long fNextPollAtMs;
	private final boolean fRunOnce;
	private final JMXServiceURL fUrl;
	private final boolean fKeyHierarchy;
	private final boolean fSeparateMsgsPerMBean;
	private final List<JmxFetchSpec> fFetchSpecs;
	private final LinkedList<MessageAndRouting> fPending;

	private static final Logger log = LoggerFactory.getLogger ( JmxSource.class );

	private static class JmxFetchSpec
	{
		public JmxFetchSpec ( String objName )
		{
			fObjName = objName;
		}

		public String getObjName () { return fObjName; }

		private final String fObjName;
	}

	private JSONObject buildDataContainer ( JSONObject domain, String keyprops )
	{
		final JSONObject attrData;
		if ( fKeyHierarchy )
		{
			JSONObject current = domain;

			final String[] keyParts = keyprops.split ( "," );
			for ( String keyPart : keyParts )
			{
				JSONObject newContainer = current.optJSONObject ( keyPart );
				if ( newContainer == null )
				{
					newContainer = new JSONObject ();
					current.put ( keyPart, newContainer );
				}
				current = newContainer;
			}

			attrData = current;
		}
		else
		{
			attrData = new JSONObject ();
			domain.put ( keyprops, attrData );
		}
		return attrData;
	}

	private Object valToJson ( Object val )
	{
		if ( val == null )
		{
			return null;
		}
		else if (
			( val instanceof String ) ||
			( val instanceof Boolean ) ||
			( val instanceof Byte ) ||
			( val instanceof Character ) ||
			( val instanceof Short ) ||
			( val instanceof Integer ) ||
			( val instanceof Long ) || 
			( val instanceof BigInteger ) ||
			( val instanceof BigDecimal )
		)
		{
			return val;
		}
		else if ( val instanceof Float )
		{
			final Float d = (Float) val;
			if ( d.isInfinite () || d.isNaN () )
			{
				return "NaN";
			}
			return val;
		}
		else if ( val instanceof Double )
		{
			final Double d = (Double) val;
			if ( d.isInfinite () || d.isNaN () )
			{
				return "NaN";
			}
			return val;
		}
		else if ( val instanceof ObjectName )
		{
			return ((ObjectName)val).toString ();
		}
		else if ( val instanceof CompositeDataSupport )
		{
			final JSONObject obj = new JSONObject ();

			final CompositeDataSupport cds = (CompositeDataSupport) val;
			for ( String key : cds.getCompositeType ().keySet () )
			{
				obj.put ( key, valToJson ( cds.get ( key ) ) );
			}

			return obj;
		}
		else if ( val instanceof TabularDataSupport )
		{
			final JSONArray obj = new JSONArray ();

			final TabularDataSupport tds = (TabularDataSupport) val;
			for ( Object oo : tds.values () )
			{
				Object json = valToJson ( oo );
				obj.put ( json );
			}
			
			return obj;
		}
		else if ( val.getClass ().isArray () )
		{
			final JSONArray arr = new JSONArray ();
			for ( int i=0; i<Array.getLength ( val ); i++ )
			{
				arr.put ( valToJson ( Array.get ( val, i ) ) );
			}
			return arr;
		}
		else if ( val instanceof List )
		{
			final JSONArray arr = new JSONArray ();
			for ( Object o : ((List<?>) val) )
			{
				arr.put ( valToJson ( o ) );
			}
			return arr;
		}
		else if ( val instanceof Set )
		{
			final JSONArray arr = new JSONArray ();
			for ( Object o : ((Set<?>) val) )
			{
				arr.put ( valToJson ( o ) );
			}
			return arr;
		}
		else if ( val instanceof Map )
		{
			final JSONObject obj = new JSONObject ();
			for ( Map.Entry<?, ?> entry : ( (Map<?, ?>) val ).entrySet () )
			{
				obj.put ( entry.getKey ().toString (),
					valToJson ( entry.getValue () ) );
			}
			return obj;
		}

		log.warn ( "Don't know how to convert {}", val.getClass ().getCanonicalName () );
		return null;
	}

	private List<JSONObject> populateMsgData (  ) throws IOException
	{
		final LinkedList<JSONObject> result = new LinkedList<> ();
		JSONObject current = null;

		// connect to the JMX source and populate our data
		log.debug ( "Connecting to JMX @ {}", fUrl );
		try ( final JMXConnector jmxc = JMXConnectorFactory.connect ( fUrl, null ) )
		{
			final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection ();

			for ( JmxFetchSpec q : fFetchSpecs )
			{
				// maybe start a new msgs
				if ( fSeparateMsgsPerMBean )
				{
					if ( current != null )
					{
						result.add ( current );
					}
					current = new JSONObject ();
				}

				// create an object name from the user's string
				ObjectName objname = null;
				final String onStr = q.getObjName ();
				if ( onStr != null )
				{
					objname = new ObjectName ( q.getObjName () );
				}
				if ( objname == null )
				{
					continue;
				}

				// use the given object name, expanding it if it's a pattern
				final Set<ObjectName> mbeans = new TreeSet<ObjectName> ();
				if ( objname.isPattern () )
				{
					mbeans.addAll ( mbsc.queryNames ( null, objname ) );
				}
				else
				{
					mbeans.add ( objname );
				}

				// iterate thru the bean list...
				for ( ObjectName mbean : mbeans )
		        {
					final String objName = mbean.toString ();
					final String[] parts = objName.split ( ":" );
					final String domainStr = parts[0];
					final String keyprops = ( parts.length == 1 || parts[1] == null ) ? "" : parts[1];

					log.info ( "Querying object {}...", objName );

					JSONObject domain = current.optJSONObject ( domainStr );
					if ( domain == null )
					{
						domain = new JSONObject ();
						current.put ( domainStr, domain );
					}

					// determine the attribute's location in our message
					final JSONObject attrData = buildDataContainer ( domain, keyprops );

					try
					{
						final MBeanInfo info = mbsc.getMBeanInfo ( mbean );
						final MBeanAttributeInfo[] attrInfo = info.getAttributes ();
						for ( MBeanAttributeInfo attribute : attrInfo )
						{
							if ( !attribute.isReadable () )
							{
								continue;
							}
							
							log.debug ( "Getting value for attr {} {}...", objName, attribute.getName () );
							try
							{
								final Object val = mbsc.getAttribute ( mbean, attribute.getName () );
								attrData.put ( attribute.getName (), valToJson ( val ) );
							}
							catch ( RuntimeMBeanException x )
							{
								if ( x.getCause () instanceof UnsupportedOperationException )
								{
									log.info ( "{} attribute {} is not supported. {}", objName, attribute.getName (), x.getMessage () );
								}
								else
								{
									throw x;
								}
							}
						}
					}
					catch ( RuntimeMBeanException x )
					{
						log.warn ( "RuntimeMBeanException on {} {}", objName, x.getMessage () );
					}
					catch ( Exception x )
					{
						// JMX is kind of insane; we're essentially forced to just catch anything 
						log.warn ( "Unable to retrieve data for {} {}", objName, x.getMessage () );
					}
		        }
			}

			if ( current != null )
			{
				result.add ( current );
			}
			
			return result;
		}
		catch ( MalformedObjectNameException x )
		{
			throw new IOException ( x );
		}
	}
}
