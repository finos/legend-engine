// Copyright 2022 Goldman Sachs
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

import io.prometheus.client.CollectorRegistry;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.junit.After;
import org.junit.Test;

import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlDevError;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlDevExecution;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdError;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdErrorWithDataSpace;
import static org.finos.legend.engine.query.graphQL.api.metrics.GraphQLMetricsHandler.observeGraphqlProdExecution;
import static org.junit.Assert.assertEquals;

public class GraphQLMetricsHandlerTest
{

    private final CollectorRegistry METRIC_REGISTRY = MetricsHandler.getMetricsRegistry();
    private final double DELTA = 0d;

    private final String[] ERROR_COUNTER_LABEL_NAMES = {"exceptionClass", "category", "source", "projectBasePath", "queryClassPath", "mappingClassPath", "runtimeClassPath", "dataSpacePath"};
    private final String[] LATENCY_COUNTER_LABEL_NAMES = {"projectBasePath", "queryClassPath", "mappingClassPath", "runtimeClassPath", "dataSpacePath"};
    private final String[] LATENCY_BUCKET_LABEL_NAMES = {"projectBasePath", "queryClassPath", "mappingClassPath", "runtimeClassPath", "dataSpacePath", "le"};
    @After()
    public void clearCounterData()
    {
        GraphQLMetricsHandler.GRAPHQL_EXECUTION_LATENCY.clear();
        GraphQLMetricsHandler.GRAPHQL_EXECUTION_ERROR_COUNTER.clear();
    }

    @Test
    public void testGraphqlDevExecutionLatency(){
        long start = System.currentTimeMillis();
        observeGraphqlDevExecution("group", "workspace", "query", "mapping", "runtime", start, start + 100);
        observeGraphqlDevExecution("group", "workspace", "query", "mapping", "runtime", start, start + 1_000);
        observeGraphqlDevExecution("group", "workspace", "query", "mapping", "runtime", start, start + 5_000);
        observeGraphqlDevExecution("group", "workspace", "query", "mapping", "runtime", start, start + 60_000);
        String[] labels = {"group:workspace", "query", "mapping", "runtime", "NA"};

        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_sum", LATENCY_COUNTER_LABEL_NAMES, labels), 66.10, 0.01);
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_count", LATENCY_COUNTER_LABEL_NAMES, labels), 4, 0);

        assertLatencyBucketCountForDev("0.2", 1.0);
        assertLatencyBucketCountForDev("1.0", 2.0);
        assertLatencyBucketCountForDev("2.0", 2.0);
        assertLatencyBucketCountForDev("5.0", 3.0);
        assertLatencyBucketCountForDev("60.0", 4.0);
        assertLatencyBucketCountForDev("+Inf", 4.0);
    }

    @Test
    public void testGraphqlProdExecutionLatency(){
        long start = System.currentTimeMillis();
        observeGraphqlProdExecution("group", "artifact", "version", "query", "mapping", "runtime", start, start + 100);
        observeGraphqlProdExecution("group", "artifact", "version", "query", "mapping", "runtime", start, start + 1_000);
        observeGraphqlProdExecution("group", "artifact", "version", "query", "mapping", "runtime", start, start + 5_000);
        observeGraphqlProdExecution("group", "artifact", "version", "query", "mapping", "runtime", start, start + 60_000);
        String[] labels = {"group:artifact:version", "query", "mapping", "runtime", "NA"};

        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_sum", LATENCY_COUNTER_LABEL_NAMES, labels), 66.10, 0.01);
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_count", LATENCY_COUNTER_LABEL_NAMES, labels), 4, 0);

        assertLatencyBucketCountForProd("0.2", 1.0);
        assertLatencyBucketCountForProd("1.0", 2.0);
        assertLatencyBucketCountForProd("2.0", 2.0);
        assertLatencyBucketCountForProd("5.0", 3.0);
        assertLatencyBucketCountForProd("60.0", 4.0);
        assertLatencyBucketCountForProd("+Inf", 4.0);
    }

    @Test
    public void testGraphqlDevErrorForGenericException()
    {
        observeGraphqlDevError(new Exception(), "groupId", "workspace", "query", "mapping", "runtime");
        String[] labels = {"Exception", "UnknownError", "GraphqlExecute", "groupId:workspace", "query", "mapping", "runtime", "NA"};
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_error_total", ERROR_COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }
    @Test
    public void testGraphqlProdErrorForConnectionException()
    {
        observeGraphqlProdError(new ConnectionException(new Exception("error")), "groupId", "artifact", "version", "query", "mapping", "runtime");
        String[] labels = {"ConnectionException", "InternalServerError", "GraphqlExecute", "groupId:artifact:version", "query", "mapping", "runtime", "NA"};
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_error_total", ERROR_COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }
    @Test
    public void testGraphqlProdExecutionWithDataSpaceForNestedException()
    {
        RuntimeException nestedOtherErrorException = new RuntimeException(new java.net.SocketTimeoutException("socket timeout"));
        observeGraphqlProdErrorWithDataSpace(new Exception(nestedOtherErrorException), "groupId", "artifact", "version", "query", "dataSpace");
        String[] labels = {"Exception", "InternalServerError", "GraphqlExecute", "groupId:artifact:version", "query", "NA", "NA", "dataSpace"};
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_error_total", ERROR_COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }
    @Test
    public void testGraphqlDevErrorForServingExecutionCategory()
    {
        observeGraphqlDevError(new Exception("Error in 'some::graph': Couldn't resolve something"), "groupId", "workspace", "query", "mapping", "runtime");
        String[] labels = {"Exception", "ServerExecutionError", "GraphqlExecute", "groupId:workspace", "query", "mapping", "runtime", "NA"};
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_error_total", ERROR_COUNTER_LABEL_NAMES, labels), 1, DELTA);
    }

    private void assertLatencyBucketCountForDev(String bucketLabel, Double bucketVal){
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_bucket", LATENCY_BUCKET_LABEL_NAMES,
                new String[] {"group:workspace", "query", "mapping", "runtime", "NA", bucketLabel}), bucketVal, 0);
    }
    private void assertLatencyBucketCountForProd(String bucketLabel, Double bucketVal){
        assertEquals(METRIC_REGISTRY.getSampleValue("graphql_execution_latency_bucket", LATENCY_BUCKET_LABEL_NAMES,
                new String[] {"group:artifact:version", "query", "mapping", "runtime", "NA", bucketLabel}), bucketVal, 0);
    }

}
