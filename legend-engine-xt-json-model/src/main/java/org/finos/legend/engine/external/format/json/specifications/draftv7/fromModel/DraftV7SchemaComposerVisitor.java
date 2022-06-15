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
import org.finos.legend.engine.external.format.json.model.JSONSchemaEmpty;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7Schema;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaArray;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaBoolean;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaEmpty;
import org.finos.legend.engine.external.format.json.specifications.draftv7.model.DraftV7SchemaInteger;
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
        DraftV7SchemaArray.DraftV7SchemaArrayBuilder userSchemaBuilder = (DraftV7SchemaArray.DraftV7SchemaArrayBuilder) loadCommonProperties(schemaObject, DraftV7SchemaArray.builder());

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
                .type("array")
                .minItems(schemaObject.minItems)
                .maxItems(schemaObject.maxItems)
                .uniqueItems(schemaObject.uniqueItems)
                .prefixItems(listToSpecModel(schemaObject.prefixItems))
                .containedItemSchema(schemaObject.containedItemSchema != null ? schemaObject.containedItemSchema.accept(this) : null)
                .minContains(schemaObject.minContains)
                .maxContains(schemaObject.maxContains)
                .build();

    }

    @Override
    public DraftV7SchemaBoolean visit(JSONSchemaBoolean schemaObject)
    {
        DraftV7SchemaBoolean.DraftV7SchemaBooleanBuilder userSchemaBuilder = (DraftV7SchemaBoolean.DraftV7SchemaBooleanBuilder) loadCommonProperties(schemaObject, DraftV7SchemaBoolean.builder());
        return userSchemaBuilder
                .type("boolean")
                .build();
    }

    @Override
    public DraftV7SchemaEmpty visit(JSONSchemaEmpty schemaObject)
    {
        return (DraftV7SchemaEmpty) loadCommonProperties(schemaObject, DraftV7SchemaEmpty.builder()).build();
    }

    @Override
    public DraftV7SchemaInteger visit(JSONSchemaInteger schemaObject)
    {
        DraftV7SchemaInteger.DraftV7SchemaIntegerBuilder userSchemaBuilder = (DraftV7SchemaInteger.DraftV7SchemaIntegerBuilder) loadCommonProperties(schemaObject, DraftV7SchemaInteger.builder());
        return userSchemaBuilder
                .type("integer")
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum((Number) schemaObject.exclusiveMaximum)
                .exclusiveMinimum((Number) schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public DraftV7SchemaNull visit(JSONSchemaNull schemaObject)
    {
        DraftV7SchemaNull.DraftV7SchemaNullBuilder userSchemaBuilder = (DraftV7SchemaNull.DraftV7SchemaNullBuilder) loadCommonProperties(schemaObject, DraftV7SchemaNull.builder());
        return userSchemaBuilder
                .type("null")
                .build();
    }

    @Override
    public DraftV7SchemaNumber visit(JSONSchemaNumber schemaObject)
    {
        DraftV7SchemaNumber.DraftV7SchemaNumberBuilder<?> userSchemaBuilder = (DraftV7SchemaNumber.DraftV7SchemaNumberBuilder<?>) loadCommonProperties(schemaObject, DraftV7SchemaNumber.builder());
        return userSchemaBuilder
                .type("number")
                .minimum(schemaObject.minimum)
                .maximum(schemaObject.maximum)
                .exclusiveMaximum((Number) schemaObject.exclusiveMaximum)
                .exclusiveMinimum((Number) schemaObject.exclusiveMinimum)
                .multipleOf(schemaObject.multipleOf)
                .format(schemaObject.format)
                .build();
    }

    @Override
    public DraftV7SchemaObject visit(JSONSchemaObject schemaObject)
    {
        DraftV7SchemaObject.DraftV7SchemaObjectBuilder userSchemaBuilder = (DraftV7SchemaObject.DraftV7SchemaObjectBuilder) loadCommonProperties(schemaObject, DraftV7SchemaObject.builder());
        return userSchemaBuilder
                .type("object")
                .minProperties(schemaObject.minProperties)
                .maxProperties(schemaObject.maxProperties)
                .propertyNames(schemaObject.propertyNames != null ? schemaObject.propertyNames.accept(this) : null)
                .properties(mapToSpecModel(schemaObject.properties))
                .requiredProperties(schemaObject.requiredProperties)
                .additionalProperties(schemaObject.additionalProperties instanceof Boolean ? schemaObject.additionalProperties : schemaObject.additionalProperties != null ? ((JSONSchema) schemaObject.additionalProperties).accept(this) : null)
                .build();
    }

    @Override
    public DraftV7SchemaString visit(JSONSchemaString schemaObject)
    {
        DraftV7SchemaString.DraftV7SchemaStringBuilder userSchemaBuilder = (DraftV7SchemaString.DraftV7SchemaStringBuilder) loadCommonProperties(schemaObject, DraftV7SchemaString.builder());
        return userSchemaBuilder
                .type("string")
                .minLength(schemaObject.minLength)
                .maxLength(schemaObject.maxLength)
                .pattern(schemaObject.pattern)
                .format(schemaObject.format)
                .build();
    }

    private DraftV7Schema.DraftV7SchemaBuilder<?> loadCommonProperties(JSONSchema receivedMetaModel, DraftV7Schema.DraftV7SchemaBuilder<?> specSchemaBuilder)
    {

        return specSchemaBuilder
                .allOf(listToSpecModel(receivedMetaModel.allOf))
                .oneOf(listToSpecModel(receivedMetaModel.oneOf))
                .anyOf(listToSpecModel(receivedMetaModel.anyOf))
                .title(receivedMetaModel.title)
                .description(receivedMetaModel.description)
                .id(receivedMetaModel.id)
                .schema(receivedMetaModel.schema)
                .defaultValue(receivedMetaModel.defaultValue)
                .constantValue(receivedMetaModel.constantValue)
                .readOnly(receivedMetaModel.readOnly)
                .writeOnly(receivedMetaModel.writeOnly)
                .definitions(mapToSpecModel(receivedMetaModel.definitions))
                .mustNotMatch(receivedMetaModel.mustNotMatch != null ? receivedMetaModel.mustNotMatch.accept(this) : null)
                .ifCondition(receivedMetaModel.ifCondition != null ? receivedMetaModel.ifCondition.accept(this) : null)
                .elseCondition(receivedMetaModel.elseCondition != null ? receivedMetaModel.elseCondition.accept(this) : null)
                .thenCondition(receivedMetaModel.thenCondition != null ? receivedMetaModel.thenCondition.accept(this) : null)
                .refValue(receivedMetaModel.refValue)
                .possibleValues(receivedMetaModel.possibleValues)
                .example(receivedMetaModel.example)
                .customProperties(receivedMetaModel.customProperties);
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
