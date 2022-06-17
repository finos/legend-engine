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

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.format.json.model.JSONSchema;
import org.finos.legend.engine.external.format.json.model.JSONSchemaArray;
import org.finos.legend.engine.external.format.json.model.JSONSchemaBoolean;
import org.finos.legend.engine.external.format.json.model.JSONSchemaDiscriminator;
import org.finos.legend.engine.external.format.json.model.JSONSchemaFragment;
import org.finos.legend.engine.external.format.json.model.JSONSchemaInteger;
import org.finos.legend.engine.external.format.json.model.JSONSchemaMultiType;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNull;
import org.finos.legend.engine.external.format.json.model.JSONSchemaNumber;
import org.finos.legend.engine.external.format.json.model.JSONSchemaObject;
import org.finos.legend.engine.external.format.json.model.JSONSchemaString;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaArray;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaBoolean;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaFragment;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaInteger;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaMultiType;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNull;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaNumber;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaObject;
import org.finos.legend.pure.generated.Root_meta_external_format_json_metamodel_JSONSchemaString;
import org.finos.legend.pure.generated.Root_meta_json_JSONArray;
import org.finos.legend.pure.generated.Root_meta_json_JSONBoolean;
import org.finos.legend.pure.generated.Root_meta_json_JSONElement;
import org.finos.legend.pure.generated.Root_meta_json_JSONNumber;
import org.finos.legend.pure.generated.Root_meta_json_JSONObject;
import org.finos.legend.pure.generated.Root_meta_json_JSONString;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONSchemaMetamodelParserVisitor implements JSONSchemaMetamodelVisitor<JSONSchema>
{

    @Override
    public JSONSchemaArray visit(Root_meta_external_format_json_metamodel_JSONSchemaArray schemaObject)
    {
        JSONSchemaArray.JSONSchemaArrayBuilder protocolModelBuilder = (JSONSchemaArray.JSONSchemaArrayBuilder) loadCommonProperties(schemaObject, JSONSchemaArray.builder());
        if (schemaObject._itemSchemas() != null && schemaObject._itemSchemas().size() == 1)
        {
            if (schemaObject._itemSchemas().getAny() instanceof Root_meta_json_JSONBoolean)
            {
                protocolModelBuilder.itemSchemas(((Root_meta_json_JSONBoolean) schemaObject._itemSchemas().getAny())._value());
            }
            else if (schemaObject._itemSchemas().getAny() instanceof Root_meta_external_format_json_metamodel_JSONSchema)
            {
                protocolModelBuilder = protocolModelBuilder.itemSchemas(this.visit((Root_meta_external_format_json_metamodel_JSONSchema) schemaObject._itemSchemas().getAny()));
            }
        }
        return protocolModelBuilder
                .minItems(schemaObject._minItems() != null ? schemaObject._minItems().intValue() : null)
                .maxItems(schemaObject._maxItems() != null ? schemaObject._maxItems().intValue() : null)
                .uniqueItems(schemaObject._uniqueItems())
                .build();
    }

    @Override
    public JSONSchemaBoolean visit(Root_meta_external_format_json_metamodel_JSONSchemaBoolean schemaObject)
    {
        return (JSONSchemaBoolean) loadCommonProperties(schemaObject, JSONSchemaBoolean.builder()).build();
    }

    @Override
    public JSONSchemaFragment visit(Root_meta_external_format_json_metamodel_JSONSchemaFragment schemaObject)
    {
        return (JSONSchemaFragment) loadCommonProperties(schemaObject, JSONSchemaFragment.builder()).build();
    }

    @Override
    public JSONSchemaMultiType visit(Root_meta_external_format_json_metamodel_JSONSchemaMultiType schemaObject)
    {
        throw new EngineException("Multiple JSONSchema types are currently not supported");
    }

    @Override
    public JSONSchemaInteger visit(Root_meta_external_format_json_metamodel_JSONSchemaInteger schemaObject)
    {
        return (JSONSchemaInteger) loadCommonNumberProperties(schemaObject, (JSONSchemaInteger.JSONSchemaIntegerBuilder) loadCommonProperties(schemaObject, JSONSchemaInteger.builder())).build();
    }

    @Override
    public JSONSchemaNull visit(Root_meta_external_format_json_metamodel_JSONSchemaNull schemaObject)
    {
        return (JSONSchemaNull) loadCommonProperties(schemaObject, JSONSchemaNull.builder()).build();
    }

    @Override
    public JSONSchemaNumber visit(Root_meta_external_format_json_metamodel_JSONSchemaNumber schemaObject)
    {
        return loadCommonNumberProperties(schemaObject, (JSONSchemaNumber.JSONSchemaNumberBuilder<?>) loadCommonProperties(schemaObject, JSONSchemaNumber.builder())).build();
    }

    @Override
    public JSONSchemaObject visit(Root_meta_external_format_json_metamodel_JSONSchemaObject schemaObject)
    {
        JSONSchemaObject.JSONSchemaObjectBuilder protocolModelBuilder = (JSONSchemaObject.JSONSchemaObjectBuilder) loadCommonProperties(schemaObject, JSONSchemaObject.builder());
        if (schemaObject._requiredProperties() != null && schemaObject._requiredProperties().size() != 0)
        {
            List<String> requiredProperties = new ArrayList<>();
            schemaObject._requiredProperties().forEach(requiredProperties::add);
            protocolModelBuilder = protocolModelBuilder.requiredProperties(requiredProperties);
        }
        if (schemaObject._additionalProperties() != null)
        {
            if (schemaObject._additionalProperties() instanceof Boolean)
            {
                protocolModelBuilder = protocolModelBuilder.additionalProperties(schemaObject._additionalProperties());
            }
            else if (schemaObject._additionalProperties() instanceof Root_meta_external_format_json_metamodel_JSONSchema)
            {
                protocolModelBuilder = protocolModelBuilder.additionalProperties(this.visit((Root_meta_external_format_json_metamodel_JSONSchema) schemaObject._additionalProperties()));
            }
            else
            {
                throw new EngineException("Unable to compile additionalProperties. Supported types for additionalProperties are Boolean, Root_meta_external_format_json_metamodel_JSONSchema. Received: " + schemaObject._additionalProperties().getClass().getName());
            }
        }
        return protocolModelBuilder
                .minProperties(schemaObject._minProperties() != null ? schemaObject._minProperties().intValue() : null)
                .maxProperties(schemaObject._maxProperties() != null ? schemaObject._maxProperties().intValue() : null)
                .propertyNames(schemaObject._propertyNames() != null ? this.visit(schemaObject._propertyNames()) : null)
                .properties(mapToProtocolModel(schemaObject._properties()))
                .patternProperties(schemaObject._patternProperties())
                .build();
    }

    @Override
    public JSONSchemaString visit(Root_meta_external_format_json_metamodel_JSONSchemaString schemaObject)
    {
        JSONSchemaString.JSONSchemaStringBuilder protocolModelBuilder = (JSONSchemaString.JSONSchemaStringBuilder) loadCommonProperties(schemaObject, JSONSchemaString.builder());
        return protocolModelBuilder
                .minLength(schemaObject._minLength() != null ? schemaObject._minLength().intValue() : null)
                .maxLength(schemaObject._maxLength() != null ? schemaObject._maxLength().intValue() : null)
                .pattern(schemaObject._pattern())
                .format(schemaObject._format())
                .build();
    }


    @Override
    public JSONSchema visit(Root_meta_external_format_json_metamodel_JSONSchema schemaObject)
    {
        if (schemaObject == null)
        {
            return null;
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaArray)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaArray) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaBoolean)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaBoolean) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaFragment)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaFragment) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaMultiType)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaMultiType) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaInteger)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaInteger) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaNull)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaNull) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaNumber)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaNumber) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaObject)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaObject) schemaObject);
        }
        else if (schemaObject instanceof Root_meta_external_format_json_metamodel_JSONSchemaString)
        {
            return this.visit((Root_meta_external_format_json_metamodel_JSONSchemaString) schemaObject);
        }
        else
        {
            throw new EngineException("Cannot visit Root_meta_external_format_json_metamodel_JSONSchema, please use the subclasses");
        }
    }


    private JSONSchema.JSONSchemaBuilder<?> loadCommonProperties(Root_meta_external_format_json_metamodel_JSONSchema loadedSchema, JSONSchema.JSONSchemaBuilder<?> protocolSchemaBuilder)
    {
        if (loadedSchema._discriminator() != null)
        {
            Map<String, String> discMapping = new LinkedHashMap<>();
            loadedSchema._discriminator()._mapping().getMap().forEachKeyValue((o, o2) -> discMapping.put((String) o, (String) o2));
            JSONSchemaDiscriminator discriminator = JSONSchemaDiscriminator.builder().propertyName(loadedSchema._discriminator()._propertyName()).mapping(discMapping).build();
            protocolSchemaBuilder = protocolSchemaBuilder.discriminator(discriminator);
        }
        return protocolSchemaBuilder
                .allOf(listToProtocolModel(loadedSchema._allOf()))
                .oneOf(listToProtocolModel(loadedSchema._oneOf()))
                .anyOf(listToProtocolModel(loadedSchema._anyOf()))
                .title(loadedSchema._title())
                .description(loadedSchema._description())
                .id(loadedSchema._id())
                .schema(loadedSchema._schema())
                .defaultValue(convertToJavaType(loadedSchema._defaultValue()))
                .constantValue(convertToJavaType(loadedSchema._constantValue()))
                .readOnly(loadedSchema._readOnly())
                .writeOnly(loadedSchema._writeOnly())
                .nullable(loadedSchema._nullable())
                .definitions(mapToProtocolModel(loadedSchema._definitions()))
                .mustNotMatch(loadedSchema._mustNotMatch() != null ? this.visit(loadedSchema._mustNotMatch()) : null)
                .ifCondition(loadedSchema._ifCondition() != null ? this.visit(loadedSchema._ifCondition()) : null)
                .elseCondition(loadedSchema._elseCondition() != null ? this.visit(loadedSchema._elseCondition()) : null)
                .thenCondition(loadedSchema._thenCondition() != null ? this.visit(loadedSchema._thenCondition()) : null)
                .refValue(loadedSchema._refValue())
                .possibleValues((List<Object>) convertToJavaType(loadedSchema._possibleValues()))
                .example(convertToJavaType(loadedSchema._example()))
                .customProperties((Map<String, Object>) convertToJavaType(loadedSchema._customProperties()))
                .contentEncoding(loadedSchema._contentEncoding())
                .contentMediaType(loadedSchema._contentMediaType());
    }

    private Object convertToJavaType(Root_meta_json_JSONElement valueToConvert)
    {
        if (valueToConvert == null)
        {
            return null;
        }
        else if (valueToConvert instanceof Root_meta_json_JSONBoolean)
        {
            return ((Root_meta_json_JSONBoolean) valueToConvert)._value();
        }
        else if (valueToConvert instanceof Root_meta_json_JSONString)
        {
            return ((Root_meta_json_JSONString) valueToConvert)._value();
        }
        else if (valueToConvert instanceof Root_meta_json_JSONNumber)
        {
            return ((Root_meta_json_JSONNumber) valueToConvert)._value();
        }
        else if (valueToConvert instanceof Root_meta_json_JSONArray)
        {
            List<Object> arrayList = new ArrayList<>();
            ((Root_meta_json_JSONArray) valueToConvert)._values().forEach(x -> arrayList.add(convertToJavaType(x)));
            return arrayList.size() > 0 ? arrayList : null;
        }
        else if (valueToConvert instanceof Root_meta_json_JSONObject)
        {
            Map<String, Object> convertedMap = new LinkedHashMap<>();
            ((Root_meta_json_JSONObject) valueToConvert)._keyValuePairs().forEach(root_meta_json_jsonKeyValue -> convertedMap.put(root_meta_json_jsonKeyValue._key()._value(), convertToJavaType(root_meta_json_jsonKeyValue._value())));
            return convertedMap.size() > 0 ? convertedMap : null;
        }
        throw new EngineException("Unable to convert " + valueToConvert.getClass().getName() + " to Java Type. Allowed PURE types are Root_meta_json_JSONBoolean, Root_meta_json_JSONString, Root_meta_json_JSONNumber, Root_meta_json_JSONArray, Root_meta_json_JSONObject.");
    }

    private List<JSONSchema> listToProtocolModel(RichIterable<? extends Root_meta_external_format_json_metamodel_JSONSchema> actualList)
    {
        if (actualList == null || actualList.size() == 0)
        {
            return null;
        }
        List<JSONSchema> arrayList = new ArrayList<>();
        actualList.forEach(x -> arrayList.add(this.visit(x)));
        return arrayList;
    }

    private Map<String, JSONSchema> mapToProtocolModel(PureMap actualMap)
    {
        if (actualMap == null || actualMap.getMap() == null || actualMap.getMap().size() == 0)
        {
            return null;
        }
        Map<String, JSONSchema> convertedMap = new LinkedHashMap<>();
        actualMap.getMap().forEachKeyValue((o, o2) -> convertedMap.put((String) o, this.visit((Root_meta_external_format_json_metamodel_JSONSchema) o2)));
        return convertedMap;
    }

    private JSONSchemaNumber.JSONSchemaNumberBuilder<?> loadCommonNumberProperties(Root_meta_external_format_json_metamodel_JSONSchemaNumber loadedSchema, JSONSchemaNumber.JSONSchemaNumberBuilder<?> protocolSchemaBuilder)
    {

        return protocolSchemaBuilder
                .minimum(loadedSchema._minimum())
                .maximum(loadedSchema._maximum())
                .exclusiveMaximum(loadedSchema._exclusiveMaximum())
                .exclusiveMinimum(loadedSchema._exclusiveMinimum())
                .multipleOf(loadedSchema._multipleOf())
                .format(loadedSchema._format());
    }

}
