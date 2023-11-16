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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.ConnectionSpecification;

import java.io.IOException;

@JsonDeserialize(using = ConnectionSpecificationWrapper.ConnectionSpecificationWrapperDeserializer.class)
public class ConnectionSpecificationWrapper extends DatasourceSpecification
{
    public ConnectionSpecification value;

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }

    public static class ConnectionSpecificationWrapperDeserializer extends JsonDeserializer<ConnectionSpecificationWrapper>
    {
        @Override
        public ConnectionSpecificationWrapper deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException
        {
            ConnectionSpecificationWrapper wrapper = new ConnectionSpecificationWrapper();
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode valueNode = node.get("value");
            // @HACKY: new-connection-framework
            // NOTE: we do this so we can be lazy about compilation, Pure metamodel graph will treat the content as a string
            if (valueNode.isTextual())
            {
                wrapper.value = PureProtocolObjectMapperFactory.getNewObjectMapper().readValue(valueNode.asText(), ConnectionSpecification.class);
            }
            else
            {
                wrapper.value = jsonParser.getCodec().treeToValue(valueNode, ConnectionSpecification.class);
            }
            return wrapper;
        }
    }
}
