
package io.continual.metrics;

import io.continual.metrics.metricTypes.Counter;
import io.continual.metrics.metricTypes.Gauge;
import io.continual.metrics.metricTypes.Histogram;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * A facade for the actual metrics registry, but with scoped naming.
 */
public interface MetricsCatalog extends JsonSerialized
{
	/**
	 * Get a sub-catalog with the given name. Metrics added here are placed in the base catalog
	 * with a name prefix.
	 * 
	 * @param name
	 * @return a sub catalog
	 */
	MetricsCatalog getSubCatalog ( Name name );

	/**
	 * Get a sub-catalog with the given name. Metrics added here are placed in the base catalog
	 * with a name prefix.
	 * @param name
	 * @return a sub catalog
	 */
	default MetricsCatalog getSubCatalog ( String name )
	{
		return getSubCatalog ( Name.fromString ( name ) );
	}

	/**
	 * Pops a path on close
	 */
	interface PathPopper extends AutoCloseable
	{
		public void close ();	// no exception
	};

	/**
	 * Calls to this catalog to the given sub-catalog until a corresponding pop() is made (or PathPopper is closed)
	 * @param name
	 */
	PathPopper push ( String name );

	/**
	 * Pop the most recent push
	 */
	void pop ();
	
	/**
	 * Remove a metric by name
	 * @param name
	 */
	void remove ( String name );

	/**
	 * Return the {@link Counter} registered under this name; or create and
	 * register a new {@link Counter} if none is registered.
	 *
	 * @param name
	 *            the name of the metric
	 * @return a new or pre-existing {@link Counter}
	 */
	Counter counter ( Path name );

	/**
	 * Return a counter with the given name
	 * @param name
	 * @return
	 */
	default Counter counter ( String name ) { return counter ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) ); }

	/**
	 * Return a meter at the given path
	 * @param name
	 * @return a meter
	 */
	Meter meter ( Path name );

	/**
	 * Return a meter at the given path
	 * @param name
	 * @return a meter
	 */
	default Meter meter ( String name ) { return meter ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) ); }

	/**
	 * Gauges are typed, so we need a factory to register them.
	 * @param <T>
	 */
	interface GaugeFactory<T>
	{
		Gauge<T> makeNewGauage ();
	}

	/**
	 * Return a gauge at the given path
	 * @param <T>
	 * @param name
	 * @return a gauge
	 */
	<T> Gauge<T> gauge ( Path name, GaugeFactory<T> factory );

	/**
	 * Return a gauge at the given path
	 * @param name
	 * @return a gauge
	 */
	default <T> Gauge<T> gauge ( String name, GaugeFactory<T> factory ) { return gauge ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ), factory ); }

	/**
	 * Return a histogram at the given path
	 * @param name
	 * @return a histogram
	 */
	Histogram histogram ( Path name );

	/**
	 * Return a histogram at the given path
	 * @param name
	 * @return a histogram
	 */
	default Histogram histogram ( String name ) { return histogram ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) ); }

	/**
	 * Return a timer at the given path
	 * @param name
	 * @return a timer
	 */
	Timer timer ( Path name );

	/**
	 * Return a timer at the given path
	 * @param name
	 * @return a timer
	 */
	default Timer timer ( String name ) { return timer ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) ); }
}
