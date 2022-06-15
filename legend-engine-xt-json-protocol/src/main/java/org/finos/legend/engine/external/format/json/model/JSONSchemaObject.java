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

public class JSONSchemaObject extends JSONSchema
{
    public final Integer minProperties;
    public final Integer maxProperties;
    public final JSONSchema propertyNames;
    public final Map<String, JSONSchema> properties;
    public final List<String> requiredProperties;
    public final Object additionalProperties;

    protected JSONSchemaObject(JSONSchemaObject.JSONSchemaObjectBuilder b)
    {
        super(b);
        this.minProperties = b.minProperties;
        this.maxProperties = b.maxProperties;
        this.propertyNames = b.propertyNames;
        this.properties = b.properties;
        this.requiredProperties = b.requiredProperties;
        this.additionalProperties = b.additionalProperties;
    }

    public static class JSONSchemaObjectBuilder extends JSONSchemaBuilder<JSONSchemaObject.JSONSchemaObjectBuilder>
    {
        private Integer minProperties;
        private Integer maxProperties;
        private JSONSchema propertyNames;
        private Map<String, JSONSchema> properties;
        private List<String> requiredProperties;
        private Object additionalProperties;

        public JSONSchemaObject build()
        {
            return new JSONSchemaObject(this);
        }


        public JSONSchemaObjectBuilder minProperties(Integer minProperties)
        {
            this.minProperties = minProperties;
            return this;
        }

        public JSONSchemaObjectBuilder maxProperties(Integer maxProperties)
        {
            this.maxProperties = maxProperties;
            return this;
        }

        public JSONSchemaObjectBuilder propertyNames(JSONSchema propertyNames)
        {
            this.propertyNames = propertyNames;
            return this;
        }

        public JSONSchemaObjectBuilder properties(Map<String, JSONSchema> properties)
        {
            this.properties = properties;
            return this;
        }

        public JSONSchemaObjectBuilder requiredProperties(List<String> requiredProperties)
        {
            this.requiredProperties = requiredProperties;
            return this;
        }

        public JSONSchemaObjectBuilder additionalProperties(Object additionalProperties)
        {
            this.additionalProperties = additionalProperties;
            return this;
        }
    }

    public <T> T accept(JSONSchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static JSONSchemaObject.JSONSchemaObjectBuilder builder()
    {
        return new JSONSchemaObject.JSONSchemaObjectBuilder();
    }

}
