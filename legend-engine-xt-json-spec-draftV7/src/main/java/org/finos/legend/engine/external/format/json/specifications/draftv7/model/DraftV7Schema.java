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

package org.finos.legend.engine.external.format.json.specifications.draftv7.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.finos.legend.engine.external.format.json.specifications.draftv7.visitor.DraftV7SchemaVisitor;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@JsonInclude(Include.NON_NULL)
@JsonDeserialize(
        builder = DraftV7Schema.DraftV7SchemaBuilder.class,
        using = DraftV7Schema.TypeDeserializer.class
)
public class DraftV7Schema
{
    public final List<DraftV7Schema> allOf;
    public final List<DraftV7Schema> oneOf;
    public final List<DraftV7Schema> anyOf;
    public final String title;
    public final String description;
    @JsonProperty("id")
    @JsonAlias({"$id"})
    public final String id;
    @JsonProperty("$schema")
    public final String schema;
    @JsonProperty("default")
    public final Object defaultValue;
    @JsonProperty("const")
    public final Object constantValue;
    public final Boolean readOnly;
    public final Boolean writeOnly;
    public final Map<String, DraftV7Schema> definitions;
    @JsonProperty("not")
    public final DraftV7Schema mustNotMatch;
    @JsonProperty("if")
    public final DraftV7Schema ifCondition;
    @JsonProperty("else")
    public final DraftV7Schema elseCondition;
    @JsonProperty("then")
    public final DraftV7Schema thenCondition;
    @JsonProperty("$ref")
    public final String refValue;
    @JsonProperty("enum")
    public final List<Object> possibleValues;
    public final Object example;
    @JsonFormat(with = {JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED})
    public final List<String> type;
    @JsonAnySetter
    public final Map<String, Object> customProperties;
    public final String contentMediaType;
    public final String contentEncoding;

    protected DraftV7Schema(DraftV7Schema.DraftV7SchemaBuilder<?> b)
    {
        this.allOf = b.allOf;
        this.oneOf = b.oneOf;
        this.anyOf = b.anyOf;
        this.title = b.title;
        this.description = b.description;
        this.id = b.id;
        this.schema = b.schema;
        this.defaultValue = b.defaultValue;
        this.constantValue = b.constantValue;
        this.readOnly = b.readOnly;
        this.writeOnly = b.writeOnly;
        this.definitions = b.definitions;
        this.mustNotMatch = b.mustNotMatch;
        this.ifCondition = b.ifCondition;
        this.elseCondition = b.elseCondition;
        this.thenCondition = b.thenCondition;
        this.refValue = b.refValue;
        this.possibleValues = b.possibleValues;
        this.example = b.example;
        this.type = b.type;
        this.contentMediaType = b.contentMediaType;
        this.contentEncoding = b.contentEncoding;
        Map<String, Object> customProperties;
        switch (b.customPropertiesKey == null ? 0 : b.customPropertiesKey.size())
        {
            case 0:
                customProperties = Collections.emptyMap();
                break;
            case 1:
                customProperties = Collections.singletonMap(b.customPropertiesKey.get(0), b.customPropertiesValue.get(0));
                break;
            default:
                customProperties = new LinkedHashMap<>(b.customPropertiesKey.size() < 1073741824 ? 1 + b.customPropertiesKey.size() + (b.customPropertiesKey.size() - 3) / 3 : 2147483647);

                for (int i = 0; i < b.customPropertiesKey.size(); ++i)
                {
                    customProperties.put(b.customPropertiesKey.get(i), b.customPropertiesValue.get(i));
                }

                customProperties = Collections.unmodifiableMap(customProperties);
        }

        this.customProperties = customProperties;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class DraftV7SchemaBuilder<B extends DraftV7Schema.DraftV7SchemaBuilder<B>>
    {
        private List<DraftV7Schema> allOf;
        private List<DraftV7Schema> oneOf;
        private List<DraftV7Schema> anyOf;
        private String title;
        private String description;
        private String id;
        private String schema;
        private Object defaultValue;
        private Object constantValue;
        private Boolean readOnly;
        private Boolean writeOnly;
        private Map<String, DraftV7Schema> definitions;
        private DraftV7Schema mustNotMatch;
        private DraftV7Schema ifCondition;
        private DraftV7Schema elseCondition;
        private DraftV7Schema thenCondition;
        private String refValue;
        private List<Object> possibleValues;
        private Object example;
        private List<String> type;
        private ArrayList<String> customPropertiesKey;
        private ArrayList<Object> customPropertiesValue;
        private String contentMediaType;
        private String contentEncoding;

        @SuppressWarnings("unchecked")
        protected B self()
        {
            return (B) this;
        }

        public DraftV7Schema build()
        {
            return new DraftV7Schema(this);
        }


        public B allOf(List<DraftV7Schema> allOf)
        {
            this.allOf = allOf;
            return this.self();
        }

        public B oneOf(List<DraftV7Schema> oneOf)
        {
            this.oneOf = oneOf;
            return this.self();
        }

        public B anyOf(List<DraftV7Schema> anyOf)
        {
            this.anyOf = anyOf;
            return this.self();
        }

        public B title(String title)
        {
            this.title = title;
            return this.self();
        }

        public B description(String description)
        {
            this.description = description;
            return this.self();
        }

        @JsonProperty("id")
        @JsonAlias({"$id"})
        public B id(String id)
        {
            this.id = id;
            return this.self();
        }

        @JsonProperty("$schema")
        public B schema(String schema)
        {
            this.schema = schema;
            return this.self();
        }

        @JsonProperty("default")
        public B defaultValue(Object defaultValue)
        {
            this.defaultValue = defaultValue;
            return this.self();
        }

        @JsonProperty("const")
        public B constantValue(Object constantValue)
        {
            this.constantValue = constantValue;
            return this.self();
        }

        public B readOnly(Boolean readOnly)
        {
            this.readOnly = readOnly;
            return this.self();
        }

        public B writeOnly(Boolean writeOnly)
        {
            this.writeOnly = writeOnly;
            return this.self();
        }

        public B definitions(Map<String, DraftV7Schema> definitions)
        {
            this.definitions = definitions;
            return this.self();
        }

        @JsonProperty("not")
        public B mustNotMatch(DraftV7Schema mustNotMatch)
        {
            this.mustNotMatch = mustNotMatch;
            return this.self();
        }

        @JsonProperty("if")
        public B ifCondition(DraftV7Schema ifCondition)
        {
            this.ifCondition = ifCondition;
            return this.self();
        }

        @JsonProperty("else")
        public B elseCondition(DraftV7Schema elseCondition)
        {
            this.elseCondition = elseCondition;
            return this.self();
        }

        @JsonProperty("then")
        public B thenCondition(DraftV7Schema thenCondition)
        {
            this.thenCondition = thenCondition;
            return this.self();
        }

        @JsonProperty("$ref")
        public B refValue(String refValue)
        {
            this.refValue = refValue;
            return this.self();
        }

        @JsonProperty("enum")
        public B possibleValues(List<Object> possibleValues)
        {
            this.possibleValues = possibleValues;
            return this.self();
        }

        public B example(Object example)
        {
            this.example = example;
            return this.self();
        }

        @JsonFormat(with = {JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED})
        public B type(List<String> type)
        {
            this.type = type;
            return this.self();
        }

        @JsonAnySetter
        public B any(String anyKey, Object anyValue)
        {
            if (this.customPropertiesKey == null)
            {
                this.customPropertiesKey = new ArrayList<>();
                this.customPropertiesValue = new ArrayList<>();
            }

            this.customPropertiesKey.add(anyKey);
            this.customPropertiesValue.add(anyValue);
            return this.self();
        }

        public B customProperties(Map<? extends String, ? extends Object> customProperties)
        {
            if (customProperties != null)
            {
                if (this.customPropertiesKey == null)
                {
                    this.customPropertiesKey = new ArrayList<>();
                    this.customPropertiesValue = new ArrayList<>();
                }

                Iterator var2 = customProperties.entrySet().iterator();

                while (var2.hasNext())
                {
                    Entry<String, ?> mapEntry = (Entry) var2.next();
                    this.customPropertiesKey.add(mapEntry.getKey());
                    this.customPropertiesValue.add(mapEntry.getValue());
                }

            }
            return this.self();
        }

        public B clearCustomProperties()
        {
            if (this.customPropertiesKey != null)
            {
                this.customPropertiesKey.clear();
                this.customPropertiesValue.clear();
            }

            return this.self();
        }

        public B contentMediaType(String contentMediaType)
        {
            this.contentMediaType = contentMediaType;
            return this.self();
        }

        public B contentEncoding(String contentEncoding)
        {
            this.contentEncoding = contentEncoding;
            return this.self();
        }

    }

    public static class TypeDeserializer extends JsonDeserializer
    {
        private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        @Override
        public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode tree = jsonParser.readValueAsTree();
            if (!tree.has("type"))
            {
                return objectMapper.treeToValue(tree, DraftV7SchemaFragment.class);
            }
            if (tree.get("type").isArray())
            {
                List<String> type = objectMapper.convertValue(tree.get("type"), ArrayList.class);
                if (type.contains("null") && type.size() == 2)
                {
                    type.remove("null");
                    ObjectNode modifiedJsonNode = (ObjectNode) tree;
                    modifiedJsonNode.replace("type", new TextNode(type.get(0)));
                    if (modifiedJsonNode.has("customProperties"))
                    {
                        modifiedJsonNode.replace("customProperties", ((ObjectNode) modifiedJsonNode.get("customProperties")).put("nullable", true));
                    }
                    else
                    {
                        modifiedJsonNode.set("customProperties", objectMapper.createObjectNode().put("nullable", true));
                    }
                    return objectMapper.treeToValue(modifiedJsonNode, getClassForType(type.get(0)));
                }
                else
                {
                    return objectMapper.treeToValue(tree, DraftV7SchemaMultiType.class);
                }
            }

            return objectMapper.treeToValue(tree, getClassForType(tree.get("type").textValue()));
        }

        private Class<?> getClassForType(String type)
        {
            Class<?> configType = DraftV7SchemaFragment.class; // default class will be DraftV7SchemaFragment, in case nothing matches

            switch (type)
            {
                case "object":
                    configType = DraftV7SchemaObject.class;
                    break;
                case "integer":
                    configType = DraftV7SchemaInteger.class;
                    break;
                case "number":
                    configType = DraftV7SchemaNumber.class;
                    break;
                case "string":
                    configType = DraftV7SchemaString.class;
                    break;
                case "boolean":
                    configType = DraftV7SchemaBoolean.class;
                    break;
                case "array":
                    configType = DraftV7SchemaArray.class;
                    break;
                case "null":
                    configType = DraftV7SchemaNull.class;
                    break;
            }
            return configType;
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getCustomProperties()
    {
        return new LinkedHashMap<>(this.customProperties);
    }

    public <T> T accept(DraftV7SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static DraftV7Schema.DraftV7SchemaBuilder<?> builder()
    {
        return new DraftV7Schema.DraftV7SchemaBuilder<>();
    }
}
