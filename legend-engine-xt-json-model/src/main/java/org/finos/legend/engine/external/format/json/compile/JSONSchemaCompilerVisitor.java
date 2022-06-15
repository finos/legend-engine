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

package org.finos.legend.engine.external.format.json.compile;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.model.JSONSchemaArray;
import org.finos.legend.engine.external.format.json.model.JSONSchemaBoolean;
import org.finos.legend.engine.external.format.json.model.JSONSchemaEmpty;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;
import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaArray;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaArray_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaBoolean;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaBoolean_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaDiscriminator;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaDiscriminator_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaEmpty;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaEmpty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaInteger;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaInteger_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNull;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNull_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNumber;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNumber_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaObject;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaObject_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaString;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaString_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONArray;
import org.finos.legend.pure.generated.Root_meta_json_JSONArray_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONBoolean_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONElement;
import org.finos.legend.pure.generated.Root_meta_json_JSONKeyValue_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONNumber_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONObject_Impl;
import org.finos.legend.pure.generated.Root_meta_json_JSONString;
import org.finos.legend.pure.generated.Root_meta_json_JSONString_Impl;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JSONSchemaCompilerVisitor implements JSONSchemaVisitor<Root_meta_external_format_json_metamodel_JSONSchema>
{
    @Override
    public Root_meta_external_format_json_metamodel_JSONSchema visit(JSONSchema schemaObject)
    {
        throw new EngineException("Cannot visit JSONSchema, please use the subclasses");
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaArray visit(JSONSchemaArray schemaObject)
    {
        Root_meta_external_format_json_metamodel_JSONSchemaArray metaSchemaBuilder = (Root_meta_external_format_json_metamodel_JSONSchemaArray) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaArray_Impl(""), schemaObject);

        if (schemaObject.itemSchemas != null)
        {
            if (schemaObject.itemSchemas instanceof Boolean)
            {
                metaSchemaBuilder._itemSchemas(Lists.mutable.with(schemaObject.itemSchemas));
            }
            else if (schemaObject.itemSchemas instanceof JSONSchema)
            {
                metaSchemaBuilder = metaSchemaBuilder._itemSchemas(Lists.mutable.with(((JSONSchema) schemaObject.itemSchemas).accept(this)));
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
                    else if (x instanceof JSONSchema)
                    {
                        parsedList.add(((JSONSchema) x).accept(this));
                    }
                    else
                    {
                        throw new EngineException("Error while compiling ItemSchemas. Supported types are Boolean, JSONSchema. Received: " + x.getClass().getName());
                    }
                }
                metaSchemaBuilder = metaSchemaBuilder._itemSchemas(ListIterate.collect(parsedList, x -> x));
            }
        }

        if (schemaObject.minItems != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._minItems(Long.valueOf(schemaObject.minItems));
        }
        if (schemaObject.maxItems != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._maxItems(Long.valueOf(schemaObject.maxItems));
        }
        if (schemaObject.uniqueItems != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._uniqueItems(schemaObject.uniqueItems);
        }
        if (schemaObject.prefixItems != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._prefixItems(listToPure(schemaObject.prefixItems));
        }
        if (schemaObject.containedItemSchema != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._containedItemSchema(schemaObject.containedItemSchema.accept(this));
        }
        if (schemaObject.minContains != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._minContains(Long.valueOf(schemaObject.minContains));
        }
        if (schemaObject.maxContains != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._maxContains(Long.valueOf(schemaObject.maxContains));
        }
        return metaSchemaBuilder;
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaBoolean visit(JSONSchemaBoolean schemaObject)
    {
        return (Root_meta_external_format_json_metamodel_JSONSchemaBoolean) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaBoolean_Impl(""), schemaObject);
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaEmpty visit(JSONSchemaEmpty schemaObject)
    {
        return (Root_meta_external_format_json_metamodel_JSONSchemaEmpty) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaEmpty_Impl(""), schemaObject);
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaInteger visit(JSONSchemaInteger schemaObject)
    {
        return (Root_meta_external_format_json_metamodel_JSONSchemaInteger) loadCommonNumberProperties(schemaObject, (Root_meta_external_format_json_metamodel_JSONSchemaInteger) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaInteger_Impl(""), schemaObject));
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaNull visit(JSONSchemaNull schemaObject)
    {
        return (Root_meta_external_format_json_metamodel_JSONSchemaNull) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaNull_Impl(""), schemaObject);
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaNumber visit(JSONSchemaNumber schemaObject)
    {
        return loadCommonNumberProperties(schemaObject, (Root_meta_external_format_json_metamodel_JSONSchemaNumber) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaNumber_Impl(""), schemaObject));
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaObject visit(JSONSchemaObject schemaObject)
    {
        Root_meta_external_format_json_metamodel_JSONSchemaObject metaSchemaBuilder = (Root_meta_external_format_json_metamodel_JSONSchemaObject) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaObject_Impl(""), schemaObject);
        if (schemaObject.minProperties != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._minProperties(Long.valueOf(schemaObject.minProperties));
        }
        if (schemaObject.maxProperties != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._maxProperties(Long.valueOf(schemaObject.maxProperties));
        }
        if (schemaObject.propertyNames != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._propertyNames(Lists.mutable.with((Root_meta_external_format_json_metamodel_JSONSchemaString) schemaObject.propertyNames.accept(this)));
        }
        if (schemaObject.properties != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._properties(new PureMap(mapToMetaModel(schemaObject.properties)));
        }
        if (schemaObject.requiredProperties != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._requiredProperties(ListIterate.collect(schemaObject.requiredProperties, x -> x));
        }
        if (schemaObject.additionalProperties != null)
        {
            if (schemaObject.additionalProperties instanceof Boolean)
            {
                metaSchemaBuilder._additionalProperties(Lists.mutable.with(schemaObject.additionalProperties));
            }
            else if (schemaObject.additionalProperties instanceof JSONSchema)
            {
                metaSchemaBuilder._additionalProperties(Lists.mutable.with(((JSONSchema) schemaObject.additionalProperties).accept(this)));
            }
            else
            {
                throw new EngineException("Unable to compile additionalProperties. Supported types for additionalProperties are Boolean, JSONSchema. Received: " + schemaObject.additionalProperties.getClass().getName());
            }
        }
        return metaSchemaBuilder;
    }

    @Override
    public Root_meta_external_format_json_metamodel_JSONSchemaString visit(JSONSchemaString schemaObject)
    {
        Root_meta_external_format_json_metamodel_JSONSchemaString metaSchemaBuilder = (Root_meta_external_format_json_metamodel_JSONSchemaString) loadCommonProperties(new Root_meta_external_format_json_metamodel_JSONSchemaString_Impl(""), schemaObject);

        if (schemaObject.minLength != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._minLength(Long.valueOf(schemaObject.minLength));
        }
        if (schemaObject.maxLength != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._maxLength(Long.valueOf(schemaObject.maxLength));
        }
        if (schemaObject.pattern != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._pattern(schemaObject.pattern);
        }
        if (schemaObject.format != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._format(schemaObject.format);
        }
        return metaSchemaBuilder;
    }

    private Root_meta_external_format_json_metamodel_JSONSchema loadCommonProperties(Root_meta_external_format_json_metamodel_JSONSchema pureModel, JSONSchema parsedSchema)
    {
        if (parsedSchema.allOf != null)
        {
            pureModel = pureModel._allOf(listToPure(parsedSchema.allOf));
        }
        if (parsedSchema.oneOf != null)
        {
            pureModel = pureModel._oneOf(listToPure(parsedSchema.oneOf));
        }
        if (parsedSchema.anyOf != null)
        {
            pureModel = pureModel._anyOf(listToPure(parsedSchema.anyOf));
        }
        if (parsedSchema.mustNotMatch != null)
        {
            pureModel = pureModel._mustNotMatch(parsedSchema.mustNotMatch.accept(this));
        }
        if (parsedSchema.title != null)
        {
            pureModel = pureModel._title(parsedSchema.title);
        }
        if (parsedSchema.description != null)
        {
            pureModel = pureModel._description(parsedSchema.description);
        }
        if (parsedSchema.id != null)
        {
            pureModel = pureModel._id(parsedSchema.id);
        }
        if (parsedSchema.schema != null)
        {
            pureModel = pureModel._schema(parsedSchema.schema);
        }
        if (parsedSchema.defaultValue != null)
        {
            pureModel = pureModel._defaultValue(convertToJSONElement(parsedSchema.defaultValue));
        }
        if (parsedSchema.constantValue != null)
        {
            pureModel = pureModel._constantValue(convertToJSONElement(parsedSchema.constantValue));
        }
        if (parsedSchema.readOnly != null)
        {
            pureModel = pureModel._readOnly(parsedSchema.readOnly);
        }
        if (parsedSchema.writeOnly != null)
        {
            pureModel = pureModel._writeOnly(parsedSchema.writeOnly);
        }
        if (parsedSchema.nullable != null)
        {
            pureModel = pureModel._nullable(parsedSchema.nullable);
        }
        if (parsedSchema.definitions != null)
        {
            pureModel = pureModel._definitions(new PureMap(mapToMetaModel(parsedSchema.definitions)));
        }
        if (parsedSchema.ifCondition != null)
        {
            pureModel = pureModel._ifCondition(parsedSchema.ifCondition.accept(this));
        }
        if (parsedSchema.elseCondition != null)
        {
            pureModel = pureModel._elseCondition(parsedSchema.elseCondition.accept(this));
        }
        if (parsedSchema.thenCondition != null)
        {
            pureModel = pureModel._thenCondition(parsedSchema.thenCondition.accept(this));
        }
        if (parsedSchema.refValue != null)
        {
            pureModel = pureModel._refValue(parsedSchema.refValue);
        }
        if (parsedSchema.possibleValues != null)
        {
            pureModel = pureModel._possibleValues((Root_meta_json_JSONArray) convertToJSONElement(parsedSchema.possibleValues));
        }
        if (parsedSchema.example != null)
        {
            pureModel = pureModel._example(null);
        }
        if (parsedSchema.customProperties != null)
        {
            pureModel = pureModel._customProperties(new PureMap(parsedSchema.customProperties));
        }
        if (parsedSchema.discriminator != null)
        {
            Root_meta_external_format_json_metamodel_JSONSchemaDiscriminator discriminator = new Root_meta_external_format_json_metamodel_JSONSchemaDiscriminator_Impl("");
            if (parsedSchema.discriminator.mapping != null)
            {
                discriminator = discriminator._mapping(new PureMap(parsedSchema.discriminator.mapping));
            }
            if (parsedSchema.discriminator.propertyName != null)
            {
                discriminator = discriminator._propertyName(parsedSchema.discriminator.propertyName);
            }
            pureModel = pureModel._discriminator(discriminator);
        }
        return pureModel;
    }

    private MutableList<Root_meta_external_format_json_metamodel_JSONSchema> listToPure(List<JSONSchema> listOfSchemas)
    {
        if (listOfSchemas == null)
        {
            return null;
        }
        return ListIterate.collect(listOfSchemas, x -> x.accept(this));
    }

    private Map<String, Root_meta_external_format_json_metamodel_JSONSchema> mapToMetaModel(Map<String, JSONSchema> actualMap)
    {
        if (actualMap == null)
        {
            return null;
        }
        return actualMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().accept(this)));
    }

    private Root_meta_json_JSONElement convertToJSONElement(Object valueToConvert)
    {
        if (valueToConvert instanceof Boolean)
        {
            return new Root_meta_json_JSONBoolean_Impl("")._value((Boolean) valueToConvert);
        }
        else if (valueToConvert instanceof String)
        {
            return new Root_meta_json_JSONString_Impl("")._value((String) valueToConvert);
        }
        else if (valueToConvert instanceof Number)
        {
            return new Root_meta_json_JSONNumber_Impl("")._value((Number) valueToConvert);
        }
        else if (valueToConvert instanceof ArrayList)
        {
            return new Root_meta_json_JSONArray_Impl("")._values(Lists.mutable.withAll(((ArrayList<?>) valueToConvert).stream().map(this::convertToJSONElement).collect(Collectors.toList())));
        }
        else if (valueToConvert instanceof LinkedHashMap)
        {
            return new Root_meta_json_JSONObject_Impl("")._keyValuePairs(Lists.mutable.withAll(((LinkedHashMap<?, ?>) valueToConvert).entrySet().stream().map(e -> new Root_meta_json_JSONKeyValue_Impl("")._key((Root_meta_json_JSONString) convertToJSONElement(e.getKey()))._value(convertToJSONElement(e.getValue()))).collect(Collectors.toList())));
        }
        throw new EngineException("Unable to convert " + valueToConvert.getClass().getName() + " to Root_meta_json_JSONElement. Allowed Java types are Boolean, String, Number, ArrayList, LinkedHashMap.");
    }

    private static Root_meta_external_format_json_metamodel_JSONSchemaNumber loadCommonNumberProperties(JSONSchemaNumber typedSchema, Root_meta_external_format_json_metamodel_JSONSchemaNumber metaSchemaBuilder)
    {
        if (typedSchema.minimum != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._minimum((Long) typedSchema.minimum);
        }
        if (typedSchema.maximum != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._maximum((Long) typedSchema.maximum);
        }
        if (typedSchema.exclusiveMaximum != null)
        {
            if (typedSchema.exclusiveMaximum instanceof Number)
            {
                metaSchemaBuilder = metaSchemaBuilder._minimum((Long) typedSchema.exclusiveMaximum);
                metaSchemaBuilder = metaSchemaBuilder._exclusiveMaximum(true);
            }
            else if (typedSchema.exclusiveMaximum instanceof Boolean)
            {
                metaSchemaBuilder = metaSchemaBuilder._exclusiveMinimum((Boolean) typedSchema.exclusiveMaximum);
            }
            else
            {
                throw new EngineException("exclusiveMaximum can only be of instance Number or Boolean");
            }
        }
        if (typedSchema.exclusiveMinimum != null)
        {
            if (typedSchema.exclusiveMinimum instanceof Number)
            {
                metaSchemaBuilder = metaSchemaBuilder._minimum((Long) typedSchema.exclusiveMinimum);
                metaSchemaBuilder = metaSchemaBuilder._exclusiveMinimum(true);
            }
            else if (typedSchema.exclusiveMinimum instanceof Boolean)
            {
                metaSchemaBuilder = metaSchemaBuilder._exclusiveMinimum((Boolean) typedSchema.exclusiveMinimum);
            }
            else
            {
                throw new EngineException("exclusiveMinimum can only be of instance Number or Boolean");
            }
        }
        if (typedSchema.multipleOf != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._multipleOf((Long) typedSchema.multipleOf);
        }
        if (typedSchema.format != null)
        {
            metaSchemaBuilder = metaSchemaBuilder._format(typedSchema.format);
        }
        return metaSchemaBuilder;
    }
}
