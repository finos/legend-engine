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

public class JSONSchemaArray extends JSONSchema
{
    public final Integer minItems;
    public final Integer maxItems;
    public final Boolean uniqueItems;
    public final Object itemSchemas;
    public final JSONSchema containedItemSchema;

    protected JSONSchemaArray(JSONSchemaArray.JSONSchemaArrayBuilder b)
    {
        super(b);
        this.minItems = b.minItems;
        this.maxItems = b.maxItems;
        this.uniqueItems = b.uniqueItems;
        this.itemSchemas = b.itemSchemas;
        this.containedItemSchema = b.containedItemSchema;
    }

    public static class JSONSchemaArrayBuilder extends JSONSchema.JSONSchemaBuilder<JSONSchemaArray.JSONSchemaArrayBuilder>
    {
        private Integer minItems;
        private Integer maxItems;
        private Boolean uniqueItems;
        private Object itemSchemas;
        private JSONSchema containedItemSchema;

        public JSONSchemaArray build()
        {
            return new JSONSchemaArray(this);
        }

        public JSONSchemaArrayBuilder minItems(Integer minItems)
        {
            this.minItems = minItems;
            return this;
        }

        public JSONSchemaArrayBuilder maxItems(Integer maxItems)
        {
            this.maxItems = maxItems;
            return this;
        }

        public JSONSchemaArrayBuilder uniqueItems(Boolean uniqueItems)
        {
            this.uniqueItems = uniqueItems;
            return this;
        }

        public JSONSchemaArrayBuilder itemSchemas(Object itemSchemas)
        {
            this.itemSchemas = itemSchemas;
            return this;
        }

        public JSONSchemaArrayBuilder containedItemSchema(JSONSchema containedItemSchema)
        {
            this.containedItemSchema = containedItemSchema;
            return this;
        }
    }

    public static JSONSchemaArray.JSONSchemaArrayBuilder builder()
    {
        return new JSONSchemaArrayBuilder();
    }

    public <T> T accept(JSONSchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

}
