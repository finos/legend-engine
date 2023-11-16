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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;

import java.io.IOException;

@JsonDeserialize(using = AuthenticationConfigurationWrapper.AuthenticationStrategyWrapperDeserializer.class)
public class AuthenticationConfigurationWrapper extends AuthenticationStrategy
{
    public AuthenticationConfiguration value;

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }

    public static class AuthenticationStrategyWrapperDeserializer extends JsonDeserializer<AuthenticationConfigurationWrapper>
    {
        @Override
        public AuthenticationConfigurationWrapper deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException
        {
            AuthenticationConfigurationWrapper wrapper = new AuthenticationConfigurationWrapper();
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            JsonNode valueNode = node.get("value");
            // @HACKY: new-connection-framework
            // NOTE: we do this so we can be lazy about compilation, Pure metamodel graph will treat the content as a string
            if (valueNode.isTextual())
            {
                wrapper.value = PureProtocolObjectMapperFactory.getNewObjectMapper().readValue(valueNode.asText(), AuthenticationConfiguration.class);
            }
            else
            {
                wrapper.value = jsonParser.getCodec().treeToValue(valueNode, AuthenticationConfiguration.class);
            }
            return wrapper;
        }
    }
}
