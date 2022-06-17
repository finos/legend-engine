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

package org.finos.legend.engine.external.format.json.specifications.draftv7.toModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.model.JSONSchemaArray;
import org.finos.legend.engine.external.format.json.model.JSONSchemaBoolean;
import org.finos.legend.engine.external.format.json.model.JSONSchemaFragment;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaMultiType;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7Schema;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaArray;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaBoolean;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaFragment;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaInteger;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaMultiType;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaNull;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaNumber;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaObject;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaString;
import org.finos.legend.engine.external.format.json.specifications.draftv7.visitor.DraftV7SchemaVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory.getNewObjectMapper;

public class DraftV7SchemaParserVisitor implements DraftV7SchemaVisitor<JSONSchema>
{
    private final ObjectMapper objectMapper = getNewObjectMapper();

    @Override
    public JSONSchema visit(DraftV7Schema schemaObject)
    {
        throw new EngineException("Cannot visit DraftV7Schema, please use the subclasses");
    }

    @Override
    public JSONSchemaArray visit(DraftV7SchemaArray schemaObject)
    {
        JSONSchemaArray.JSONSchemaArrayBuilder protocolSchemaBuilder = (JSONSchemaArray.JSONSchemaArrayBuilder) loadCommonProperties(schemaObject, JSONSchemaArray.builder());
        if (schemaObject.itemSchemas != null)
        {
            if (schemaObject.itemSchemas instanceof Boolean)
            {
                protocolSchemaBuilder = protocolSchemaBuilder.itemSchemas(schemaObject.itemSchemas);
            }
            else if (schemaObject.itemSchemas instanceof LinkedHashMap)
            {
                JSONSchema parsedItemSchema = objectMapper.convertValue(schemaObject.itemSchemas, DraftV7Schema.class).accept(this);
                protocolSchemaBuilder = protocolSchemaBuilder.itemSchemas(parsedItemSchema);
            }
            else if (schemaObject.itemSchemas instanceof ArrayList)
            {
                List<Object> parsedList = new ArrayList<>();
                // handle cases where items can be [true, JSONSchema]
                for (Object x : (ArrayList<?>) schemaObject.itemSchemas)
                {
                    if (x instanceof Boolean)
                    {
                        parsedList.add(x);
                    }
                    else if (x instanceof LinkedHashMap)
                    {
                        JSONSchema parse = objectMapper.convertValue(x, DraftV7Schema.class).accept(this);
                        parsedList.add(parse);
                    }
                    else
                    {
                        throw new EngineException("Unable to parse items schema of Array. Supported types are Boolean, JSONSchema. Received: " + x.getClass().getName());
                    }
                }
                protocolSchemaBuilder = protocolSchemaBuilder.itemSchemas(parsedList);
            }
        }

        return protocolSchemaBuilder
                .minItems(schemaObject.minItems)
                .maxItems(schemaObject.maxItems)
                .uniqueItems(schemaObject.uniqueItems)
                .containedItemSchema(schemaObject.containedItemSchema != null ? schemaObject.containedItemSchema.accept(this) : null)
                .build();
    }

    @Override
    public JSONSchemaBoolean visit(DraftV7SchemaBoolean schemaObject)
    {
        return (JSONSchemaBoolean) loadCommonProperties(schemaObject, JSONSchemaBoolean.builder()).build();
    }

    @Override
    public JSONSchemaFragment visit(DraftV7SchemaFragment schemaObject)
    {
        return (JSONSchemaFragment) loadCommonProperties(schemaObject, JSONSchemaFragment.builder()).build();
    }

    @Override
    public JSONSchemaInteger visit(DraftV7SchemaInteger schemaObject)
    {
        JSONSchemaInteger.JSONSchemaIntegerBuilder protocolSchemaBuilder = (JSONSchemaInteger.JSONSchemaIntegerBuilder) loadCommonProperties(schemaObject, JSONSchemaInteger.builder());
        return protocolSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum)
                .exclusiveMinimum(schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public JSONSchemaMultiType visit(DraftV7SchemaMultiType schemaObject)
    {
        return ((JSONSchemaMultiType.JSONSchemaMultiTypeBuilder) loadCommonProperties(schemaObject, JSONSchemaMultiType.builder())).type(schemaObject.type).build();
    }

    @Override
    public JSONSchemaNull visit(DraftV7SchemaNull schemaObject)
    {
        return (JSONSchemaNull) loadCommonProperties(schemaObject, JSONSchemaNull.builder()).build();
    }

    @Override
    public JSONSchemaNumber visit(DraftV7SchemaNumber schemaObject)
    {
        JSONSchemaNumber.JSONSchemaNumberBuilder<?> protocolSchemaBuilder = (JSONSchemaNumber.JSONSchemaNumberBuilder<?>) loadCommonProperties(schemaObject, JSONSchemaNumber.builder());
        return protocolSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum)
                .exclusiveMinimum(schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public JSONSchemaObject visit(DraftV7SchemaObject schemaObject)
    {
        JSONSchemaObject.JSONSchemaObjectBuilder protocolSchemaBuilder = (JSONSchemaObject.JSONSchemaObjectBuilder) loadCommonProperties(schemaObject, JSONSchemaObject.builder());
        return protocolSchemaBuilder
                .minProperties(schemaObject.minProperties)
                .maxProperties(schemaObject.maxProperties)
                .propertyNames(schemaObject.propertyNames != null ? (JSONSchemaString) schemaObject.propertyNames.accept(this) : null)
                .properties(mapToProtocolModel(schemaObject.properties))
                .requiredProperties(schemaObject.requiredProperties)
                .additionalProperties(schemaObject.additionalProperties instanceof Boolean ? schemaObject.additionalProperties : schemaObject.additionalProperties != null ? objectMapper.convertValue(schemaObject.additionalProperties, DraftV7Schema.class).accept(this) : null)
                .patternProperties(schemaObject.patternProperties)
                .build();
    }

    @Override
    public JSONSchemaString visit(DraftV7SchemaString schemaObject)
    {
        JSONSchemaString.JSONSchemaStringBuilder protocolSchemaBuilder = (JSONSchemaString.JSONSchemaStringBuilder) loadCommonProperties(schemaObject, JSONSchemaString.builder());
        return protocolSchemaBuilder
                .minLength(schemaObject.minLength)
                .maxLength(schemaObject.maxLength)
                .pattern(schemaObject.pattern)
                .format(schemaObject.format)
                .build();
    }


    private JSONSchema.JSONSchemaBuilder<?> loadCommonProperties(DraftV7Schema loadedSchema, JSONSchema.JSONSchemaBuilder<?> protocolSchemaBuilder)
    {
        Map<String, Object> customProperties = loadedSchema.customProperties;
        if (loadedSchema.customProperties != null && loadedSchema.customProperties.containsKey("nullable"))
        {
            protocolSchemaBuilder = protocolSchemaBuilder.nullable(true);
            customProperties = customProperties.entrySet()
                    .stream()
                    .filter(e -> !Objects.equals(e.getKey(), "nullable"))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
        }
        return protocolSchemaBuilder
                .allOf(listToProtocolModel(loadedSchema.allOf))
                .oneOf(listToProtocolModel(loadedSchema.oneOf))
                .anyOf(listToProtocolModel(loadedSchema.anyOf))
                .title(loadedSchema.title)
                .description(loadedSchema.description)
                .id(loadedSchema.id)
                .schema(loadedSchema.schema)
                .defaultValue(loadedSchema.defaultValue)
                .constantValue(loadedSchema.constantValue)
                .readOnly(loadedSchema.readOnly)
                .writeOnly(loadedSchema.writeOnly)
                .definitions(mapToProtocolModel(loadedSchema.definitions))
                .mustNotMatch(loadedSchema.mustNotMatch != null ? loadedSchema.mustNotMatch.accept(this) : null)
                .ifCondition(loadedSchema.ifCondition != null ? loadedSchema.ifCondition.accept(this) : null)
                .elseCondition(loadedSchema.elseCondition != null ? loadedSchema.elseCondition.accept(this) : null)
                .thenCondition(loadedSchema.thenCondition != null ? loadedSchema.thenCondition.accept(this) : null)
                .refValue(loadedSchema.refValue)
                .possibleValues(loadedSchema.possibleValues)
                .example(loadedSchema.example)
                .customProperties(customProperties)
                .contentMediaType(loadedSchema.contentMediaType)
                .contentEncoding(loadedSchema.contentEncoding);
    }

    private List<JSONSchema> listToProtocolModel(List<DraftV7Schema> actualList)
    {
        if (actualList == null)
        {
            return null;
        }
        return actualList.stream().map(x -> x.accept(this)).collect(Collectors.toList());
    }

    private Map<String, JSONSchema> mapToProtocolModel(Map<String, DraftV7Schema> actualMap)
    {
        if (actualMap == null)
        {
            return null;
        }
        return actualMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().accept(this)));
    }

}
