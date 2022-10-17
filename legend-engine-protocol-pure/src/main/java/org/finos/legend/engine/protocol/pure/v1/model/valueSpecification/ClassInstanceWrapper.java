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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;

import java.io.IOException;

@JsonDeserialize(using = ClassInstanceWrapper.ClassInstanceWrapperDeserializer.class)
public class ClassInstanceWrapper extends ValueSpecification
{
    protected static ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();

    protected static ClassInstance wrapClassInstance(JsonNode node, java.lang.Class _class, String id) throws IOException
    {
        return new ClassInstance(id, om.treeToValue(node, _class));
    }

    public static class ClassInstanceWrapperDeserializer extends JsonDeserializer<ValueSpecification>
    {
        @Override
        public ValueSpecification deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
             JsonNode jtype = node.get("_type");
            String type = jtype == null ? null : jtype.asText();
            ((ObjectNode)node).remove("_type");
            if ("path".equals(type) || node.get("path") != null)
            {
                return wrapClassInstance(node, Path.class, "path");
            }
            if ("rootGraphFetchTree".equals(type) || node.get("class") != null)
            {
                ((ObjectNode) node).set("_type", new TextNode("rootGraphFetchTree"));
                return wrapClassInstance(node, RootGraphFetchTree.class, "rootGraphFetchTree");
            }
            if ("listInstance".equals(type) || node.get("values") != null)
            {
                return wrapClassInstance(node, PureList.class, "listInstance");
            }
            if ("pair".equals(type) || node.get("first") != null)
            {
                return wrapClassInstance(node, Pair.class, "pair");
            }
            if ("aggregateValue".equals(type) || node.get("mapFn") != null && node.get("name") == null)
            {
                return wrapClassInstance(node, AggregateValue.class, "aggregateValue");
            }
            if ("tdsAggregateValue".equals(type) || node.get("mapFn") != null && node.get("name") != null)
            {
                return wrapClassInstance(node, TDSAggregateValue.class, "tdsAggregateValue");
            }
            if ("tdsColumnInformation".equals(type) || node.get("columnFn") != null)
            {
                return wrapClassInstance(node, TDSColumnInformation.class, "tdsColumnInformation");
            }
            if ("tdsSortInformation".equals(type) || node.get("direction") != null)
            {
                return wrapClassInstance(node, TDSSortInformation.class, "tdsSortInformation");
            }
            if ("tdsOlapRank".equals(type) || node.get("function") != null && node.get("columnName") == null)
            {
                return wrapClassInstance(node, TdsOlapRank.class, "tdsOlapRank");
            }
            if ("tdsOlapAggregation".equals(type) || node.get("function") != null && node.get("columnName") != null)
            {
                return wrapClassInstance(node, TdsOlapAggregation.class, "tdsOlapAggregation");
            }
            if ("runtimeInstance".equals(type) || node.get("runtime") != null)
            {
                return wrapClassInstance(node, RuntimeInstance.class, "runtimeInstance");
            }
            if ("executionContext".equals(type) || node.get("executionContext") != null)
            {
                return wrapClassInstance(node, ExecutionContextInstance.class, "executionContext");
            }
            if ("alloySerializationConfig".equals(type) || node.get("includeType") != null)
            {
                return wrapClassInstance(node, SerializationConfig.class, "alloySerializationConfig");
            }
            throw new RuntimeException("NOT SUPPORTED\n" + node.toPrettyString());
        }
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return null;
    }
}
