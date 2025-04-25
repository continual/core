/*
 *  Copyright (c) 2006-2025 Continual.io. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.continual.metrics;

import io.continual.metrics.metricTypes.Gauge;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;

public class JmxMetricsCollector extends SimpleService
{
	public JmxMetricsCollector ( ServiceContainer sc, JSONObject config )
	{
		final MetricsService metrics = sc.get ( "metrics", MetricsService.class );
		if ( metrics == null )
		{
			log.warn ( "No metrics service available for JMX metrics collector" );
			fJmxCatalog = null;
			return;
		}

		final Path jmx = Path.getRootPath ().makeChildItem ( Name.fromString ( "jmx" ) );
		fJmxCatalog = metrics.getCatalog ( jmx );

		//
		// register memory usage metrics
		//
		final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean ();

		final Path mem = Path.getRootPath ().makeChildItem ( Name.fromString ( "mem" ) );
		final Path heap = mem.makeChildItem ( Name.fromString ( "heap" ) );
		final Path nonHeap = mem.makeChildItem ( Name.fromString ( "nonHeap" ) );

		fJmxCatalog.gauge ( heap.makeChildItem ( Name.fromString ( "usage" ) ), () -> {
			return new Gauge<Long> () {
				@Override
				public Long getValue () { return memoryBean.getHeapMemoryUsage ().getUsed (); }
			};
		} );
		fJmxCatalog.gauge ( heap.makeChildItem ( Name.fromString ( "max" ) ), () -> {
			return new Gauge<Long> () {
				@Override
				public Long getValue () { return memoryBean.getHeapMemoryUsage ().getMax (); }
			};
		} );
		fJmxCatalog.gauge ( heap.makeChildItem ( Name.fromString ( "pctUsed" ) ), () -> {
			return new Gauge<Double> () {
				@Override
				public Double getValue ()
				{
					final long max = memoryBean.getHeapMemoryUsage ().getMax ();
					final long used = memoryBean.getHeapMemoryUsage ().getUsed ();
					final double pct = Math.round ( (double) used / (double) max * 1000.0 ) / 10.0;
					return pct;
				}
			};
		} );
		fJmxCatalog.gauge ( nonHeap.makeChildItem ( Name.fromString ( "usage" ) ), () -> {
			return new Gauge<Long> () {
				@Override
				public Long getValue () { return memoryBean.getNonHeapMemoryUsage ().getUsed (); }
			};
		} );

		//
		// register thread info
		//
		final Path threads = Path.getRootPath ().makeChildItem ( Name.fromString ( "threads" ) );
		final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean ();

		fJmxCatalog.gauge ( threads.makeChildItem ( Name.fromString ( "count" ) ), () -> {
			return new Gauge<Integer> () {
				@Override
				public Integer getValue () { return threadBean.getThreadCount (); }
			};
		} );
		fJmxCatalog.gauge ( threads.makeChildItem ( Name.fromString ( "peak" ) ), () -> {
			return new Gauge<Integer> () {
				@Override
				public Integer getValue () { return threadBean.getPeakThreadCount (); }
			};
		} );
		fJmxCatalog.gauge ( threads.makeChildItem ( Name.fromString ( "daemon" ) ), () -> {
			return new Gauge<Integer> () {
				@Override
				public Integer getValue () { return threadBean.getDaemonThreadCount (); }
			};
		} );
		fJmxCatalog.gauge ( threads.makeChildItem ( Name.fromString ( "totalStarted" ) ), () -> {
			return new Gauge<Long> () {
				@Override
				public Long getValue () { return threadBean.getTotalStartedThreadCount (); }
			};
		} );

		// get the GC gauges started
		reloadGcGauges ();
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		fReloadGcThread.start ();
	}

	//
	// (re)register GC info
	//
	private void reloadGcGauges ()
	{
		final Name gcName = Name.fromString ( "gc" );
		final MetricsCatalog gcCat = fJmxCatalog.getSubCatalog ( gcName );

		for ( GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans () )
		{
			final Path gcb = Path.getRootPath ().makeChildItem ( Name.fromString ( gcBean.getName () ) );
			gcCat.gauge ( gcb.makeChildItem ( Name.fromString ( "count" ) ), () -> {
				return new Gauge<Long> () {
					@Override
					public Long getValue () { return gcBean.getCollectionCount (); }
				};
			} );
			gcCat.gauge ( gcb.makeChildItem ( Name.fromString ( "timeMs" ) ), () -> {
				return new Gauge<Long> () {
					@Override
					public Long getValue () { return gcBean.getCollectionTime (); }
				};
			} );
		}
	}

	private final MetricsCatalog fJmxCatalog;
	private final Thread fReloadGcThread = new Thread ( () ->
	{
		while ( isRunning () )
		{
			try
			{
				Thread.sleep ( 15 * 1000L );
			}
			catch ( InterruptedException e )
			{
				break;
			}
			reloadGcGauges ();
		}
	} );

	private static final Logger log = LoggerFactory.getLogger ( JmxMetricsCollector.class );
}
