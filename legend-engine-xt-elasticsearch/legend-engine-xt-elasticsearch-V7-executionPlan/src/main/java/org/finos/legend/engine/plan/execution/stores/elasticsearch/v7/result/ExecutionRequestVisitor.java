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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.ElasticsearchExecutionLoggingEventType;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.http.ElasticsearchV7RequestToHttpRequestVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.Elasticsearch7RequestExecutionNode;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSColumnResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSMetadata;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.ElasticsearchObjectMapperProvider;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.ResponseBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.Hit;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.AbstractRequestBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.FieldValue;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AbstractAggregateBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.Aggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AggregateBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AvgAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.CompositeAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.SumAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.ValueCountAggregate;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.slf4j.Logger;

public class ExecutionRequestVisitor extends AbstractRequestBaseVisitor<Result>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final TypeReference<ResponseBody<ObjectNode>> RESPONSE_BODY_TYPE_REFERENCE = new TypeReference<ResponseBody<ObjectNode>>()
    {
    };

    private final HttpClient client;
    private final HttpClientContext httpClientContext;
    private final Elasticsearch7RequestExecutionNode node;
    private final ExecutionState executionState;
    private final URI url;

    public ExecutionRequestVisitor(HttpClient client, HttpClientContext httpClientContext, URI url, Elasticsearch7RequestExecutionNode esNode, ExecutionState executionState)
    {
        this.client = client;
        this.httpClientContext = httpClientContext;
        this.url = url;
        this.node = esNode;
        this.executionState = executionState;
    }

    @Override
    protected Result defaultValue(RequestBase val)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result visit(SearchRequest val)
    {
        Assert.assertTrue(ExecutionNodeTDSResultHelper.isResultTDS(this.node), () -> "ES only supports TDS result");

        boolean isAggregation = !val.body.aggregations.isEmpty();

        HttpUriRequest request = val.accept(new ElasticsearchV7RequestToHttpRequestVisitor(this.url));
        String query = ((HttpEntityEnclosingRequest) request).getEntity().toString();

        Span span = GlobalTracer.get().buildSpan("Elasticsearch Request").start();
        try (Scope ignore = GlobalTracer.get().activateSpan(span))
        {
            span.log(Collections.singletonMap("query", query));

            long start = System.currentTimeMillis();
            LOGGER.info("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_START, this.executionState.authId, query));

            InputStream result = this.post(request, span, start);

            // todo handle pagination since this streams all data into memory...
            ResponseBody<ObjectNode> responseBody = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.readValue(result, RESPONSE_BODY_TYPE_REFERENCE);

            Stream<Object[]> stream = isAggregation ? this.processAggregateResponse(responseBody) : this.processNotAggregateResponse(responseBody);

            return new TDSResult(
                    stream,
                    new TDSBuilder(this.node),
                    Collections.singletonList(new ElasticsearchV7ExecutionActivity(request.getURI(), query)),
                    RequestContext.getSessionID(this.executionState.getRequestContext())
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

    private Stream<Object[]> processAggregateResponse(ResponseBody<?> responseBody) throws IOException
    {
        AggregateTDSResultVisitor aggregateTDSResultVisitor = new AggregateTDSResultVisitor();
        List<TDSColumn> tdsColumns = ((TDSResultType) this.node.resultType).tdsColumns;
        List<TDSColumnResultPath> columnResultPaths = ((TDSMetadata) node.metadata).columnResultPaths;

        List<Function<ObjectNode, Object>> extractors = ListIterate.zip(tdsColumns, columnResultPaths).stream()
                .map(ElasticsearchTDSResultHelper::aggregationTransformer)
                .collect(Collectors.toList());

        Stream<ObjectNode> objectNodeStream = null;

        if (responseBody.aggregations.size() == 1)
        {
            Map.Entry<String, Aggregate> topAggEntry = responseBody.aggregations.entrySet().iterator().next();
            Object result = topAggEntry.getValue().unionValue();
            if (result instanceof CompositeAggregate)
            {
                CompositeAggregate compositeAggregate = (CompositeAggregate) result;
                Assert.assertTrue(compositeAggregate.buckets.keyed.isEmpty(), () -> "Keyed buckets not supported");

                objectNodeStream = compositeAggregate.buckets.array.stream().map(b ->
                {
                    try (TokenBuffer tokenBuffer = new TokenBuffer(ElasticsearchObjectMapperProvider.OBJECT_MAPPER, false))
                    {
                        for (Map.Entry<String, FieldValue> entry : b.key.entrySet())
                        {
                            tokenBuffer.writeObjectField(entry.getKey(), entry.getValue().unionValue());
                        }

                        for (Map.Entry<String, Aggregate> entry : b.__additionalProperties.entrySet())
                        {
                            tokenBuffer.writeObjectField(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
                        }

                        return tokenBuffer.asParser().readValueAsTree();
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }

        if (objectNodeStream == null)
        {
            try (TokenBuffer tokenBuffer = new TokenBuffer(ElasticsearchObjectMapperProvider.OBJECT_MAPPER, false))
            {
                for (Map.Entry<String, Aggregate> entry : responseBody.aggregations.entrySet())
                {
                    tokenBuffer.writeObjectField(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
                }

                objectNodeStream = Stream.of(tokenBuffer.asParser().readValueAsTree());
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        return objectNodeStream.map(h -> extractors.stream().map(x -> x.apply(h)).toArray());
    }

    private Stream<Object[]> processNotAggregateResponse(ResponseBody<ObjectNode> responseBody) throws IOException
    {
        List<TDSColumn> tdsColumns = ((TDSResultType) this.node.resultType).tdsColumns;
        List<TDSColumnResultPath> columnResultPaths = ((TDSMetadata) node.metadata).columnResultPaths;

        List<Function<Hit<ObjectNode>, Object>> extractors = columnResultPaths.stream()
                .map(x -> ElasticsearchTDSResultHelper.hitTransformer(tdsColumns.get((int) x.index), x.resultPath))
                .collect(Collectors.toList());

        return responseBody.hits.hits.stream().map(h -> extractors.stream().map(x -> x.apply(h)).toArray());
    }

    private InputStream post(HttpUriRequest request, Span span, long startTime) throws IOException
    {
        request.addHeader("X-Opaque-Id", String.format("alloy-exec-%s", this.executionState.execID));
        request.addHeader("traceparent", String.format("00-%s-%s-01", span.context().toTraceId(), span.context().toSpanId()));

        HttpResponse httpResponse = this.client.execute(request, this.httpClientContext);

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
                    String.format("ES operation failed.  URL: %s, Status Line: %s, Response: %s", request.getURI(), httpResponse.getStatusLine(), error),
                    ExceptionCategory.USER_EXECUTION_ERROR);
        }
        else
        {
            LOGGER.info("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_STOP, this.executionState.authId, null, System.currentTimeMillis() - startTime));
            return httpResponse.getEntity().getContent();
        }
    }

    private static class AggregateTDSResultVisitor extends AbstractAggregateBaseVisitor<Object>
    {
        @Override
        protected Object defaultValue(AggregateBase val)
        {
            throw new UnsupportedOperationException(val.getClass() + " not supported");
        }

        @Override
        public Object visit(AvgAggregate val)
        {
            return val.value;
        }

        @Override
        public Object visit(SumAggregate val)
        {
            return val.value;
        }

        @Override
        public Object visit(ValueCountAggregate val)
        {
            return val.value;
        }
    }
}
