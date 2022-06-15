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

package org.finos.legend.engine.external.format.json.specifications.draftv7;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.json.JSONSchemaSpecificationExtension;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.specifications.draftv7.fromModel.DraftV7SchemaComposerVisitor;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7Schema;
import org.finos.legend.engine.external.format.json.specifications.draftv7.toModel.DraftV7SchemaParserVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import static org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory.getNewObjectMapper;

public class DraftV7SchemaSpecificationExtension implements JSONSchemaSpecificationExtension
{
    public static final String SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private final ObjectMapper objectMapper = getNewObjectMapper();

    @Override
    public String compose(JSONSchema protocolModel)
    {
        try
        {
            return objectMapper.writeValueAsString(protocolModel.accept(new DraftV7SchemaComposerVisitor()));
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
            return objectMapper.treeToValue(inputSchema, DraftV7Schema.class).accept(new DraftV7SchemaParserVisitor());
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
