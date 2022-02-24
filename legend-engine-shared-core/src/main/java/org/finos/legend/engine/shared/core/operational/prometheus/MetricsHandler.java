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

package org.finos.legend.engine.shared.core.operational.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class MetricsHandler
{
    public static final String METRIC_PREFIX = "alloy_";
    private static final String[] empty  = new String[]{};
    static MutableMap<String, Summary> serviceMetrics = Maps.mutable.empty();
    static MutableMap<String, Gauge> serviceErrors = Maps.mutable.empty();
    static MutableMap<String , Gauge> gauges = Maps.mutable.empty();
    static MutableMap<String , Counter> errorCounters = Maps.mutable.empty();
    static final Gauge allExecutions = Gauge.build().name("alloy_executions").help("Execution gauge metric ").register();
    static final Gauge allExecutionErrors = Gauge.build().name("alloy_executions_errors").help("Execution error gauge metric ").register();

    // ----------------------------------------- NEW IMPLEMENTATION -----------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsHandler.class);

    private static final CollectorRegistry METRICS_REGISTRY = new CollectorRegistry(true);

    private static final Histogram SUCCESSFUL_REQUEST_LATENCY = Histogram.build().name("legend_engine_successful_request_latency")
            .help("Measure legend engine's http request latency")
            .buckets(.1, .2, .5, 1, 2, 5, 10, 30, 100, 300)
            .labelNames("uri")
            .register(getMetricsRegistry());

    private static final Histogram OPERATION_LATENCY = Histogram.build().name("legend_operation_latency")
            .help("Measure particular operation latency within legend ecosystem")
            .buckets(.1, .2, .5, 1, 2, 5, 10, 20, 60, 120)
            .labelNames("source", "operation", "context")
            .register(getMetricsRegistry());

    private static final Counter ALL_EXECUTIONS = Counter.build("legend_engine_executions", "Execution counter metric ").register();
    private static final Counter ERROR_COUNT = Counter.build("legend_engine_error_total", "Count errors within legend ecosystem").labelNames("operation", "status").register(getMetricsRegistry());
    private static final Counter DATASTORE_SPEC_COUNT = Counter.build("legend_engine_datastore_spec_count", "Count datastore specifications").register(getMetricsRegistry());
    private static final Counter JAVA_COMPILATION_COUNT = Counter.build("legend_engine_java_compilation_count", "Count java compilations").register(getMetricsRegistry());
    private static final Gauge TEMP_FILE_COUNT = Gauge.build("legend_engine_temp_file_count", "Measure how many temporary files are being currently created").register(getMetricsRegistry());

    public static CollectorRegistry getMetricsRegistry()
    {
        return METRICS_REGISTRY;
    }

    public static void observeRequest(String uri, long start, long end)
    {
        if (uri == null)
        {
            LOGGER.warn("Metric cannot be saved. Uri or method label values are missing");
        }
        else
        {
            SUCCESSFUL_REQUEST_LATENCY.labels(uri).observe((end - start) / 1000F);
        }
    }

    public static void observeServerOperation(String operation, String context, long start, long end)
    {
        observeOperation(operation, context, start, end);
    }

    private static void observeOperation(String operation, String context, long start, long end)
    {
        if (operation == null)
        {
            LOGGER.warn("Metric cannot be saved. Operation label value is missing");
        }
        else
        {
            OPERATION_LATENCY.labels(operation, returnLabelOrUnknown(context)).observe((end - start) / 1000F);
        }
    }

    public static void incrementExecutionCount()
    {
        ALL_EXECUTIONS.inc();
    }

    public static void incrementErrorCount(String operation, int code)
    {
        ERROR_COUNT.labels(returnLabelOrUnknown(operation), String.valueOf(code)).inc();
    }

    public static void incrementDatastoreSpecCount()
    {
        DATASTORE_SPEC_COUNT.inc();
    }

    public static void incrementJavaCompilationCount()
    {
        JAVA_COMPILATION_COUNT.inc();
    }

    public static void incrementTempFileCount()
    {
        TEMP_FILE_COUNT.inc();
    }

    public static void decrementTempFileCount()
    {
        TEMP_FILE_COUNT.dec();
    }

    private static String returnLabelOrUnknown(String label)
    {
        return label != null ? label : "unknown";
    }

    // -------------------------------------- END OF NEW IMPLEMENTATION -------------------------------------

    @Deprecated
    public static <T> void createMetrics(Class<T> c)
    {
        for (Method m : c.getMethods())
        {
            if (m.isAnnotationPresent(Prometheus.class))
            {
                Prometheus val = m.getAnnotation(Prometheus.class);
                if (val.type() == Prometheus.Type.SUMMARY && (serviceMetrics.get(val.name()) ==null))
                {
                    Summary g = Summary.build().name(generateMetricName(val.name(), false))
                                       .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                                       .help(val.doc())
                                       .register();
                    serviceMetrics.put(val.name(), g);
                }
            }
        }
    }

    @Deprecated
    public static void incrementExecutionGauge()
    {
        allExecutions.inc();
    }

    @Deprecated
    public static void incrementExecutionErrorGauge()
    {
        allExecutionErrors.inc();
    }

    @Deprecated
    public static synchronized void observe(String name, long startTime, long endTime)
    {
        if (serviceMetrics.get(name) == null)
        {
            Summary g = Summary.build().name(generateMetricName(name, false))
                               .quantile(0.5, 0.05).quantile(0.9, 0.01).quantile(0.99, 0.001)
                               .help(name + " duration metrics")
                               .register();
            serviceMetrics.put(name, g);
            g.observe((endTime - startTime) / 1000F);
        }
        else
        {
            serviceMetrics.get(name).observe((endTime - startTime) / 1000F);
        }
    }

    @Deprecated
    public static synchronized void observeCount(String name) {
        observeCount(name, empty,empty, false);
    }

    @Deprecated
    public static synchronized void decrementCount(String name) {
        observeCount(name, empty,empty, true);
    }

    @Deprecated
    public static synchronized void observeCount(String name, String[] labelNames, String[] labelValues, boolean decrement)
    {
        Gauge g;
        if (gauges.get(name) == null)
        {
            g = Gauge.build().name(generateMetricName(name,false))
                    .help(name + " gauge metric")
                    .labelNames(labelNames).register();
            gauges.put(name, g);
            if (decrement) {
                g.labels(labelValues).dec();
            } else {
                g.labels(labelValues).inc();
            }
        }
        else
        {
           g = gauges.get(name);
            if (decrement) {
                g.labels(labelValues).dec();
            } else {
                g.labels(labelValues).inc();
            }
        }
    }

    @Deprecated
    public static synchronized void observeErrorCount(String name) {
        observeErrorCount(name, empty,empty);
    }

    @Deprecated
    public static synchronized void observeErrorCount(String name,  String[] labelNames, String[] labelValues)
    {
        if (errorCounters.get(name) == null)
        {
            Counter c = Counter.build().name(generateMetricName(name,true))
                    .help(name+" count metric")
                    .labelNames(labelNames).register();
            errorCounters.put(name, c);
            c.labels(labelValues).inc();
        }
        else
        {
            errorCounters.get(name).labels(labelValues).inc();
        }
    }

    @Deprecated
    public static synchronized void observeError(String name)
    {
        if (serviceErrors.get(name) == null)
        {
           Gauge g = Gauge.build().name(generateMetricName(name, true)).help(name + "error gauge").register();
            serviceErrors.put(name, g);
            g.inc();
        }
        else
        {
            serviceErrors.get(name).inc();
        }
    }

    @Deprecated
    public static String generateMetricName(String name, boolean isErrorMetric)
    {
        return METRIC_PREFIX + name
                .replace("/", "_")
                .replace("-", "_")
                .replace("{", "")
                .replace("}", "")
                .replaceAll(" ", "_") + (isErrorMetric ? "_errors" : "");
    }
}
