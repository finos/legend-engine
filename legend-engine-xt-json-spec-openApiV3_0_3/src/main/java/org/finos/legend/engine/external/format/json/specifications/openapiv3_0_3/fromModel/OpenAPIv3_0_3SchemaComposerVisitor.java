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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.fromModel;

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
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Components;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Discriminator;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3ExternalDoc;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaArray;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaBoolean;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaFragment;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaInteger;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNull;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaNumber;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaObject;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3SchemaString;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3XML;
import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema.EXTERNAL_DOCS_KEY;
import static org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model.OpenAPIv3_0_3Schema.XML_KEY;
import static org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory.getNewObjectMapper;

public class OpenAPIv3_0_3SchemaComposerVisitor implements JSONSchemaVisitor<OpenAPIv3_0_3Schema>
{
    private final ObjectMapper objectMapper = getNewObjectMapper();

    @Override
    public OpenAPIv3_0_3Schema visit(JSONSchema schemaObject)
    {
        throw new EngineException("Cannot visit JSONSchema, please use the subclasses");
    }

    @Override
    public OpenAPIv3_0_3SchemaArray visit(JSONSchemaArray schemaObject)
    {
        OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaArray.builder());
        // this if-else ladder is in place to handle the different datatypes that "items" keyword can take.
        if (schemaObject.itemSchemas != null)
        {
            if (schemaObject.itemSchemas instanceof Boolean)
            {
                userSchemaBuilder = userSchemaBuilder.itemSchemas(schemaObject.itemSchemas);
            }
            else if (schemaObject.itemSchemas instanceof JSONSchema)
            {
                OpenAPIv3_0_3Schema parsedItemSchema = ((JSONSchema) schemaObject.itemSchemas).accept(this);
                userSchemaBuilder = userSchemaBuilder.itemSchemas(parsedItemSchema);
            }
            else if (schemaObject.itemSchemas instanceof ArrayList)
            {
                List<Object> itemSchemas = new ArrayList<>();
                ((ArrayList<?>) schemaObject.itemSchemas).forEach(x ->
                {
                    // handle cases where items can be [true, JSONSchema]
                    if (x instanceof JSONSchema)
                    {
                        itemSchemas.add(((JSONSchema) x).accept(this));
                    }
                    else
                    {
                        itemSchemas.add(x);
                    }
                });
                userSchemaBuilder = userSchemaBuilder.itemSchemas(itemSchemas);
            }
        }
        return userSchemaBuilder
                .type("array")
                .minItems(schemaObject.minItems)
                .maxItems(schemaObject.maxItems)
                .uniqueItems(schemaObject.uniqueItems)
                .build();
    }

    @Override
    public OpenAPIv3_0_3SchemaBoolean visit(JSONSchemaBoolean schemaObject)
    {
        OpenAPIv3_0_3SchemaBoolean.OpenAPIv3_0_3SchemaBooleanBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaBoolean.OpenAPIv3_0_3SchemaBooleanBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaBoolean.builder());
        return userSchemaBuilder
                .type("boolean")
                .build();
    }

    @Override
    public OpenAPIv3_0_3SchemaFragment visit(JSONSchemaFragment schemaObject)
    {
        return (OpenAPIv3_0_3SchemaFragment) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaFragment.builder()).build();

    }

    @Override
    public OpenAPIv3_0_3Schema visit(JSONSchemaMultiType schemaObject)
    {
        throw new EngineException("Cannot compose JSONSchemaMultiType");
    }

    @Override
    public OpenAPIv3_0_3SchemaInteger visit(JSONSchemaInteger schemaObject)
    {
        OpenAPIv3_0_3SchemaInteger.OpenAPIv3_0_3SchemaIntegerBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaInteger.OpenAPIv3_0_3SchemaIntegerBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaInteger.builder());
        if (schemaObject.exclusiveMinimum != null)
        {
            userSchemaBuilder = userSchemaBuilder
                    .exclusiveMinimum(true)
                    .minimum(schemaObject.exclusiveMinimum);
        }
        else
        {
            userSchemaBuilder = userSchemaBuilder
                    .minimum(schemaObject.minimum);
        }
        if (schemaObject.exclusiveMaximum != null)
        {
            userSchemaBuilder = userSchemaBuilder
                    .exclusiveMaximum(true)
                    .maximum(schemaObject.exclusiveMaximum);
        }
        else
        {
            userSchemaBuilder = userSchemaBuilder
                    .maximum(schemaObject.maximum);
        }

        return userSchemaBuilder
                .type("integer")
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public OpenAPIv3_0_3SchemaNull visit(JSONSchemaNull schemaObject)
    {
        OpenAPIv3_0_3SchemaNull.OpenAPIv3_0_3SchemaNullBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaNull.OpenAPIv3_0_3SchemaNullBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaNull.builder());
        return userSchemaBuilder
                .type("null")
                .build();
    }

    @Override
    public OpenAPIv3_0_3SchemaNumber visit(JSONSchemaNumber schemaObject)
    {
        OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<?> userSchemaBuilder = (OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<?>) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaNumber.builder());
        if (schemaObject.exclusiveMinimum != null)
        {
            userSchemaBuilder = userSchemaBuilder
                    .exclusiveMinimum(true)
                    .minimum(schemaObject.exclusiveMinimum);
        }
        else
        {
            userSchemaBuilder = userSchemaBuilder
                    .minimum(schemaObject.minimum);
        }
        if (schemaObject.exclusiveMaximum != null)
        {
            userSchemaBuilder = userSchemaBuilder
                    .exclusiveMaximum(true)
                    .maximum(schemaObject.exclusiveMaximum);
        }
        else
        {
            userSchemaBuilder = userSchemaBuilder
                    .maximum(schemaObject.maximum);
        }
        return userSchemaBuilder
                .type("number")
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();

    }

    @Override
    public OpenAPIv3_0_3SchemaObject visit(JSONSchemaObject schemaObject)
    {
        OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaObject.builder());
        return userSchemaBuilder
                .type("object")
                .minProperties(schemaObject.minProperties)
                .maxProperties(schemaObject.maxProperties)
                .properties(mapToSpecModel(schemaObject.properties))
                .requiredProperties(schemaObject.requiredProperties)
                .additionalProperties(schemaObject.additionalProperties instanceof Boolean ? schemaObject.additionalProperties : schemaObject.additionalProperties != null ? ((JSONSchema) schemaObject.additionalProperties).accept(this) : null)
                .build();
    }

    @Override
    public OpenAPIv3_0_3SchemaString visit(JSONSchemaString schemaObject)
    {
        OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder userSchemaBuilder = (OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder) loadCommonProperties(schemaObject, OpenAPIv3_0_3SchemaString.builder());
        return userSchemaBuilder
                .type("string")
                .minLength(schemaObject.minLength)
                .maxLength(schemaObject.maxLength)
                .pattern(schemaObject.pattern)
                .format(schemaObject.format)
                .build();
    }

    private OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<?> loadCommonProperties(JSONSchema receivedProtocolModel, OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<?> specSchemaBuilder)
    {
        Map<String, Object> customProperties = receivedProtocolModel.customProperties;
        if (customProperties != null)
        {
            if (customProperties.containsKey(XML_KEY))
            {
                specSchemaBuilder = specSchemaBuilder.xml(objectMapper.convertValue(customProperties.get(XML_KEY), OpenAPIv3_0_3XML.class));
                customProperties.remove(XML_KEY);
            }
            if (customProperties.containsKey(EXTERNAL_DOCS_KEY))
            {
                specSchemaBuilder = specSchemaBuilder.externalDocs(objectMapper.convertValue(customProperties.get(EXTERNAL_DOCS_KEY), OpenAPIv3_0_3ExternalDoc.class));
                customProperties.remove(EXTERNAL_DOCS_KEY);
            }
        }
        if (receivedProtocolModel.discriminator != null)
        {
            specSchemaBuilder = specSchemaBuilder.discriminator(
                    OpenAPIv3_0_3Discriminator
                            .builder()
                            .propertyName(
                                    receivedProtocolModel
                                            .discriminator
                                            .propertyName
                            )
                            .mapping(receivedProtocolModel
                                    .discriminator
                                    .mapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().replaceFirst("#/definitions", "#/components/schemas")))
                            )
                            .build()
            );
        }
        return specSchemaBuilder
                .allOf(listToSpecModel(receivedProtocolModel.allOf))
                .oneOf(listToSpecModel(receivedProtocolModel.oneOf))
                .anyOf(listToSpecModel(receivedProtocolModel.anyOf))
                .title(receivedProtocolModel.title)
                .description(receivedProtocolModel.description)
                .defaultValue(receivedProtocolModel.defaultValue)
                .readOnly(receivedProtocolModel.readOnly)
                .writeOnly(receivedProtocolModel.writeOnly)
                .mustNotMatch(receivedProtocolModel.mustNotMatch != null ? receivedProtocolModel.mustNotMatch.accept(this) : null)
                .refValue(receivedProtocolModel.refValue != null ? receivedProtocolModel.refValue.replaceFirst("#/definitions/", "#/components/schemas/") : null)
                .possibleValues(receivedProtocolModel.possibleValues)
                .example(receivedProtocolModel.example)
                .nullable(receivedProtocolModel.nullable)
                .customProperties(customProperties)
                .components(receivedProtocolModel.definitions != null ? OpenAPIv3_0_3Components.builder().schemas(mapToSpecModel(receivedProtocolModel.definitions)).build() : null)
                .schema(receivedProtocolModel.schema);
    }

    private List<OpenAPIv3_0_3Schema> listToSpecModel(List<JSONSchema> actualList)
    {
        if (actualList == null)
        {
            return null;
        }
        return actualList.stream().map(x -> x.accept(this)).collect(Collectors.toList());
    }

    private Map<String, OpenAPIv3_0_3Schema> mapToSpecModel(Map<String, JSONSchema> actualMap)
    {
        if (actualMap == null)
        {
            return null;
        }
        return actualMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().accept(this)));
    }

}
