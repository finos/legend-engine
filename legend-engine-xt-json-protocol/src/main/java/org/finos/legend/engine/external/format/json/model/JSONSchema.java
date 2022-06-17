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

package org.finos.legend.engine.external.format.json.model;

import org.finos.legend.engine.external.format.json.visitor.JSONSchemaVisitor;

import java.util.List;
import java.util.Map;

public class JSONSchema
{
    public final List<JSONSchema> allOf;
    public final List<JSONSchema> oneOf;
    public final List<JSONSchema> anyOf;
    public final JSONSchema mustNotMatch;
    public final String title;
    public final String description;
    public final String id;
    public final String schema;
    public final Object defaultValue;
    public final Object constantValue;
    public final Boolean readOnly;
    public final Boolean writeOnly;
    public final Boolean nullable;
    public final Map<String, JSONSchema> definitions;
    public final JSONSchema ifCondition;
    public final JSONSchema elseCondition;
    public final JSONSchema thenCondition;
    public final String refValue;
    public final List<Object> possibleValues;
    public final Object example;
    public final Map<String, Object> customProperties;
    public final JSONSchemaDiscriminator discriminator;
    public final String contentMediaType;
    public final String contentEncoding;

    protected JSONSchema(JSONSchemaBuilder<?> b)
    {
        this.allOf = b.allOf;
        this.oneOf = b.oneOf;
        this.anyOf = b.anyOf;
        this.mustNotMatch = b.mustNotMatch;
        this.title = b.title;
        this.description = b.description;
        this.id = b.id;
        this.schema = b.schema;
        this.defaultValue = b.defaultValue;
        this.constantValue = b.constantValue;
        this.readOnly = b.readOnly;
        this.writeOnly = b.writeOnly;
        this.nullable = b.nullable;
        this.definitions = b.definitions;
        this.ifCondition = b.ifCondition;
        this.elseCondition = b.elseCondition;
        this.thenCondition = b.thenCondition;
        this.refValue = b.refValue;
        this.possibleValues = b.possibleValues;
        this.example = b.example;
        this.customProperties = b.customProperties;
        this.discriminator = b.discriminator;
        this.contentMediaType = b.contentMediaType;
        this.contentEncoding = b.contentEncoding;
    }

    public static class JSONSchemaBuilder<B extends JSONSchema.JSONSchemaBuilder<B>>
    {
        private List<JSONSchema> allOf;
        private List<JSONSchema> oneOf;
        private List<JSONSchema> anyOf;
        private JSONSchema mustNotMatch;
        private String title;
        private String description;
        private String id;
        private String schema;
        private Object defaultValue;
        private Object constantValue;
        private Boolean readOnly;
        private Boolean writeOnly;
        private Boolean nullable;
        private Map<String, JSONSchema> definitions;
        private JSONSchema ifCondition;
        private JSONSchema elseCondition;
        private JSONSchema thenCondition;
        private String refValue;
        private List<Object> possibleValues;
        private Object example;
        private Map<String, Object> customProperties;
        private JSONSchemaDiscriminator discriminator;
        private String contentMediaType;
        private String contentEncoding;

        @SuppressWarnings("unchecked")
        protected B self()
        {
            return (B) this;
        }

        public JSONSchema build()
        {
            return new JSONSchema(this);
        }

        public B allOf(List<JSONSchema> allOf)
        {
            this.allOf = allOf;
            return this.self();
        }

        public B oneOf(List<JSONSchema> oneOf)
        {
            this.oneOf = oneOf;
            return this.self();
        }

        public B anyOf(List<JSONSchema> anyOf)
        {
            this.anyOf = anyOf;
            return this.self();
        }

        public B mustNotMatch(JSONSchema mustNotMatch)
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

        public B id(String id)
        {
            this.id = id;
            return this.self();
        }

        public B schema(String schema)
        {
            this.schema = schema;
            return this.self();
        }

        public B defaultValue(Object defaultValue)
        {
            this.defaultValue = defaultValue;
            return this.self();
        }

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

        public B nullable(Boolean nullable)
        {
            this.nullable = nullable;
            return this.self();
        }

        public B definitions(Map<String, JSONSchema> definitions)
        {
            this.definitions = definitions;
            return this.self();
        }

        public B ifCondition(JSONSchema ifCondition)
        {
            this.ifCondition = ifCondition;
            return this.self();
        }

        public B elseCondition(JSONSchema elseCondition)
        {
            this.elseCondition = elseCondition;
            return this.self();
        }

        public B thenCondition(JSONSchema thenCondition)
        {
            this.thenCondition = thenCondition;
            return this.self();
        }

        public B refValue(String refValue)
        {
            this.refValue = refValue;
            return this.self();
        }

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

        public B customProperties(Map<String, Object> customProperties)
        {
            this.customProperties = customProperties;
            return this.self();
        }

        public B discriminator(JSONSchemaDiscriminator discriminator)
        {
            this.discriminator = discriminator;
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

    public <T> T accept(JSONSchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
