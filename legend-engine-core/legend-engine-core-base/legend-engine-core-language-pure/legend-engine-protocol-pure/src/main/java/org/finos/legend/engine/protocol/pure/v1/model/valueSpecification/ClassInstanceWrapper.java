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
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.eclipse.collections.api.factory.Lists;
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
import java.util.List;

@JsonDeserialize(using = ClassInstanceWrapper.ClassInstanceWrapperDeserializer.class)
public class ClassInstanceWrapper extends ValueSpecification
{
    protected static ObjectMapper om = PureProtocolObjectMapperFactory.getNewObjectMapper();

    public static class ClassInstanceWrapperDeserializer extends JsonDeserializer<ValueSpecification>
    {
        @Override
        public ValueSpecification deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode jtype = node.get("_type");
            String type = jtype == null ? null : jtype.asText();
            if (!(node instanceof  NullNode))
            {
                ((ObjectNode) node).remove("_type");
            }
            else
            {
                node = new POJONode(new PureList());
            }
            List<String> fields = Lists.mutable.empty();
            node.fieldNames().forEachRemaining(fields::add);
            if ("path".equals(type) || node.get("path") != null)
            {
                Path p = om.treeToValue(node, Path.class);
                return new ClassInstance("path", p, p.sourceInformation);
            }
            if ("rootGraphFetchTree".equals(type) || node.get("class") != null)
            {
                ((ObjectNode) node).set("_type", new TextNode("rootGraphFetchTree"));
                RootGraphFetchTree p = om.treeToValue(node, RootGraphFetchTree.class);
                return new ClassInstance("rootGraphFetchTree", p, p.sourceInformation);
            }
            if ("listInstance".equals(type) || node.get("values") != null || fields.isEmpty() || (fields.size() == 1 && "sourceInformation".equals(fields.get(0))))
            {
                PureList p = om.treeToValue(node, PureList.class);
                return new ClassInstance("listInstance", p, p.sourceInformation);
            }
            if ("pair".equals(type) || node.get("first") != null)
            {
                Pair p = om.treeToValue(node, Pair.class);
                return new ClassInstance("pair", p, p.sourceInformation);
            }
            if ("aggregateValue".equals(type) || node.get("mapFn") != null && node.get("name") == null)
            {
                AggregateValue p = om.treeToValue(node, AggregateValue.class);
                return new ClassInstance("aggregateValue", p, p.sourceInformation);
            }
            if ("tdsAggregateValue".equals(type) || node.get("mapFn") != null && node.get("name") != null)
            {
                TDSAggregateValue p = om.treeToValue(node, TDSAggregateValue.class);
                return new ClassInstance("tdsAggregateValue", p, p.sourceInformation);
            }
            if ("tdsColumnInformation".equals(type) || node.get("columnFn") != null)
            {
                TDSColumnInformation p = om.treeToValue(node, TDSColumnInformation.class);
                return new ClassInstance("tdsColumnInformation", p, p.sourceInformation);
            }
            if ("tdsSortInformation".equals(type) || node.get("direction") != null)
            {
                TDSSortInformation p = om.treeToValue(node, TDSSortInformation.class);
                return new ClassInstance("tdsSortInformation", p, p.sourceInformation);
            }
            if ("tdsOlapRank".equals(type) || node.get("function") != null && node.get("columnName") == null)
            {
                TdsOlapRank p = om.treeToValue(node, TdsOlapRank.class);
                return new ClassInstance("tdsOlapRank", p, p.sourceInformation);
            }
            if ("tdsOlapAggregation".equals(type) || node.get("function") != null && node.get("columnName") != null)
            {
                TdsOlapAggregation p = om.treeToValue(node, TdsOlapAggregation.class);
                return new ClassInstance("tdsOlapAggregation", p, p.sourceInformation);
            }
            if ("runtimeInstance".equals(type) || node.get("runtime") != null)
            {
                RuntimeInstance p = om.treeToValue(node, RuntimeInstance.class);
                return new ClassInstance("runtimeInstance", p, p.sourceInformation);
            }
            if ("executionContextInstance".equals(type) || node.get("executionContext") != null)
            {
                ExecutionContextInstance p = om.treeToValue(node, ExecutionContextInstance.class);
                return new ClassInstance("executionContextInstance", p, p.sourceInformation);
            }
            if ("alloySerializationConfig".equals(type) || node.get("typeKeyName") != null)
            {
                SerializationConfig p = om.treeToValue(node, SerializationConfig.class);
                return new ClassInstance("alloySerializationConfig", p, p.sourceInformation);
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
