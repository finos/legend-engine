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

package org.finos.legend.engine.query.graphQL.api.metrics;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.GRAPHQL_EXECUTE;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.exceptionToPrettyString;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.getCounterLabelValues;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.getMetricsRegistry;
import static org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.toCamelCase;

public class GraphQLMetricsHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLMetricsHandler.class);
    private static boolean categorisationEnabled = true;

    protected static final Histogram GRAPHQL_EXECUTION_LATENCY = Histogram.build().name("graphql_execution_latency")
            .help("Measure particular execution latency for a prod graphql query")
            .buckets(.1, .2, .5, 1, 2, 5, 10, 20, 60, 120)
            .labelNames("projectBasePath", "queryClassPath", "mappingClassPath", "runtimeClassPath", "dataSpacePath")
            .register(getMetricsRegistry());

    protected static final Counter GRAPHQL_EXECUTION_ERROR_COUNTER = Counter.build().name("graphql_execution_error_total")
            .help("Count errors in graphql executions")
            .labelNames("exceptionClass", "category", "source", "projectBasePath", "queryClassPath", "mappingClassPath", "runtimeClassPath", "dataSpacePath")
            .register(getMetricsRegistry());

    public static synchronized void setCategorisationEnabled(boolean flag)
    {
        categorisationEnabled = flag;
        LOGGER.info("Exception categorisation in error handling has been set to {}", flag);
    }


    public static void observeGraphqlDevExecution(String groupId, String workspaceId, String queryClassPath, String mappingClassPath, String runtimeClassPath, long start, long end)
    {
        observeLatency(start, end, getGraphQLDevProjectBasePath(groupId, workspaceId), queryClassPath, mappingClassPath, runtimeClassPath, "NA");
    }

    public static void observeGraphqlProdExecution(String groupId, String artifactId, String versionId, String queryClassPath, String mappingClassPath, String runtimeClassPath, long start, long end)
    {
        observeLatency(start, end, getGraphQLProdProjectBasePath(groupId, artifactId, versionId), queryClassPath, mappingClassPath, runtimeClassPath, "NA");
    }

    public static void observeGraphqlProdExecutionWithDataSpace(String groupId, String artifactId, String versionId, String queryClassPath, String dataSpacePath, long start, long end)
    {
        observeLatency(start, end, getGraphQLProdProjectBasePath(groupId, artifactId, versionId), queryClassPath, "NA", "NA", dataSpacePath);
    }

    public static void observeGraphqlDevError(Exception ex, String groupId, String workspaceId, String queryClassPath, String mappingClassPath, String runtimeClassPath)
    {
        observeError(ex, getGraphQLDevProjectBasePath(groupId, workspaceId), queryClassPath, mappingClassPath, runtimeClassPath, "NA");
    }

    public static void observeGraphqlProdError(Exception ex, String groupId, String artifactId, String versionId, String queryClassPath, String mappingClassPath, String runtimeClassPath)
    {
        observeError(ex, getGraphQLProdProjectBasePath(groupId, artifactId, versionId), queryClassPath, mappingClassPath, runtimeClassPath, "NA");
    }

    public static void observeGraphqlProdErrorWithDataSpace(Exception ex, String groupId, String artifactId, String versionId, String queryClassPath, String dataSpacePath)
    {
        observeError(ex, getGraphQLProdProjectBasePath(groupId, artifactId, versionId), queryClassPath, "NA", "NA", dataSpacePath);
    }


    private static void observeLatency(long start, long end, String... labels)
    {
        GRAPHQL_EXECUTION_LATENCY.labels(processLabels(labels)).observe((end - start) / 1000F);
    }

    private static String[] processLabels(String... labels)
    {
        List<String> processedLabels = new ArrayList<>();
        for (String label : labels)
        {
            processedLabels.add(returnLabelOrUnknown(label));
        }
        return processedLabels.toArray(new String[0]);
    }

    private static String returnLabelOrUnknown(String label)
    {
        return label != null ? label : "unknown";
    }

    private static String getGraphQLDevProjectBasePath(String groupId, String workspaceId)
    {
        return String.format("%s:%s", groupId, workspaceId);
    }

    private static String getGraphQLProdProjectBasePath(String groupId, String artifactId, String versionId)
    {
        return String.format("%s:%s:%s", groupId, artifactId, versionId);
    }

    /**
     * Sourced from {@link MetricsHandler#observeError(Enum, Exception, String)}
     */
    private static synchronized void observeError(Exception exception, String projectBasePath, String queryClassPath, String mappingClassPath, String runtimeClassPath, String dataSpacePath)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Error Categorisation").startActive(true))
        {
            String source = removeErrorSuffix(toCamelCase(GRAPHQL_EXECUTE));
            String exceptionClass = exception.getClass().getSimpleName();
            ExceptionCategory category = ExceptionCategory.UNKNOWN_ERROR;
            if (categorisationEnabled)
            {
                MetricsHandler.ExceptionLabelValues exceptionLabelValues = getCounterLabelValues(exception);
                exceptionClass = exceptionLabelValues.exceptionClass;
                category = exceptionLabelValues.exceptionCategory;
            }
            GRAPHQL_EXECUTION_ERROR_COUNTER.labels(exceptionClass, toCamelCase(category), source, projectBasePath, queryClassPath, mappingClassPath, runtimeClassPath, dataSpacePath).inc();
            LOGGER.error("Exception added to metric - Label: {}. Category: {}. Source: {}. " +
                            "projectBasePath: {}. queryClassPath: {}. mappingClassPath: {}. runtimeClassPath: {}. dataSpacePath: {}. " +
                            "{}. Exception Categorisation is: {}",
                    exceptionClass, category, source,
                    projectBasePath, queryClassPath, mappingClassPath, runtimeClassPath, dataSpacePath,
                    exceptionToPrettyString(exception), categorisationEnabled
            );
        }
    }

    private static String removeErrorSuffix(String string)
    {
        return string.endsWith("Error") ? string.substring(0, string.lastIndexOf("Error")) : string;
    }

}
