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

package org.finos.legend.engine.external.format.json.specifications.draftv7.fromModel;

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
import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DraftV7SchemaComposerVisitor implements JSONSchemaVisitor<DraftV7Schema>
{
    @Override
    public DraftV7Schema visit(JSONSchema schemaObject)
    {
        throw new EngineException("Cannot visit JSONSchema, please use the subclasses");
    }

    @Override
    public DraftV7SchemaArray visit(JSONSchemaArray schemaObject)
    {
        DraftV7SchemaArray.DraftV7SchemaArrayBuilder userSchemaBuilder = (DraftV7SchemaArray.DraftV7SchemaArrayBuilder) loadCommonProperties(schemaObject, DraftV7SchemaArray.builder(), "array");

        // this if-else ladder is in place to handle the different datatype that "items" keyword can take.
        if (schemaObject.itemSchemas != null)
        {
            if (schemaObject.itemSchemas instanceof Boolean)
            {
                userSchemaBuilder = userSchemaBuilder.itemSchemas(schemaObject.itemSchemas);
            }
            else if (schemaObject.itemSchemas instanceof JSONSchema)
            {
                DraftV7Schema parsedItemSchema = ((JSONSchema) schemaObject.itemSchemas).accept(this);
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
                .minItems(schemaObject.minItems)
                .maxItems(schemaObject.maxItems)
                .uniqueItems(schemaObject.uniqueItems)
                .containedItemSchema(schemaObject.containedItemSchema != null ? schemaObject.containedItemSchema.accept(this) : null)
                .build();

    }

    @Override
    public DraftV7SchemaBoolean visit(JSONSchemaBoolean schemaObject)
    {
        return (DraftV7SchemaBoolean) loadCommonProperties(schemaObject, DraftV7SchemaBoolean.builder(), "boolean").build();
    }

    @Override
    public DraftV7SchemaFragment visit(JSONSchemaFragment schemaObject)
    {
        return (DraftV7SchemaFragment) loadCommonProperties(schemaObject, DraftV7SchemaFragment.builder(), null).build();
    }

    @Override
    public DraftV7SchemaMultiType visit(JSONSchemaMultiType schemaObject)
    {
        return (DraftV7SchemaMultiType) loadCommonProperties(schemaObject, DraftV7SchemaMultiType.builder(), null).type(schemaObject.type).build();
    }

    @Override
    public DraftV7SchemaInteger visit(JSONSchemaInteger schemaObject)
    {
        DraftV7SchemaInteger.DraftV7SchemaIntegerBuilder userSchemaBuilder = (DraftV7SchemaInteger.DraftV7SchemaIntegerBuilder) loadCommonProperties(schemaObject, DraftV7SchemaInteger.builder(), "integer");
        return userSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum)
                .exclusiveMinimum(schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public DraftV7SchemaNull visit(JSONSchemaNull schemaObject)
    {
        return (DraftV7SchemaNull) loadCommonProperties(schemaObject, DraftV7SchemaNull.builder(), "null").build();
    }

    @Override
    public DraftV7SchemaNumber visit(JSONSchemaNumber schemaObject)
    {
        DraftV7SchemaNumber.DraftV7SchemaNumberBuilder<?> userSchemaBuilder = (DraftV7SchemaNumber.DraftV7SchemaNumberBuilder<?>) loadCommonProperties(schemaObject, DraftV7SchemaNumber.builder(), "number");
        return userSchemaBuilder
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum(schemaObject.exclusiveMaximum)
                .exclusiveMinimum(schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public DraftV7SchemaObject visit(JSONSchemaObject schemaObject)
    {
        DraftV7SchemaObject.DraftV7SchemaObjectBuilder userSchemaBuilder = (DraftV7SchemaObject.DraftV7SchemaObjectBuilder) loadCommonProperties(schemaObject, DraftV7SchemaObject.builder(), "object");
        return userSchemaBuilder
                .minProperties(schemaObject.minProperties)
                .maxProperties(schemaObject.maxProperties)
                .propertyNames(schemaObject.propertyNames != null ? schemaObject.propertyNames.accept(this) : null)
                .properties(mapToSpecModel(schemaObject.properties))
                .requiredProperties(schemaObject.requiredProperties)
                .additionalProperties(schemaObject.additionalProperties instanceof Boolean ? schemaObject.additionalProperties : schemaObject.additionalProperties != null ? ((JSONSchema) schemaObject.additionalProperties).accept(this) : null)
                .patternProperties(schemaObject.patternProperties)
                .build();
    }

    @Override
    public DraftV7SchemaString visit(JSONSchemaString schemaObject)
    {
        DraftV7SchemaString.DraftV7SchemaStringBuilder userSchemaBuilder = (DraftV7SchemaString.DraftV7SchemaStringBuilder) loadCommonProperties(schemaObject, DraftV7SchemaString.builder(), "string");
        return userSchemaBuilder
                .minLength(schemaObject.minLength)
                .maxLength(schemaObject.maxLength)
                .pattern(schemaObject.pattern)
                .format(schemaObject.format)
                .build();
    }

    private DraftV7Schema.DraftV7SchemaBuilder<?> loadCommonProperties(JSONSchema receivedProtocolModel, DraftV7Schema.DraftV7SchemaBuilder<?> specSchemaBuilder, String type)
    {
        if (type != null)
        {
            List<String> typeArr = new ArrayList<>();
            typeArr.add(type);
            if (receivedProtocolModel.nullable != null && receivedProtocolModel.nullable)
            {
                typeArr.add("null");
            }
            specSchemaBuilder = specSchemaBuilder.type(typeArr);
        }
        return specSchemaBuilder
                .allOf(listToSpecModel(receivedProtocolModel.allOf))
                .oneOf(listToSpecModel(receivedProtocolModel.oneOf))
                .anyOf(listToSpecModel(receivedProtocolModel.anyOf))
                .title(receivedProtocolModel.title)
                .description(receivedProtocolModel.description)
                .id(receivedProtocolModel.id)
                .schema(receivedProtocolModel.schema)
                .defaultValue(receivedProtocolModel.defaultValue)
                .constantValue(receivedProtocolModel.constantValue)
                .readOnly(receivedProtocolModel.readOnly)
                .writeOnly(receivedProtocolModel.writeOnly)
                .definitions(mapToSpecModel(receivedProtocolModel.definitions))
                .mustNotMatch(receivedProtocolModel.mustNotMatch != null ? receivedProtocolModel.mustNotMatch.accept(this) : null)
                .ifCondition(receivedProtocolModel.ifCondition != null ? receivedProtocolModel.ifCondition.accept(this) : null)
                .elseCondition(receivedProtocolModel.elseCondition != null ? receivedProtocolModel.elseCondition.accept(this) : null)
                .thenCondition(receivedProtocolModel.thenCondition != null ? receivedProtocolModel.thenCondition.accept(this) : null)
                .refValue(receivedProtocolModel.refValue)
                .possibleValues(receivedProtocolModel.possibleValues)
                .example(receivedProtocolModel.example)
                .customProperties(receivedProtocolModel.customProperties)
                .contentMediaType(receivedProtocolModel.contentMediaType)
                .contentEncoding(receivedProtocolModel.contentEncoding);
    }

    private List<DraftV7Schema> listToSpecModel(List<JSONSchema> actualList)
    {
        if (actualList == null)
        {
            return null;
        }
        return actualList.stream().map(x -> x.accept(this)).collect(Collectors.toList());
    }

    private Map<String, DraftV7Schema> mapToSpecModel(Map<String, JSONSchema> actualMap)
    {
        if (actualMap == null)
        {
            return null;
        }
        return actualMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().accept(this)));
    }
}
