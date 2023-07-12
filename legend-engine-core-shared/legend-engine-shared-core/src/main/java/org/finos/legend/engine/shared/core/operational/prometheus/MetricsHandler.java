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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategoryData;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class MetricsHandler
{
    public static final String METRIC_PREFIX = "alloy_";
    private static final String[] empty = new String[]{};
    static MutableMap<String, Summary> serviceMetrics = Maps.mutable.empty();
    static MutableMap<String, Gauge> gauges = Maps.mutable.empty();
    static final Gauge allExecutions = Gauge.build().name("alloy_executions").help("Execution gauge metric ").register();

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
            .labelNames("operation", "context")
            .register(getMetricsRegistry());

    private static final Counter ALL_EXECUTIONS = Counter.build("legend_engine_executions", "Execution counter metric ").register();
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
                if (val.type() == Prometheus.Type.SUMMARY && (serviceMetrics.get(val.name()) == null))
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
    public static synchronized void observeCount(String name)
    {
        observeCount(name, empty, empty, false);
    }

    @Deprecated
    public static synchronized void decrementCount(String name)
    {
        observeCount(name, empty, empty, true);
    }

    @Deprecated
    public static synchronized void observeCount(String name, String[] labelNames, String[] labelValues, boolean decrement)
    {
        Gauge g;
        if (gauges.get(name) == null)
        {
            g = Gauge.build().name(generateMetricName(name, false))
                    .help(name + " gauge metric")
                    .labelNames(labelNames).register();
            gauges.put(name, g);
            if (decrement)
            {
                g.labels(labelValues).dec();
            }
            else
            {
                g.labels(labelValues).inc();
            }
        }
        else
        {
            g = gauges.get(name);
            if (decrement)
            {
                g.labels(labelValues).dec();
            }
            else
            {
                g.labels(labelValues).inc();
            }
        }
    }

    // -------------------------------------- ERROR HANDLING -------------------------------------

    /**
     * Path to JSON file outlining error data for categorisation available to open source.
     */
    private static final String EXCEPTION_DATA_PATH = "/ExceptionData.json";

    /**
     * Prometheus counter to record exceptions with labels of the service causing the exception if it is a service-related exception,
     * the label given to the exception, the category of the exception and source of the exception.
     */
    protected static final Counter EXCEPTION_ERROR_COUNTER = Counter.build("legend_engine_error_total", "Count errors in legend engine").labelNames("exceptionClass", "category", "source", "serviceName").register(getMetricsRegistry());

    /**
     * List of objects corresponding to the error categories holding their associated exception data.
     */
    private static final List<ExceptionCategoryData> EXCEPTION_CATEGORY_DATA = readExceptionData();

    /**
     * Flag to turn exception categorisation on and off.
     */
    private static boolean categorisationEnabled = true;

    /**
     * Types of exception matching priorities that can be performed on an incoming exceptions.
     */
    public enum MatchingPriority
    {
        @JsonProperty("primary")
        PRIMARY,
        @JsonProperty("secondary")
        SECONDARY,
    }

    /**
     * Method to record an exception occurring during execution and add it to the metrics.
     * @param origin the stage in execution at which the exception occurred. For service execution exceptions use SERVICE_EXECUTE_ERROR.
     * @param exception the non-null exception to be analysed that has occurred in execution.
     * @param servicePath the name of the service whose execution invoked the error.
     */
    public static synchronized void observeError(Enum origin, Exception exception, String servicePath)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Error Categorisation").startActive(true))
        {
            Assert.assertTrue(origin != null, () -> "Exception origin must not be null!");
            String source = removeErrorSuffix(toCamelCase(origin));
            String servicePattern = servicePath == null ? "N/A" : servicePath;
            String exceptionClass = exception.getClass().getSimpleName();
            ExceptionCategory category = ExceptionCategory.UNKNOWN_ERROR;
            if (categorisationEnabled)
            {
                ExceptionLabelValues exceptionLabelValues = getCounterLabelValues(exception);
                exceptionClass = exceptionLabelValues.exceptionClass;
                category = exceptionLabelValues.exceptionCategory;
            }
            EXCEPTION_ERROR_COUNTER.labels(exceptionClass, toCamelCase(category), source, servicePattern).inc();
            LOGGER.error("Exception added to metric - Label: {}. Category: {}. Source: {}. Service: {}. {}. Exception Categorisation is: {}", exceptionClass, category, source, servicePattern, exceptionToPrettyString(exception), categorisationEnabled);
        }
    }

    /**
     * Method to loop down the cause trace of the exception to classify the exception to a category and extract its exception label.
     * If the data cannot be extracted from the original exception, its cause it iteratively analysed.
     * @param exception the original exception to be analysed that has occurred in execution.
     * @return a pair of values corresponding to the exceptionLabel and category labels in the Counter.
     */
    private static synchronized ExceptionLabelValues getCounterLabelValues(Throwable exception)
    {
        int categorisationDepthLimit = 5;
        ExceptionLabelValues exceptionLabelValues = new ExceptionLabelValues(getExceptionClass(exception), ExceptionCategory.UNKNOWN_ERROR);
        for (int depth = 0; depth < categorisationDepthLimit && exception != null; depth++)
        {
            if (isEngineExceptionWithValidExceptionCategory(exception))
            {
                exceptionLabelValues.exceptionCategory = ((EngineException) exception).getErrorCategory();
                exceptionLabelValues.exceptionClass = getExceptionClass(exception);
                return exceptionLabelValues;
            }
            else
            {
                ExceptionCategory matchedCategory = matchExceptionToExceptionDataFile(exception);
                if (matchedCategory != ExceptionCategory.UNKNOWN_ERROR)
                {
                    exceptionLabelValues.exceptionCategory = matchedCategory;
                    exceptionLabelValues.exceptionClass = getExceptionClass(exception);
                    return exceptionLabelValues;
                }
            }
            exception = exception.getCause();
        }
        return exceptionLabelValues;
    }

    /**
     * Method to obtain the exception class from an exception
     * Prefixes the exception with the type if it is an engine exception
     * @param exception is the exception whose class to obtain
     * @return error counter label exceptionClass' value.
     */
    private static synchronized String getExceptionClass(Throwable exception)
    {
        String prefix = exception instanceof EngineException ? toCamelCase(((EngineException) exception).getErrorType()) : "";
        return prefix + exception.getClass().getSimpleName();
    }

    /**
     * Method to check if an exception is an engine exception that has a nonnull and unknown category
     * @param exception is the exception to be checked
     * @return true if the exception has an associated valid category and false otherwise.
     */
    private static synchronized boolean isEngineExceptionWithValidExceptionCategory(Throwable exception)
    {
        if (exception instanceof EngineException)
        {
            EngineException engineException = (EngineException) exception;
            return engineException.getErrorCategory() != ExceptionCategory.UNKNOWN_ERROR && engineException.getErrorCategory() != null;
        }
        return false;
    }

    /**
     * Method to try and match an exception to an exception category using the matching methods.
     * @param exception is the exception that occurred in the engine.
     * @return Category belonging to the exception.
     */
    private static synchronized ExceptionCategory matchExceptionToExceptionDataFile(Throwable exception)
    {
        for (MatchingPriority method : MatchingPriority.values())
        {
            for (ExceptionCategoryData categoryData : EXCEPTION_CATEGORY_DATA)
            {
                if (categoryData.matches(exception, method))
                {
                    return categoryData.getExceptionCategory();
                }
            }
        }
        return ExceptionCategory.UNKNOWN_ERROR;
    }

    /**
     * Find and read JSON file with outline of exceptions to be used in categorizing incoming exceptions
     * @return List of objects corresponding to the exception categories with their respective data
     */
    private static synchronized List<ExceptionCategoryData> readExceptionData()
    {
        List<ExceptionCategoryData> categories;
        try (InputStream inputStream = MetricsHandler.class.getResourceAsStream(EXCEPTION_DATA_PATH))
        {
            categories = Arrays.asList(new ObjectMapper().readValue(inputStream, ExceptionCategoryData[].class));
            LOGGER.info("Successfully read exception data from {}.", MetricsHandler.class.getResource(EXCEPTION_DATA_PATH));
        }
        catch (Exception e)
        {
                LOGGER.warn("Error reading exception categorisation data: {}", exceptionToPrettyString(e));
                EngineException engineException = new EngineException("Cannot read exception data file properly", e, ExceptionCategory.INTERNAL_SERVER_ERROR);
                observeError(LoggingEventType.ERROR_MANAGEMENT_ERROR, engineException, null);
                throw engineException;
        }
        return categories;
    }

    /**
     * Method to turn exception categorisation on and off
     * @param flag is true to set categorisation on and false otherwise.
     */
    public static synchronized void setCategorisationEnabled(boolean flag)
    {
        categorisationEnabled = flag;
        LOGGER.info("Exception categorisation in error handling has been set to {}", flag);
    }

    // -------------------------------------- STRING UTILS -------------------------------------

    /**
     * Method to convert a snake case enum value to camel case for pretty printing for metrics
     * @param value NonNull enum value to be converted
     * @return camelCase string of enum value
     */
    public static String toCamelCase(Enum value)
    {
        if (value == null)
        {
            return "";
        }
        String snakeCaseString = value.toString();
        String[] elements = snakeCaseString.toLowerCase().split("_");
        StringBuilder output = new StringBuilder();
        Arrays.stream(elements).forEach(element -> output.append(element.substring(0, 1).toUpperCase()).append(element.substring(1)));
        return output.toString();
    }

    /**
     * Method to delete the suffix "Error" from a string if it exists.
     * @param string the string whose suffix to remove
     * @return string without the suffix "Error" if applicable.
     */
    private static String removeErrorSuffix(String string)
    {
        return string.endsWith("Error") ? string.substring(0, string.lastIndexOf("Error")) : string;
    }

    /**
     * Method to pretty print an exception with the purpose of adding it to the error data file
     * @param exception is the exception to be pretty printed
     * @return pretty print formatted exception string
     */
    private static String exceptionToPrettyString(Exception exception)
    {
        String name = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        String cause = exception.getCause() == null ? "None" : exception.getCause().toString();
        return String.format("Exception: %s. Message: %s. Cause: %s", name, message, cause);
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

    /**
     * Class to represent a tuple of label values to be used in the Error Prometheus Counter.
     */
    private static class ExceptionLabelValues
    {
        /**
         * exceptionLabel label value for the prometheus error counter
         */
        public String exceptionClass;

        /**
         * category label value for the prometheus error counter
         */
        public ExceptionCategory exceptionCategory;

        /**
         * Constructor to create a pair of exception-related label values.
         * @param exceptionClass is the simple name of the class causing the exception.
         * @param exceptionCategory is the category label value for the prometheus error counter.
         */
        public ExceptionLabelValues(String exceptionClass, ExceptionCategory exceptionCategory)
        {
            this.exceptionClass = exceptionClass;
            this.exceptionCategory = exceptionCategory;
        }
    }


    // ---------------------------------- REDUNDANT CODE FOR BACKWARD COMPATIBILITY ---------------------------------

    /**
     * Redundant code, kept temporarily for backwards compatibility.
     * To be deleted.
     */

    @Deprecated
    public static void incrementErrorCount(String operation, int code)
    {
    }

    @Deprecated
    public static void incrementExecutionErrorGauge()
    {
    }

    @Deprecated
    public static synchronized void observeErrorCount(String name)
    {
    }

    @Deprecated
    public static synchronized void observeErrorCount(String name, String[] labelNames, String[] labelValues)
    {
    }

    @Deprecated
    public static synchronized void observeError(String name)
    {
        EXCEPTION_ERROR_COUNTER.labels(name, toCamelCase(ExceptionCategory.UNKNOWN_ERROR), toCamelCase(LoggingEventType.CATCH_ALL), "N/A").inc();
    }
}
