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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.visitor.OpenAPIv3_0_3SchemaVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@JsonTypeInfo(use = Id.NAME, property = "type", defaultImpl = OpenAPIv3_0_3SchemaEmpty.class, include = As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
        @Type(value = OpenAPIv3_0_3SchemaObject.class, name = "object"),
        @Type(value = OpenAPIv3_0_3SchemaInteger.class, name = "integer"),
        @Type(value = OpenAPIv3_0_3SchemaNumber.class, name = "number"),
        @Type(value = OpenAPIv3_0_3SchemaString.class, name = "string"),
        @Type(value = OpenAPIv3_0_3SchemaBoolean.class, name = "boolean"),
        @Type(value = OpenAPIv3_0_3SchemaArray.class, name = "array"),
        @Type(value = OpenAPIv3_0_3SchemaNull.class, name = "null")}
)
@JsonInclude(Include.NON_NULL)
@JsonDeserialize(
        builder = OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder.class
)
public class OpenAPIv3_0_3Schema
{
    public static final String XML_KEY = "x-xml";
    public static final String EXTERNAL_DOCS_KEY = "x-externalDocs";
    public final List<OpenAPIv3_0_3Schema> allOf;
    public final List<OpenAPIv3_0_3Schema> oneOf;
    public final List<OpenAPIv3_0_3Schema> anyOf;
    @JsonProperty("not")
    public final OpenAPIv3_0_3Schema mustNotMatch;
    public final String title;
    public final String description;
    @JsonProperty("default")
    public final Object defaultValue;
    public final Boolean readOnly;
    public final Boolean writeOnly;
    public final Boolean nullable;
    public final Boolean deprecated;
    @JsonProperty("$ref")
    public final String refValue;
    @JsonProperty("enum")
    public final List<Object> possibleValues;
    public final Object example;
    public final OpenAPIv3_0_3Discriminator discriminator;
    public final String type;
    public final OpenAPIv3_0_3XML xml;
    public final OpenAPIv3_0_3ExternalDoc externalDocs;
    @JsonProperty("$schema")
    public final String schema;
    @JsonAnySetter
    public final Map<String, Object> customProperties;

    protected OpenAPIv3_0_3Schema(OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<?> b)
    {
        this.allOf = b.allOf;
        this.oneOf = b.oneOf;
        this.anyOf = b.anyOf;
        this.mustNotMatch = b.mustNotMatch;
        this.title = b.title;
        this.description = b.description;
        this.defaultValue = b.defaultValue;
        this.readOnly = b.readOnly;
        this.writeOnly = b.writeOnly;
        this.nullable = b.nullable;
        this.deprecated = b.deprecated;
        this.refValue = b.refValue;
        this.possibleValues = b.possibleValues;
        this.example = b.example;
        this.discriminator = b.discriminator;
        this.type = b.type;
        this.xml = b.xml;
        this.externalDocs = b.externalDocs;
        this.schema = b.schema;
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

    @JsonTypeInfo(use = Id.NAME, property = "type", defaultImpl = OpenAPIv3_0_3SchemaEmpty.class, include = As.EXISTING_PROPERTY, visible = true)
    @JsonSubTypes({
            @Type(value = OpenAPIv3_0_3SchemaObject.class, name = "object"),
            @Type(value = OpenAPIv3_0_3SchemaInteger.class, name = "integer"),
            @Type(value = OpenAPIv3_0_3SchemaNumber.class, name = "number"),
            @Type(value = OpenAPIv3_0_3SchemaString.class, name = "string"),
            @Type(value = OpenAPIv3_0_3SchemaBoolean.class, name = "boolean"),
            @Type(value = OpenAPIv3_0_3SchemaArray.class, name = "array"),
            @Type(value = OpenAPIv3_0_3SchemaNull.class, name = "null")}
    )
    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3SchemaBuilder<B extends OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<B>>
    {
        private List<OpenAPIv3_0_3Schema> allOf;
        private List<OpenAPIv3_0_3Schema> oneOf;
        private List<OpenAPIv3_0_3Schema> anyOf;
        private OpenAPIv3_0_3Schema mustNotMatch;
        private String title;
        private String description;
        private Object defaultValue;
        private Boolean readOnly;
        private Boolean writeOnly;
        private Boolean nullable;
        private Boolean deprecated;
        private String refValue;
        private List<Object> possibleValues;
        private Object example;
        private OpenAPIv3_0_3Discriminator discriminator;
        private String type;
        private OpenAPIv3_0_3XML xml;
        private OpenAPIv3_0_3ExternalDoc externalDocs;
        private String schema;
        private ArrayList<String> customPropertiesKey;
        private ArrayList<Object> customPropertiesValue;

        @SuppressWarnings("unchecked")
        protected B self()
        {
            return (B) this;
        }

        public OpenAPIv3_0_3Schema build()
        {
            return new OpenAPIv3_0_3Schema(this);
        }

        public B allOf(List<OpenAPIv3_0_3Schema> allOf)
        {
            this.allOf = allOf;
            return this.self();
        }

        public B oneOf(List<OpenAPIv3_0_3Schema> oneOf)
        {
            this.oneOf = oneOf;
            return this.self();
        }

        public B anyOf(List<OpenAPIv3_0_3Schema> anyOf)
        {
            this.anyOf = anyOf;
            return this.self();
        }

        @JsonProperty("not")
        public B mustNotMatch(OpenAPIv3_0_3Schema mustNotMatch)
        {
            this.mustNotMatch = mustNotMatch;
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

        @JsonProperty("default")
        public B defaultValue(Object defaultValue)
        {
            this.defaultValue = defaultValue;
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

        public B nullable(Boolean nullable)
        {
            this.nullable = nullable;
            return this.self();
        }

        public B deprecated(Boolean deprecated)
        {
            this.deprecated = deprecated;
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

        public B discriminator(OpenAPIv3_0_3Discriminator discriminator)
        {
            this.discriminator = discriminator;
            return this.self();
        }

        public B type(String type)
        {
            this.type = type;
            return this.self();
        }

        public B xml(OpenAPIv3_0_3XML xml)
        {
            this.xml = xml;
            return this.self();
        }

        public B externalDocs(OpenAPIv3_0_3ExternalDoc externalDocs)
        {
            this.externalDocs = externalDocs;
            return this.self();
        }

        @JsonProperty("$schema")
        public B schema(String schema)
        {
            this.schema = schema;
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
            if (customProperties == null)
            {
                throw new NullPointerException("customProperties cannot be null");
            }
            else
            {
                if (this.customPropertiesKey == null)
                {
                    this.customPropertiesKey = new ArrayList<>();
                    this.customPropertiesValue = new ArrayList<>();
                }

                Iterator var2 = customProperties.entrySet().iterator();

                while (var2.hasNext())
                {
                    Entry<? extends String, ? extends Object> mapEntry = (Entry) var2.next();
                    this.customPropertiesKey.add(mapEntry.getKey());
                    this.customPropertiesValue.add(mapEntry.getValue());
                }

                return this.self();
            }
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
    }

    @JsonAnyGetter
    public Map<String, Object> getCustomProperties()
    {
        return new LinkedHashMap<>(this.customProperties);
    }

    public <T> T accept(OpenAPIv3_0_3SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<?> builder()
    {
        return new OpenAPIv3_0_3Schema.OpenAPIv3_0_3SchemaBuilder<>();
    }

}
