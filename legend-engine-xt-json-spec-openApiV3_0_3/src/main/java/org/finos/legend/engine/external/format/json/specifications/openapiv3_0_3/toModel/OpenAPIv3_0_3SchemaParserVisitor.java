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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.toModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.model.JSONSchemaArray;
import org.finos.legend.engine.external.format.json.model.JSONSchemaBoolean;
import org.finos.legend.engine.external.format.json.model.JSONSchemaDiscriminator;
import org.finos.legend.engine.external.format.json.model.JSONSchemaFragment;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaArray;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaBoolean;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaFragment;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaInteger;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNull;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNumber;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaObject;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaString;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.visitor.OpenAPIv3_0_3SchemaVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema.EXTERNAL_DOCS_KEY;
import static org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema.XML_KEY;
import static org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory.getNewObjectMapper;

public class OpenAPIv3_0_3SchemaParserVisitor implements OpenAPIv3_0_3SchemaVisitor<JSONSchema>
{
    private static final ObjectMapper objectMapper = getNewObjectMapper();

    @Override
    public JSONSchema visit(OpenAPIv3_0_3Schema schemaObject)
    {
        throw new EngineException("Cannot visit OpenAPIv3_0_3Schema, please use the subclasses");
    }

    @Override
    public JSONSchemaArray visit(OpenAPIv3_0_3SchemaArray schemaObject)
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
                JSONSchema parsedItemSchema = objectMapper.convertValue(schemaObject.itemSchemas, OpenAPIv3_0_3Schema.class).accept(this);
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
                        JSONSchema parse = objectMapper.convertValue(x, OpenAPIv3_0_3Schema.class).accept(this);
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
                .build();
    }

    @Override
    public JSONSchemaBoolean visit(OpenAPIv3_0_3SchemaBoolean schemaObject)
    {
        JSONSchemaBoolean.JSONSchemaBooleanBuilder protocolSchemaBuilder = (JSONSchemaBoolean.JSONSchemaBooleanBuilder) loadCommonProperties(schemaObject, JSONSchemaBoolean.builder());
        return protocolSchemaBuilder
                .build();
    }

    @Override
    public JSONSchemaFragment visit(OpenAPIv3_0_3SchemaFragment schemaObject)
    {
        return (JSONSchemaFragment) loadCommonProperties(schemaObject, JSONSchemaFragment.builder()).build();
    }

    @Override
    public JSONSchemaInteger visit(OpenAPIv3_0_3SchemaInteger schemaObject)
    {
        JSONSchemaInteger.JSONSchemaIntegerBuilder protocolSchemaBuilder = (JSONSchemaInteger.JSONSchemaIntegerBuilder) loadCommonProperties(schemaObject, JSONSchemaInteger.builder());

        return protocolSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum != null && schemaObject.exclusiveMaximum ? schemaObject.maximum : null)
                .exclusiveMinimum(schemaObject.exclusiveMinimum != null && schemaObject.exclusiveMinimum ? schemaObject.minimum : null)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public JSONSchemaNull visit(OpenAPIv3_0_3SchemaNull schemaObject)
    {
        JSONSchemaNull.JSONSchemaNullBuilder<?> protocolSchemaBuilder = (JSONSchemaNull.JSONSchemaNullBuilder<?>) loadCommonProperties(schemaObject, JSONSchemaNull.builder());
        return protocolSchemaBuilder
                .build();
    }

    @Override
    public JSONSchemaNumber visit(OpenAPIv3_0_3SchemaNumber schemaObject)
    {
        JSONSchemaNumber.JSONSchemaNumberBuilder<?> protocolSchemaBuilder = (JSONSchemaNumber.JSONSchemaNumberBuilder<?>) loadCommonProperties(schemaObject, JSONSchemaNumber.builder());
        return protocolSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum != null && schemaObject.exclusiveMaximum ? schemaObject.maximum : null)
                .exclusiveMinimum(schemaObject.exclusiveMinimum != null && schemaObject.exclusiveMinimum ? schemaObject.minimum : null)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public JSONSchemaObject visit(OpenAPIv3_0_3SchemaObject schemaObject)
    {
        JSONSchemaObject.JSONSchemaObjectBuilder protocolSchemaBuilder = (JSONSchemaObject.JSONSchemaObjectBuilder) loadCommonProperties(schemaObject, JSONSchemaObject.builder());
        return protocolSchemaBuilder
                .minProperties(schemaObject.minProperties)
                .maxProperties(schemaObject.maxProperties)
                .properties(mapToProtocolModel(schemaObject.properties))
                .requiredProperties(schemaObject.requiredProperties)
                .additionalProperties(schemaObject.additionalProperties instanceof Boolean ? schemaObject.additionalProperties : schemaObject.additionalProperties != null ? objectMapper.convertValue(schemaObject.additionalProperties, OpenAPIv3_0_3Schema.class).accept(this) : null)
                .build();
    }

    @Override
    public JSONSchemaString visit(OpenAPIv3_0_3SchemaString schemaObject)
    {
        JSONSchemaString.JSONSchemaStringBuilder protocolSchemaBuilder = (JSONSchemaString.JSONSchemaStringBuilder) loadCommonProperties(schemaObject, JSONSchemaString.builder());
        return protocolSchemaBuilder
                .minLength(schemaObject.minLength)
                .maxLength(schemaObject.maxLength)
                .pattern(schemaObject.pattern)
                .format(schemaObject.format)
                .build();
    }

    private JSONSchema.JSONSchemaBuilder<?> loadCommonProperties(OpenAPIv3_0_3Schema loadedSchema, JSONSchema.JSONSchemaBuilder<?> protocolSchemaBuilder)
    {
        Map<String, Object> customProperties = loadedSchema.getCustomProperties();
        if (loadedSchema.xml != null)
        {
            customProperties.put(XML_KEY, objectMapper.convertValue(loadedSchema.xml, Map.class));
        }
        if (loadedSchema.externalDocs != null)
        {
            customProperties.put(EXTERNAL_DOCS_KEY, objectMapper.convertValue(loadedSchema.externalDocs, Map.class));
        }
        if (loadedSchema.discriminator != null)
        {
            protocolSchemaBuilder = protocolSchemaBuilder.discriminator(
                    JSONSchemaDiscriminator
                            .builder()
                            .propertyName(
                                    loadedSchema
                                            .discriminator
                                            .propertyName
                            )
                            .mapping(loadedSchema.discriminator.mapping != null ? loadedSchema.discriminator.mapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().replaceFirst("#/components/schemas", "#/definitions"))) : null)
                            .build()
            );
        }
        return protocolSchemaBuilder
                .allOf(listToProtocolModel(loadedSchema.allOf))
                .oneOf(listToProtocolModel(loadedSchema.oneOf))
                .anyOf(listToProtocolModel(loadedSchema.anyOf))
                .title(loadedSchema.title)
                .description(loadedSchema.description)
                .defaultValue(loadedSchema.defaultValue)
                .readOnly(loadedSchema.readOnly)
                .writeOnly(loadedSchema.writeOnly)
                .mustNotMatch(loadedSchema.mustNotMatch != null ? loadedSchema.mustNotMatch.accept(this) : null)
                .refValue(loadedSchema.refValue != null ? loadedSchema.refValue.replaceFirst("#/components/schemas/", "#/definitions/") : null)
                .possibleValues(loadedSchema.possibleValues)
                .example(loadedSchema.example)
                .nullable(loadedSchema.nullable)
                .customProperties(customProperties)
                .definitions(loadedSchema.components != null ? mapToProtocolModel(loadedSchema.components.schemas) : null)
                .schema(loadedSchema.schema);
    }

    private List<JSONSchema> listToProtocolModel(List<OpenAPIv3_0_3Schema> actualList)
    {
        if (actualList == null)
        {
            return null;
        }
        return actualList.stream().map(x -> x.accept(this)).collect(Collectors.toList());
    }

    private Map<String, JSONSchema> mapToProtocolModel(Map<String, OpenAPIv3_0_3Schema> actualMap)
    {
        if (actualMap == null)
        {
            return null;
        }
        return actualMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().accept(this)));
    }

}
