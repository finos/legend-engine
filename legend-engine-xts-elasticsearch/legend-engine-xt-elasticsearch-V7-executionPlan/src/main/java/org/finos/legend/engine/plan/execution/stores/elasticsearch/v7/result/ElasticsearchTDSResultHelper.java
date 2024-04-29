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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.tds.TDSColumnResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.AbstractResultPathVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.DocValueResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.FieldResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.ResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds.SourceFieldResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds._IDResultPath;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.types.Hit;
import org.finos.legend.engine.shared.core.operational.Assert;

public final class ElasticsearchTDSResultHelper
{
    private ElasticsearchTDSResultHelper()
    {

    }

    public static Function<ObjectNode, Object> aggregationTransformer(Pair<TDSColumn, TDSColumnResultPath> column)
    {
        ResultPath one = column.getTwo().resultPath;
        String nameAsString = String.join(".", one.fieldPath);
        TDSColumn tdsColumn = column.getOne();
        Function<JsonNode, Object> tdsTransformer = Functions.ifTrue(x -> !x.isNull() && !x.isMissingNode(), jsonToTDSValue(tdsColumn));
        return Functions.chain(value -> value.path(nameAsString), tdsTransformer);
    }

    public static Function<Hit<ObjectNode>, Object> hitTransformer(TDSColumn column, ResultPath path)
    {
        Function<JsonNode, Object> tdsJsonTransformer = jsonToTDSValue(column);
        Function<Object, Object> tdsRawTransformer = rawToTDSValue(column);

        return path.accept(new AbstractResultPathVisitor<Function<Hit<ObjectNode>, Object>>()
        {
            @Override
            protected Function<Hit<ObjectNode>, Object> defaultValue(ResultPath val)
            {
                throw new UnsupportedOperationException(val.getClass() + " not supported");
            }

            public Function<Hit<ObjectNode>, Object> visit(FieldResultPath val)
            {
                return val.readFrom.accept(this);
            }

            @Override
            public Function<Hit<ObjectNode>, Object> visit(_IDResultPath val)
            {
                return hit -> hit._id.getLiteral();
            }

            @Override
            public Function<Hit<ObjectNode>, Object> visit(DocValueResultPath val)
            {
                String nameAsString = String.join(".", val.fieldPath);

                return hit ->
                {
                    List<Object> fieldValue = (List<Object>) hit.fields.get(nameAsString).getLiteral();
                    if (fieldValue != null && fieldValue.size() > 1)
                    {
                        throw new UnsupportedOperationException(nameAsString + " contains more than one value, and tds does not support array");
                    }
                    return fieldValue == null || fieldValue.isEmpty() ? null : tdsRawTransformer.apply(fieldValue.get(0));
                };
            }

            @Override
            public Function<Hit<ObjectNode>, Object> visit(SourceFieldResultPath sourceFieldResultPath)
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
                            value = value.path(field);
                            if (value.isNull() || value.isMissingNode())
                            {
                                return null;
                            }
                        }

                        Assert.assertFalse(value.isContainerNode(), () -> String.format("Complex types (arrays, maps) not supported on ES TDS results.  Found on path '%s' for id '%s", nameAsString, hit._id));
                        return tdsJsonTransformer.apply(value);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(String.format("Unable to process response value for path %s with TDS type %s.  Result _id: %s", nameAsString, column.type, hit._id), e);
                    }
                };
            }
        });
    }

    private static Function<JsonNode, Object> jsonToTDSValue(TDSColumn column)
    {
        switch (column.type)
        {
            case "String":
                return JsonNode::asText;
            case "Integer":
                return JsonNode::asLong;
            case "Float":
            case "Number":
                return JsonNode::asDouble;
            case "Decimal":
                return Functions.chain(JsonNode::asText, BigDecimal::new);
            case "Boolean":
                return JsonNode::asBoolean;
            case "Date":
            case "DateTime":
            case "StrictDate":
                return x ->
                {
                    if (x.isLong())
                    {
                        return PureDate.fromTemporal(Instant.ofEpochMilli(x.asLong()), Calendar.MILLISECOND);
                    }
                    else
                    {
                       return PureDate.parsePureDate(x.asText());
                    }
                };
            default:
                throw new UnsupportedOperationException("TDS type not supported: " + column.type);
        }
    }

    private static Function<Object, Object> rawToTDSValue(TDSColumn column)
    {
        switch (column.type)
        {
            case "String":
                return String.class::cast;
            case "Integer":
                return Functions.chain(Number.class::cast, Number::longValue);
            case "Float":
            case "Number":
                return Functions.chain(Number.class::cast, Number::doubleValue);
            case "Decimal":
                return x ->
                {
                    throw new UnsupportedOperationException("Decimal value " + x.toString() + " not supported yet");
                };
            case "Boolean":
                return Boolean.class::cast;
            case "Date":
            case "DateTime":
            case "StrictDate":
                return Functions.chain(String.class::cast, PureDate::parsePureDate);
            default:
                throw new UnsupportedOperationException("TDS type not supported: " + column.type);
        }
    }
}
