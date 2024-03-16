// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.server.support.server.monitoring;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.SimpleTimer;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Pattern;

public class ServerMetricsHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMetricsHandler.class);

    private static final Counter OPERATION_START_COUNTER = createCounter("datapush_operations", "Counter of data push operations started");
    private static final Summary OPERATION_COMPLETE_SUMMARY = createDurationSummary("datapush_operations_completed", "Duration summary for data push operations completing with no error or redirect");
    private static final Summary OPERATION_REDIRECT_SUMMARY = createDurationSummary("datapush_operations_redirected", "Duration summary for data push operations terminating with a redirect");
    private static final Summary OPERATION_ERROR_SUMMARY = createDurationSummary("datapush_operations_errors", "Duration summary for data push operations terminating with an error");

    private static final MetricsRegistry<Summary> ADDITIONAL_SUMMARIES = new MetricsRegistry<Summary>("duration summary")
    {
        @Override
        protected Summary createNewMetric(String name, String help)
        {
            return createDurationSummary(name, help);
        }
    };

    private static final MetricsRegistry<Counter> ADDITIONAL_COUNTERS = new MetricsRegistry<Counter>("counter")
    {
        @Override
        protected Counter createNewMetric(String name, String help)
        {
            return createCounter(name, help);
        }
    };

    public static void operationStart()
    {
        OPERATION_START_COUNTER.inc();
    }

    public static void operationComplete(long startNanos, long endNanos, String durationMetricName)
    {
        operationTermination(startNanos, endNanos, OPERATION_COMPLETE_SUMMARY, durationMetricName);
    }

    public static void operationRedirect(long startNanos, long endNanos, String durationMetricName)
    {
        operationTermination(startNanos, endNanos, OPERATION_REDIRECT_SUMMARY, durationMetricName);
    }

    public static void operationError(long startNanos, long endNanos, String durationMetricName)
    {
        operationTermination(startNanos, endNanos, OPERATION_ERROR_SUMMARY, durationMetricName);
    }

    private static void operationTermination(long startNanos, long endNanos, Summary durationSummary, String durationMetricName)
    {
        double duration = SimpleTimer.elapsedSecondsFromNanos(startNanos, endNanos);
        durationSummary.observe(duration);
        if (durationMetricName != null)
        {
            Summary summary = ADDITIONAL_SUMMARIES.getOrCreate(durationMetricName);
            if (summary != null)
            {
                summary.observe(duration);
            }
        }
    }

    public static void incrementCounter(String name)
    {
        Counter counter = ADDITIONAL_COUNTERS.getOrCreate(name);
        if (counter != null)
        {
            counter.inc();
        }
    }

    private static Summary createDurationSummary(String name, String help)
    {
        return Summary.build(name, help)
                .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                .register();
    }

    private static Counter createCounter(String name, String help)
    {
        return Counter.build(name, help).register();
    }

    private abstract static class MetricsRegistry<T extends Collector>
    {
        private static final String METRIC_PREFIX = "datapush_";
        private static final Pattern METRIC_NAME_REPLACE = Pattern.compile("\\W++");

        private final ConcurrentMutableMap<String, T> metrics = ConcurrentHashMap.newMap();
        private final String description;

        protected MetricsRegistry(String description)
        {
            this.description = Objects.requireNonNull(description);
        }

        T getOrCreate(String name)
        {
            T metric = this.metrics.get(name);
            if ((metric == null) && !this.metrics.containsKey(name))
            {
                synchronized (this)
                {
                    metric = this.metrics.get(name);
                    if ((metric == null) && !this.metrics.containsKey(name))
                    {
                        try
                        {
                            String metricName = METRIC_PREFIX + METRIC_NAME_REPLACE.matcher(name).replaceAll("_");
                            String metricHelp = name + " " + this.description;
                            metric = createNewMetric(metricName, metricHelp);
                        }
                        catch (Exception e)
                        {
                            LOGGER.error("Error creating new {} \"{}\"", this.description, name, e);
                        }
                        // If metric creation fails, we put null in the map so that we don't keep trying and failing to create the same metric.
                        this.metrics.put(name, metric);
                    }
                }
            }
            return metric;
        }

        protected abstract T createNewMetric(String name, String help);
    }
}