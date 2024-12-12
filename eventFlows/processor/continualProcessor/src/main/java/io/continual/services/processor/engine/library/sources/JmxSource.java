package io.continual.services.processor.engine.library.sources;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
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
		// is it time yet?
		if ( Clock.now () < fNextPollAtMs )
		{
			return null;
		}
		fNextPollAtMs = Clock.now () + fPollFreqMs;
		
		// if this source was configured to run once only, signal the end of stream
		// so that we're not called again
		if ( fRunOnce )
		{
			noteEndOfStream ();
		}

		// generate the message
		final JSONObject msgData = new JSONObject ();
		populateMsgData ( msgData );
		return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( msgData ) );
	}

	private final long fPollFreqMs;
	private long fNextPollAtMs;
	private final boolean fRunOnce;
	private final JMXServiceURL fUrl;
	private final boolean fKeyHierarchy;
	private final List<JmxFetchSpec> fFetchSpecs;

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
			( val instanceof Float ) ||
			( val instanceof Double ) ||
			( val instanceof BigInteger ) ||
			( val instanceof BigDecimal )
		)
		{
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
			for ( Object o : (Object[]) val )
			{
				arr.put ( valToJson ( o ) );
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

		log.info ( "Don't know how to convert {}", val.getClass ().getCanonicalName () );
		return null;
	}

	private void populateMsgData ( JSONObject msgData ) throws IOException
	{
		// connect to the JMX source and populate our data
		log.debug ( "Connecting to JMX @ {}", fUrl );
		try ( final JMXConnector jmxc = JMXConnectorFactory.connect ( fUrl, null ) )
		{
			final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection ();

			for ( JmxFetchSpec q : fFetchSpecs )
			{
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

					log.debug ( "Querying on object {}...", objName );

					JSONObject domain = msgData.optJSONObject ( domainStr );
					if ( domain == null )
					{
						domain = new JSONObject ();
						msgData.put ( domainStr, domain );
					}

					// determine the attribute location
					final JSONObject attrData = buildDataContainer ( domain, keyprops );

					try
					{
						final MBeanInfo info = mbsc.getMBeanInfo ( mbean );
						final MBeanAttributeInfo[] attrInfo = info.getAttributes ();
						for ( MBeanAttributeInfo attribute : attrInfo )
						{
							log.debug ( "Getting value for attr {} {}...", objName, attribute.getName () );

							final Object val = mbsc.getAttribute ( mbean, attribute.getName () );
							attrData.put ( attribute.getName (), valToJson ( val ) );
						}
					}
					catch ( Exception x )
					{
						// JMX is kind of insane; we're essentially forced to just catch anything 
						log.warn ( "Unable to retrieve data for {} {}", objName, x.getMessage () );
					}
		        }
			}
		}
		catch ( MalformedObjectNameException x )
		{
			throw new IOException ( x );
		}
	}
}
