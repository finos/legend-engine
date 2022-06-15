//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.json.JSONSchemaSpecificationExtension;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.fromModel.OpenAPIv3_0_3SchemaComposerVisitor;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.toModel.OpenAPIv3_0_3SchemaParserVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import static org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory.getNewObjectMapper;

public class OpenAPIv3_0_3SchemaSpecificationExtension implements JSONSchemaSpecificationExtension
{
    private static final String SCHEMA_URI = "https://spec.openapis.org/oas/v3.0.3#specification";

    private final ObjectMapper objectMapper = getNewObjectMapper();

    @Override
    public String compose(JSONSchema protocolModel)
    {
        try
        {
            return objectMapper.writeValueAsString(protocolModel.accept(new OpenAPIv3_0_3SchemaComposerVisitor()));
        }
        catch (JsonProcessingException e)
        {
            throw new EngineException(e.getMessage());
        }
    }

    @Override
    public JSONSchema parse(JsonNode inputSchema)
    {
        try
        {
            return objectMapper.treeToValue(inputSchema, OpenAPIv3_0_3Schema.class).accept(new OpenAPIv3_0_3SchemaParserVisitor());
        }
        catch (JsonProcessingException e)
        {
            throw new EngineException(e.getMessage());
        }
    }

    @Override
    public String getSchemaURL()
    {
        return SCHEMA_URI;
    }
}
