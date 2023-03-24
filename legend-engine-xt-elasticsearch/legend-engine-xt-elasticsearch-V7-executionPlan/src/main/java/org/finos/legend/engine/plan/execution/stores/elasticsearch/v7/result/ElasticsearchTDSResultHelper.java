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
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.ResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.ResultPathVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.SourceFieldResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.ElasticsearchObjectMapperProvider;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.ResponseBody;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.Hit;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.HitsMetadata;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.TotalHits;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.TotalHitsRelation;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.ShardStatistics;
import org.finos.legend.engine.shared.core.operational.Assert;

public final class ElasticsearchTDSResultHelper
{
    private ElasticsearchTDSResultHelper()
    {

    }

    public static Function<Hit<JsonNode>, Object> hitTransformer(TDSColumn column, ResultPath path)
    {
        Function<JsonNode, Object> tdsTransformer = toTDSValue(column);

        return path.accept(new ResultPathVisitor<Function<Hit<JsonNode>, Object>>()
        {
            @Override
            public Function<Hit<JsonNode>, Object> visit(SourceFieldResultPath sourceFieldResultPath)
            {
                String nameAsString = String.join(".", sourceFieldResultPath.fieldPath);

                return hit ->
                {
                    try
                    {
                        JsonNode value = hit._source;
                        for (String field : sourceFieldResultPath.fieldPath)
                        {
                            Assert.assertTrue(value.isObject(), () -> String.format("Field '%s' in field path '%s' is not a map.  ID: %s", field, nameAsString, hit._id));
                            value = value.get(field);
                            if (value.isNull())
                            {
                                break;
                            }
                        }

                        Assert.assertFalse(value.isContainerNode(), () -> String.format("Complex types (arrays, maps) not supported on ES TDS results.  Found on path '%s' for id '%s", nameAsString, hit._id));

                        if (!value.isNull())
                        {
                            return tdsTransformer.apply(value);
                        }
                        else
                        {
                            return null;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(String.format("Unable to process response value for path %s with TDS type %s.  Result _id: %s", nameAsString, column.type, hit._id));
                    }
                };
            }
        });
    }

    private static Function<JsonNode, Object> toTDSValue(TDSColumn column)
    {
        switch (column.type)
        {
            case "String":
                return JsonNode::asText;
            case "Integer":
                return JsonNode::asLong;
            case "Float":
                return JsonNode::asDouble;
            case "Decimal":
                return ((Function<JsonNode, String>) JsonNode::asText).andThen(BigDecimal::new);
            case "Boolean":
                return JsonNode::asBoolean;
            case "Date":
            case "DateTime":
            case "StrictDate":
                return ((Function<JsonNode, String>) JsonNode::asText).andThen(PureDate::parsePureDate);
            default:
                throw new UnsupportedOperationException("TDS type not supported: " + column.type);
        }
    }

    public static Stream<Hit<JsonNode>> tdsFromHits(InputStream is, ResponseBody<JsonNode> responseBody) throws IOException
    {
        return StreamSupport.stream(new HitsSpliterator(is, responseBody), false);
    }

    private static class HitsSpliterator implements Spliterator<Hit<JsonNode>>, Closeable
    {
        private final InputStream is;
        private final ResponseBody<JsonNode> responseBody;
        private final JsonParser jsonParser;
        private Iterator<Hit<JsonNode>> hits = null;
        private long rowsFound = 0;

        public HitsSpliterator(InputStream is, ResponseBody<JsonNode> responseBody) throws IOException
        {
            this.is = is;
            this.jsonParser = ElasticsearchObjectMapperProvider.OBJECT_MAPPER.reader().getFactory().createParser(is);
            this.responseBody = responseBody;
            this.responseBody.hits = new HitsMetadata<>();
            this.responseBody.hits.total = new TotalHits();
            this.responseBody.hits.total.value = Long.MAX_VALUE;
            this.responseBody.hits.total.relation = TotalHitsRelation.gte;
            this.advanceParser();
        }

        @Override
        public boolean tryAdvance(Consumer<? super Hit<JsonNode>> action)
        {
            boolean hasNext = this.hits.hasNext();
            if (hasNext)
            {
                this.rowsFound++;
                action.accept(this.hits.next());
            }
            else
            {
                // todo check we got all results?
                // todo how to paginate?  searchAfter? scroll id? pit?
            }
            return hasNext;
        }

        private void advanceParser() throws IOException
        {
            Assert.assertTrue(this.hits == null || !this.hits.hasNext(), () -> "cannot advance parser when there still hits to be consumed!");
            while (jsonParser.currentToken() != JsonToken.END_OBJECT)
            {
                if (jsonParser.currentToken() == JsonToken.FIELD_NAME)
                {
                    String propertyName = jsonParser.getCurrentName();
                    JsonToken fieldValueToken = jsonParser.nextToken();
                    switch (propertyName)
                    {
                        case "took":
                            assert fieldValueToken == JsonToken.VALUE_NUMBER_INT;
                            responseBody.took = jsonParser.getLongValue();
                            break;
                        case "timed_out":
                            assert fieldValueToken == JsonToken.VALUE_TRUE || fieldValueToken == JsonToken.VALUE_FALSE;
                            responseBody.timed_out = jsonParser.getBooleanValue();
                            if (responseBody.timed_out)
                            {
                                throw new RuntimeException("ES request timed out after " + responseBody.took);
                            }
                            break;
                        case "_shards":
                            assert fieldValueToken == JsonToken.START_OBJECT;
                            responseBody._shards = jsonParser.readValueAs(ShardStatistics.class);
                            if (responseBody._shards.failed != null && responseBody._shards.failed.doubleValue() > 0)
                            {
                                // todo use failure to details - responseBody._shards.failures
                                throw new RuntimeException("Some shards failed");
                            }
                            break;
                        case "terminated_early":
                            assert fieldValueToken == JsonToken.VALUE_NULL || fieldValueToken == JsonToken.VALUE_TRUE || fieldValueToken == JsonToken.VALUE_FALSE;
                            ;
                            responseBody.terminated_early = jsonParser.getValueAsBoolean(false);
                            if (responseBody.terminated_early)
                            {
                                throw new RuntimeException("ES request terminated early");
                            }
                            break;
                        case "hits":
                            assert fieldValueToken == JsonToken.START_OBJECT;
                            do
                            {
                                jsonParser.nextToken();
                                String hitPropertyName = jsonParser.getCurrentName();
                                JsonToken hitFieldValueToken = jsonParser.nextToken();
                                switch (hitPropertyName)
                                {
                                    case "total":
                                        assert hitFieldValueToken == JsonToken.START_OBJECT;
                                        responseBody.hits.total = jsonParser.readValueAs(TotalHits.class);
                                        break;
                                    case "max_score":
                                        assert hitFieldValueToken == JsonToken.VALUE_NUMBER_FLOAT || hitFieldValueToken == JsonToken.VALUE_NULL;
                                        responseBody.hits.max_score = hitFieldValueToken == JsonToken.VALUE_NULL ? null : jsonParser.getDoubleValue();
                                        break;
                                    case "hits":
                                        assert hitFieldValueToken == JsonToken.START_ARRAY;
                                        jsonParser.clearCurrentToken();
                                        this.hits = jsonParser.readValuesAs(new TypeReference<Hit<JsonNode>>()
                                        {
                                        });
                                        return; // return so we can consume hits!
                                    default:
                                        throw new IllegalStateException("unexpected token");
                                }
                            }
                            while (jsonParser.currentToken() != JsonToken.END_OBJECT);
                            break;
                        default:
                            continue;
                    }
                }
                jsonParser.nextToken();
            }
        }

        @Override
        public Spliterator<Hit<JsonNode>> trySplit()
        {
            return null;
        }

        @Override
        public long estimateSize()
        {
            return this.responseBody.hits.total.value;
        }

        @Override
        public int characteristics()
        {
            return 0;
        }

        @Override
        public void close()
        {
            try
            {
                while (hits.hasNext())
                {
                    this.hits.next();
                }
                this.advanceParser();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            finally
            {
                try
                {
                    this.is.close();
                }
                catch (IOException ignore)
                {
                    // ignore
                }
            }
        }
    }
}
