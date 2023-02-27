// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.result;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.util.EntityUtils;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.ElasticsearchExecutionLoggingEventType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.Elasticsearch7RequestExecutionNode;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSColumnResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSMetadata;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.BulkRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.closepointintime.ClosePointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.count.CountRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.index.IndexRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.openpointintime.OpenPointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.ResponseBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.Hit;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.delete.DeleteRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.get.GetRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBaseVisitor;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.slf4j.Logger;

public class ExecutionRequestVisitor implements RequestBaseVisitor<Result>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final HttpClient client;
    private final HttpClientContext httpClientContext;
    private final HttpUriRequest request;
    private final Elasticsearch7RequestExecutionNode node;
    private final ExecutionState executionState;

    public ExecutionRequestVisitor(HttpClient client, HttpClientContext httpClientContext, HttpUriRequest request, Elasticsearch7RequestExecutionNode esNode,  ExecutionState executionState)
    {
        this.client = client;
        this.httpClientContext = httpClientContext;
        this.request = request;
        this.node = esNode;
        this.executionState = executionState;
    }

    @Override
    public Result visit(BulkRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(ClosePointInTimeRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(CountRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(CreateRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(DeleteRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(GetRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(IndexRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(OpenPointInTimeRequest val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(SearchRequest val)
    {
        Assert.assertTrue(ExecutionNodeTDSResultHelper.isResultTDS(this.node), () -> "ES only supports TDS result");

        // todo handle aggregations
        // todo handle pagination?

        boolean isAggregation = !val.body.aggregations.isEmpty();

        Assert.assertFalse(isAggregation, () -> "Aggregations not supported yet");

        String query = ((HttpEntityEnclosingRequest) this.request).getEntity().toString();

        Span span = GlobalTracer.get().buildSpan("Elasticsearch Request").start();
        try (Scope ignore = GlobalTracer.get().activateSpan(span))
        {
            span.log(Collections.singletonMap("query", query));

            long start = System.currentTimeMillis();
            LOGGER.info("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_START, this.executionState.authId, query));

            InputStream result = this.post(span, start);

            Stream<Object[]> stream = this.processResponse(result);

            return new TDSResult(
                    stream,
                    new TDSBuilder(this.node),
                    Collections.singletonList(new ElasticsearchV7ExecutionActivity(this.request.getURI(), query)),
                    this.executionState.getSessionID()
            );
        }
        catch (IOException e)
        {
            throw new EngineException(e.getMessage(), e, ExceptionCategory.USER_EXECUTION_ERROR);
        }
        finally
        {
            span.finish();
        }
    }

    private Stream<Object[]> processResponse(InputStream result) throws IOException
    {
        ResponseBody<JsonNode> responseBody = new ResponseBody<>();
        Stream<Hit<JsonNode>> rows = ElasticsearchTDSResultHelper.tdsFromHits(result, responseBody);

        List<TDSColumn> tdsColumns = ((TDSResultType) this.node.resultType).tdsColumns;
        List<TDSColumnResultPath> columnResultPaths = ((TDSMetadata) node.metadata).columnResultPaths;

        List<Function<Hit<JsonNode>, Object>> extractors = columnResultPaths.stream()
                .map(x -> ElasticsearchTDSResultHelper.hitTransformer(tdsColumns.get((int) x.index), x.resultPath))
                .collect(Collectors.toList());

        return rows.map(h -> extractors.stream().map(x -> x.apply(h)).toArray());
    }

    private InputStream post(Span span, long startTime) throws IOException
    {
        this.request.addHeader("X-Opaque-Id", String.format("alloy-exec-%s", this.executionState.execID));
        this.request.addHeader("traceparent", String.format("00-%s-%s-01", span.context().toTraceId(), span.context().toSpanId()));

        HttpResponse httpResponse = this.client.execute(this.request, this.httpClientContext);

        if (httpResponse.getStatusLine().getStatusCode() > 299)
        {
            String error = EntityUtils.toString(httpResponse.getEntity());
            span.setTag(Tags.ERROR, true);
            Map<String, Object> errorLogs = new HashMap<>(2);
            errorLogs.put("event", Tags.ERROR.getKey());
            errorLogs.put("error.object", error);
            span.log(errorLogs);

            LOGGER.error("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_STOP_ERROR, this.executionState.authId, error, System.currentTimeMillis() - startTime));

            throw new EngineException(
                    String.format("ES operation failed.  URL: %s, Status Line: %s, Response: %s", this.request.getURI(), httpResponse.getStatusLine(), error),
                    ExceptionCategory.USER_EXECUTION_ERROR);
        }
        else
        {
            LOGGER.info("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_STOP, this.executionState.authId, null, System.currentTimeMillis() - startTime));
            return httpResponse.getEntity().getContent();
        }
    }
}
