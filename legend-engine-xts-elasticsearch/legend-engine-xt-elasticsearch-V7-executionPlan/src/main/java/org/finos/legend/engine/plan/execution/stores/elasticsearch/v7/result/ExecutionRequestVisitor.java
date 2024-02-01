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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.JsonPointerBasedFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction2;
import org.eclipse.collections.impl.lazy.iterator.CollectIterator;
import org.eclipse.collections.impl.lazy.iterator.FlatCollectIterator;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.ElasticsearchExecutionLoggingEventType;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.http.ElasticsearchV7RequestToHttpRequestVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.store.elasticsearch.specification.utils.ExternalTaggedUnionMap;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.Elasticsearch7RequestExecutionNode;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSColumnResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSMetadata;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.AggregateResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.DocCountAggregateResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.ElasticsearchObjectMapperProvider;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.Hit;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.TotalHits;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.TotalHitsRelation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.AbstractRequestBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.FieldValue;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.RequestBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AbstractAggregateBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AbstractMultiBucketBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.Aggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AggregateBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AggregationContainer;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.AvgAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.Buckets;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.CompositeBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.DoubleTermsBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.FiltersAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.FiltersBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.LongTermsBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MaxAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MinAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MultiBucketBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.MultiTermsBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.StringTermsBucket;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.SumAggregate;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.TermsBucketBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations.ValueCountAggregate;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.slf4j.Logger;

public class ExecutionRequestVisitor extends AbstractRequestBaseVisitor<Result>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExecutionRequestVisitor.class);

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

        Span span = GlobalTracer.get().buildSpan("Elasticsearch Request").start();
        Scope scope = GlobalTracer.get().activateSpan(span);

        List<ExecutionActivity> activities = Lists.mutable.empty();

        ElasticsearchResultSpliterator spliterator = new ElasticsearchResultSpliterator(val, activities);

        Stream<Object[]> stream = StreamSupport.stream(spliterator, false)
                .flatMap(Function.identity())
                .onClose(spliterator::close)
                .onClose(scope::close)
                .onClose(span::finish);

        return new TDSResult(
                stream,
                new TDSBuilder(this.node),
                activities,
                RequestContext.getSessionID(this.executionState.getRequestContext())
        );
    }

    private Iterator<Object[]> processAggregateResponse(SearchRequest searchRequest, JsonParser parser, Span span, Procedure<MultiBucketBase> lastBucket) throws IOException
    {
        Map<String, AggregationContainer> aggregations = searchRequest.body.aggregations;

        // extract total hits
        FilteringParserDelegate totalHitsParse = new FilteringParserDelegate(parser, new JsonPointerBasedFilter("/hits/total"), false, false);
        TotalHits totalHits = totalHitsParse.readValueAs(TotalHits.class);
        span.log(String.format("Aggregation query hit %s %d documents", totalHits.relation, totalHits.value.getLiteral()));

        // reset parser back to root after reading totals
        while (!parser.getParsingContext().getParent().inRoot())
        {
            parser.nextToken();
        }

        AggregateTDSResultVisitor aggregateTDSResultVisitor = new AggregateTDSResultVisitor(totalHits);
        List<TDSColumn> tdsColumns = ((TDSResultType) this.node.resultType).tdsColumns;
        List<TDSColumnResultPath> columnResultPaths = ((TDSMetadata) node.metadata).columnResultPaths;

        List<String> docCountName = columnResultPaths.stream()
                .map(x -> x.resultPath)
                .filter(DocCountAggregateResultPath.class::isInstance)
                .map(DocCountAggregateResultPath.class::cast)
                .map(x -> String.join(".", x.fieldPath))
                .collect(Collectors.toList());

        List<Function<ObjectNode, Object>> extractors = ListIterate.zip(tdsColumns, columnResultPaths).stream()
                .map(ElasticsearchTDSResultHelper::aggregationTransformer)
                .collect(Collectors.toList());

        Iterator<ObjectNode> objectNodeStream = null;

        FilteringParserDelegate aggsParser = new FilteringParserDelegate(parser, new JsonPointerBasedFilter("/aggregations"), false, false);

        if (aggregations.size() == 1)
        {
            Map.Entry<String, AggregationContainer> aggregationContainerEntry = aggregations.entrySet().iterator().next();
            AggregationContainer aggregationContainer = aggregationContainerEntry.getValue();

            if (aggregationContainer.composite != null)
            {
                FilteringParserDelegate bucketsParser = new FilteringParserDelegate(aggsParser, new JsonPointerBasedFilter("/composite#" + aggregationContainerEntry.getKey() + "/buckets"), false, false);
                bucketsParser.nextToken();
                bucketsParser.clearCurrentToken();
                Iterator<CompositeBucket> compositeBucketIterator = bucketsParser.readValuesAs(CompositeBucket.class);
                objectNodeStream = processComposite(aggregateTDSResultVisitor, compositeBucketIterator, docCountName, lastBucket);
            }
            else if (aggregationContainer.terms != null)
            {
                aggsParser.nextToken(); // move inside aggregation map
                String fieldName = aggsParser.nextFieldName();
                int typeSeparator = fieldName.indexOf('#');
                String key = fieldName.substring(typeSeparator + 1);
                String fieldType = fieldName.substring(0, typeSeparator);

                Class<? extends TermsBucketBase> bucketClazz;

                switch (fieldType)
                {
                    case "sterms":
                        bucketClazz = StringTermsBucket.class;
                        break;
                    case "lterms":
                        bucketClazz = LongTermsBucket.class;
                        break;
                    case "dterms":
                        bucketClazz = DoubleTermsBucket.class;
                        break;
                    default:
                        throw new UnsupportedOperationException("Terms aggregation not supported: " + fieldName);
                }

                FilteringParserDelegate bucketsParser = new FilteringParserDelegate(aggsParser, new JsonPointerBasedFilter("/buckets"), false, false);
                bucketsParser.nextToken();
                bucketsParser.clearCurrentToken();
                Iterator<? extends TermsBucketBase> bucketsIterator = bucketsParser.readValuesAs(bucketClazz);
                objectNodeStream = processTermsBucket(key, aggregateTDSResultVisitor, bucketsIterator, docCountName, lastBucket);
            }
            else if (aggregationContainer.multi_terms != null)
            {
                FilteringParserDelegate bucketsParser = new FilteringParserDelegate(aggsParser, new JsonPointerBasedFilter("/multi_terms#" + aggregationContainerEntry.getKey() + "/buckets"), false, false);
                bucketsParser.nextToken();
                bucketsParser.clearCurrentToken();
                Iterator<MultiTermsBucket> bucketsIterator = bucketsParser.readValuesAs(MultiTermsBucket.class);
                objectNodeStream = processMultiTermsBucket(aggregationContainerEntry.getKey(), aggregateTDSResultVisitor, bucketsIterator, docCountName, lastBucket);
            }
            else if (aggregationContainer.filters != null)
            {
                String key = aggregationContainerEntry.getKey();
                FilteringParserDelegate filtersParser = new FilteringParserDelegate(aggsParser, new JsonPointerBasedFilter("/filters#" + key), false, false);
                filtersParser.nextToken();
                filtersParser.clearCurrentToken();
                FiltersAggregate filtersAggregate = filtersParser.readValueAs(FiltersAggregate.class);
                objectNodeStream = processFiltersBucket(aggregateTDSResultVisitor, key, docCountName, filtersAggregate);
            }
        }

        if (objectNodeStream == null)
        {
            Map<String, Aggregate> aggregateMap;

            if (searchRequest.body.aggregations.isEmpty())
            {
                aggregateMap = Collections.emptyMap();
            }
            else
            {
                aggregateMap = aggsParser.readValueAs(new TypeReference<ExternalTaggedUnionMap<String, Aggregate>>()
                {

                });
            }

            MutableMap<String, Object> result = Maps.mutable.empty();

            for (String docValue : docCountName)
            {
                Assert.assertTrue(totalHits.relation.equals(TotalHitsRelation.eq), () -> "Doc value count operation could not be computed as elastic give inaccurate total hits!");
                result.put(docValue, totalHits.value.unionValue());
            }

            for (Map.Entry<String, Aggregate> entry : aggregateMap.entrySet())
            {
                result.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
            }

            ObjectNode tree = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(result);

            objectNodeStream = Collections.singletonList(tree).iterator();
        }

        return new CollectIterator<>(objectNodeStream, h -> extractors.stream().map(x -> x.apply(h)).toArray());
    }

    private static Iterator<ObjectNode> processFiltersBucket(AggregateTDSResultVisitor aggregateTDSResultVisitor, String key, List<String> docCountName, FiltersAggregate filtersAggregate)
    {
        Buckets<FiltersBucket> buckets = filtersAggregate.buckets;

        List<ObjectNode> bucketResults = Lists.mutable.empty();

        for (Map.Entry<String, FiltersBucket> bucketEntry : buckets.keyed.entrySet())
        {
            FiltersBucket value = bucketEntry.getValue();

            if (value.doc_count.getLiteral() == 0)
            {
                continue;
            }

            MutableMap<String, Object> result = Maps.mutable.with(key, bucketEntry.getKey());

            for (String docValue : docCountName)
            {
                result.put(docValue, value.doc_count.unionValue());
            }

            for (Map.Entry<String, Aggregate> aggregateEntry : value.__additionalProperties.entrySet())
            {
                result.put(aggregateEntry.getKey(), ((AggregateBase) aggregateEntry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
            }

            bucketResults.add(ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(result));
        }
        return bucketResults.iterator();
    }

    private static Iterator<ObjectNode> processComposite(AggregateTDSResultVisitor aggregateTDSResultVisitor, Iterator<CompositeBucket> buckets, List<String> docCountName, Procedure<MultiBucketBase> lastBucket)
    {
        return new CollectIterator<>(buckets, b ->
        {
            lastBucket.accept(b);
            MutableMap<Object, Object> map = Maps.mutable.empty();

            for (String docValue : docCountName)
            {
                map.put(docValue, b.doc_count.unionValue());
            }

            for (Map.Entry<String, FieldValue> entry : b.key.entrySet())
            {
                map.put(entry.getKey(), entry.getValue().unionValue());
            }

            for (Map.Entry<String, Aggregate> entry : b.__additionalProperties.entrySet())
            {
                map.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
            }

            return ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map);
        });
    }

    private static Iterator<ObjectNode> processMultiTermsBucket(String key, AggregateTDSResultVisitor aggregateTDSResultVisitor, Iterator<MultiTermsBucket> buckets, List<String> docCountName, Procedure<MultiBucketBase> lastBucket)
    {
        String[] fields = key.split("~");

        return new CollectIterator<>(buckets, b ->
        {
            lastBucket.accept(b);
            MutableMap<Object, Object> map = Maps.mutable.empty();

            for (String docValue : docCountName)
            {
                map.put(docValue, b.doc_count.unionValue());
            }

            for (int i = 0; i < fields.length; i++)
            {
                String field = fields[i];
                Object value = ((LiteralOrExpression<?>) b.key.get(i).unionValue()).getLiteral();
                map.put(field, value);
            }

            for (Map.Entry<String, Aggregate> entry : b.__additionalProperties.entrySet())
            {
                map.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
            }

            return ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map);
        });
    }

    private static Iterator<ObjectNode> processTermsBucket(String key, AggregateTDSResultVisitor aggregateTDSResultVisitor, Iterator<? extends TermsBucketBase> buckets, List<String> docCountName, Procedure<MultiBucketBase> lastBucket)
    {
        MultiBucketKeyVisitor multiBucketKeyVisitor = new MultiBucketKeyVisitor();
        return new FlatCollectIterator<>(buckets, bucket ->
        {
            lastBucket.accept(bucket);
            Object keyValue = bucket.accept(multiBucketKeyVisitor);
            keyValue = keyValue instanceof LiteralOrExpression ? ((LiteralOrExpression<?>) keyValue).unionValue() : keyValue;

            List<ObjectNode> results = Lists.mutable.empty();

            Aggregate nullKey = bucket.__additionalProperties.get(key + "~missing~_last");
            boolean nullLast = true;
            if (nullKey == null)
            {
                nullKey = bucket.__additionalProperties.get(key + "~missing~_first");
                nullLast = false;
            }
            Aggregate notNullKey = bucket.__additionalProperties.get(key + "~exists");

            if (notNullKey != null && notNullKey.filter.doc_count.value != 0)
            {
                MutableMap<Object, Object> map = Maps.mutable.with(key, keyValue);

                for (String docValue : docCountName)
                {
                    map.put(docValue, notNullKey.filter.doc_count.unionValue());
                }

                for (Map.Entry<String, Aggregate> entry : notNullKey.filter.__additionalProperties.entrySet())
                {
                    map.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
                }

                results.add(ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map));
            }

            if (nullKey != null && nullKey.missing.doc_count.value != 0)
            {
                MutableMap<Object, Object> map = Maps.mutable.with(key, null);

                for (String docValue : docCountName)
                {
                    map.put(docValue, nullKey.missing.doc_count.unionValue());
                }

                for (Map.Entry<String, Aggregate> entry : nullKey.missing.__additionalProperties.entrySet())
                {
                    map.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
                }

                if (!nullLast)
                {
                    results.add(0, ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map));
                }
                else
                {
                    results.add(ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map));
                }
            }

            // backward compatible processing... before the missing/existing was introduced
            if (nullKey == null && notNullKey == null)
            {
                MutableMap<Object, Object> map = Maps.mutable.with(key, keyValue);

                for (String docValue : docCountName)
                {
                    map.put(docValue, bucket.doc_count.unionValue());
                }

                for (Map.Entry<String, Aggregate> entry : bucket.__additionalProperties.entrySet())
                {
                    map.put(entry.getKey(), ((AggregateBase) entry.getValue().unionValue()).accept(aggregateTDSResultVisitor));
                }

                results.add(ElasticsearchObjectMapperProvider.OBJECT_MAPPER.valueToTree(map));
            }

            return results;
        });
    }

    private Iterator<Object[]> processNotAggregateResponse(JsonParser parser, Span span) throws IOException
    {
        TypeReference<Hit<ObjectNode>> hitTypeReference = new TypeReference<Hit<ObjectNode>>()
        {
        };

        // move parser to hits...
        FilteringParserDelegate hitsParser = new FilteringParserDelegate(parser, new JsonPointerBasedFilter("/hits"), false, false);

        // extract total hits
        FilteringParserDelegate d3 = new FilteringParserDelegate(hitsParser, new JsonPointerBasedFilter("/total"), false, false);
        TotalHits totalHits = d3.readValueAs(TotalHits.class);
        span.log(String.format("Query reported total hits %s %d", totalHits.relation.esName(), totalHits.value.getLiteral()));

        FilteringParserDelegate hitsListParser = new FilteringParserDelegate(hitsParser, new JsonPointerBasedFilter("/hits"), false, false);
        hitsListParser.nextToken(); // start array
        hitsListParser.clearCurrentToken(); // force to look into next token
        Iterator<Hit<ObjectNode>> hits = hitsListParser.readValuesAs(hitTypeReference);

        List<TDSColumn> tdsColumns = ((TDSResultType) this.node.resultType).tdsColumns;
        List<TDSColumnResultPath> columnResultPaths = ((TDSMetadata) node.metadata).columnResultPaths;

        List<Function<Hit<ObjectNode>, Object>> extractors = columnResultPaths.stream()
                .map(x -> ElasticsearchTDSResultHelper.hitTransformer(tdsColumns.get((int) x.index), x.resultPath))
                .collect(Collectors.toList());

        return new CollectIterator<>(hits, h -> extractors.stream().map(x -> x.apply(h)).toArray());
    }

    private static JsonParser toResponseBodyJsonParser(InputStream responseBody, Span span) throws IOException
    {
        // root parser...
        JsonParser parser = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.getFactory().createParser(responseBody);

        // extract how long took
        FilteringParserDelegate tookParser = new FilteringParserDelegate(parser, new JsonPointerBasedFilter("/took"), false, false);
        Long took = tookParser.readValueAs(Long.class);
        span.log(String.format("Query took %dms", took));

        // extract if timed out
        FilteringParserDelegate timedOutParser = new FilteringParserDelegate(parser, new JsonPointerBasedFilter("/timed_out"), false, false);
        Boolean timedOut = timedOutParser.readValueAs(Boolean.class);

        Assert.assertFalse(timedOut, () -> String.format("Elastic reported query timed out after %dms", took));
        return parser;
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

    private static class MultiBucketKeyVisitor extends AbstractMultiBucketBaseVisitor<Object>
    {
        @Override
        protected Object defaultValue(MultiBucketBase val)
        {
            throw new UnsupportedOperationException(val.getClass() + " not supported");
        }

        @Override
        public Object visit(StringTermsBucket val)
        {
            return val.key.unionValue();
        }

        @Override
        public Object visit(LongTermsBucket val)
        {
            return Long.parseLong(val.key.getLiteral());
        }

        @Override
        public Object visit(DoubleTermsBucket val)
        {
            return val.key;
        }
    }

    private static class AggregateTDSResultVisitor extends AbstractAggregateBaseVisitor<Object>
    {
        private final boolean noHits;

        private AggregateTDSResultVisitor(TotalHits total)
        {
            this.noHits = total.value.getLiteral() == 0L;
        }

        @Override
        protected Object defaultValue(AggregateBase val)
        {
            throw new UnsupportedOperationException(val.getClass() + " not supported");
        }

        @Override
        public Object visit(AvgAggregate val)
        {
            return isNoHits() ? null : val.value;
        }

        private boolean isNoHits()
        {
            return this.noHits;
        }

        @Override
        public Object visit(SumAggregate val)
        {
            return val.value;
        }

        @Override
        public Object visit(MaxAggregate val)
        {
            return isNoHits() ? null : val.value;
        }

        @Override
        public Object visit(MinAggregate val)
        {
            return isNoHits() ? null : val.value;
        }

        @Override
        public Object visit(ValueCountAggregate val)
        {
            return val.value;
        }
    }

    private class ElasticsearchResultSpliterator extends Spliterators.AbstractSpliterator<Stream<Object[]>> implements AutoCloseable
    {
        private static final long MAX_COMPOSITE_BUCKETS_PER_REQUEST = 1000L;
        private static final long MAX_TERMS_BUCKETS_PER_REQUEST = 5_001L;

        private final SearchRequest searchRequest;
        private final List<ExecutionActivity> activities;
        private final boolean isAggregation;
        private boolean closed = false;
        private InputStream currInputStream = EmptyInputStream.INSTANCE;
        private MultiBucketBase lastBucket = null;
        private long totalBuckets = 0L;

        private ElasticsearchResultSpliterator(SearchRequest searchRequest, List<ExecutionActivity> activities)
        {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.IMMUTABLE);
            this.searchRequest = searchRequest;
            this.activities = activities;
            this.isAggregation = ((TDSMetadata) node.metadata).columnResultPaths.stream()
                    .map(x -> x.resultPath)
                    .allMatch(x -> x instanceof AggregateResultPath || x instanceof DocCountAggregateResultPath);
        }

        @Override
        public boolean tryAdvance(Consumer<? super Stream<Object[]>> action)
        {
            if (!this.closed)
            {
                // close previous, just in case...
                this.closeCurrentInputStream();

                boolean next = false;

                ThrowingFunction2<JsonParser, Span, Iterator<Object[]>> processor;

                if (this.isAggregation)
                {
                    processor = (x, y) -> ExecutionRequestVisitor.this.processAggregateResponse(this.searchRequest, x, y, b ->
                    {
                        this.lastBucket = b;
                        this.totalBuckets++;
                    });

                    Map<String, AggregationContainer> aggregations = searchRequest.body.aggregations;
                    if (aggregations.size() == 1)
                    {
                        Map.Entry<String, AggregationContainer> aggregationContainerEntry = aggregations.entrySet().iterator().next();
                        AggregationContainer aggregationContainer = aggregationContainerEntry.getValue();

                        if (aggregationContainer.composite != null)
                        {
                            next = true; // only composite aggregation can handle multiple request

                            if (!activities.isEmpty())
                            {
                                // we called once already, check if we need to call again
                                if (this.totalBuckets == MAX_COMPOSITE_BUCKETS_PER_REQUEST)
                                {
                                    // reset
                                    this.totalBuckets = 0L;
                                    // search after last bucket
                                    aggregationContainer.composite.after = ((CompositeBucket) this.lastBucket).key;
                                }
                                else
                                {
                                    return false; // if we got less than requested, we are done...
                                }
                            }
                            else
                            {
                                Assert.assertTrue(aggregationContainer.composite.size == null, () -> "Limit/Take on group by not supported yet");
                                aggregationContainer.composite.size = LiteralOrExpression.literal(MAX_COMPOSITE_BUCKETS_PER_REQUEST);
                            }
                        }
                        else if (aggregationContainer.terms != null)
                        {
                            if (!activities.isEmpty())
                            {
                                Assert.assertTrue(this.totalBuckets < MAX_TERMS_BUCKETS_PER_REQUEST,
                                        () -> String.format("While sorting on aggregate values, we received more buckets that configured of %d.  Either avoid sorting group by, or reduce with filters.", MAX_TERMS_BUCKETS_PER_REQUEST - 1));
                                return false; // if we got less than requested, we are done...
                            }
                            else
                            {
                                Assert.assertTrue(aggregationContainer.terms.size == null, () -> "Limit/Take on group by not supported yet");
                                aggregationContainer.terms.size = LiteralOrExpression.literal(MAX_TERMS_BUCKETS_PER_REQUEST);
                                // we want to check on size of result in case there are more than supported...
                                next = true;
                            }
                        }
                        else if (aggregationContainer.multi_terms != null)
                        {
                            if (!activities.isEmpty())
                            {
                                Assert.assertTrue(this.totalBuckets < MAX_TERMS_BUCKETS_PER_REQUEST,
                                        () -> String.format("While sorting on aggregate values, we received more buckets that configured of %d.  Either avoid sorting group by, or reduce with filters.", MAX_TERMS_BUCKETS_PER_REQUEST - 1));
                                return false; // if we got less than requested, we are done...
                            }
                            else
                            {
                                Assert.assertTrue(aggregationContainer.multi_terms.size == null, () -> "Limit/Take on group by not supported yet");
                                aggregationContainer.multi_terms.size = LiteralOrExpression.literal(MAX_TERMS_BUCKETS_PER_REQUEST);
                                // we want to check on size of result in case there are more than supported...
                                next = true;
                            }
                        }
                    }
                }
                else
                {
                    processor = ExecutionRequestVisitor.this::processNotAggregateResponse;
                }

                if (!next && !this.activities.isEmpty())
                {
                    return false;
                }

                HttpUriRequest request = this.searchRequest.accept(new ElasticsearchV7RequestToHttpRequestVisitor(ExecutionRequestVisitor.this.url, ExecutionRequestVisitor.this.executionState));
                String query = ((HttpEntityEnclosingRequest) request).getEntity().toString();

                ElasticsearchV7ExecutionActivity executionActivity = new ElasticsearchV7ExecutionActivity(request.getURI(), query);
                this.activities.add(executionActivity);

                Span span = GlobalTracer.get().buildSpan("Elasticsearch Request Execution").start();
                try (Scope ignore = GlobalTracer.get().activateSpan(span))
                {
                    span.log(Collections.singletonMap("query", query));
                    long start = System.currentTimeMillis();
                    LOGGER.info("{}", new LogInfo(ElasticsearchExecutionLoggingEventType.EXECUTION_ELASTICSEARCH_START, ExecutionRequestVisitor.this.executionState.authId, query));
                    this.currInputStream = ExecutionRequestVisitor.this.post(request, span, start);

                    JsonParser parser = toResponseBodyJsonParser(this.currInputStream, span);

                    Iterator<Object[]> stream = processor.safeValue(parser, span);

                    action.accept(StreamSupport.stream(Spliterators.spliteratorUnknownSize(stream, Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED), false));

                    return next;
                }
                catch (Exception e)
                {
                    throw new EngineException("Error while executing query: " + query, e, ExceptionCategory.USER_EXECUTION_ERROR);
                }
                finally
                {
                    span.finish();
                }
            }
            else
            {
                return false;
            }
        }

        public void close()
        {
            closeCurrentInputStream();
            this.closed = true;
        }

        private void closeCurrentInputStream()
        {
            try
            {
                this.currInputStream.close();
            }
            catch (IOException ignore)
            {
                // ignore close failures...
            }
        }
    }
}
