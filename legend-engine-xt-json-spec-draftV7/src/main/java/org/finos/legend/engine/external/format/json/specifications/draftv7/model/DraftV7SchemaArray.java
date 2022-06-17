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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.finos.legend.engine.external.format.json.specifications.draftv7.visitor.DraftV7SchemaVisitor;

@JsonDeserialize(
        builder = DraftV7SchemaArray.DraftV7SchemaArrayBuilder.class
)
public class DraftV7SchemaArray extends DraftV7Schema
{
    public final Integer minItems;
    public final Integer maxItems;
    public final Boolean uniqueItems;
    @JsonProperty("items")
    public final Object itemSchemas;
    @JsonProperty("contains")
    public final DraftV7Schema containedItemSchema;

    protected DraftV7SchemaArray(DraftV7SchemaArray.DraftV7SchemaArrayBuilder b)
    {
        super(b);
        this.minItems = b.minItems;
        this.maxItems = b.maxItems;
        this.uniqueItems = b.uniqueItems;
        this.itemSchemas = b.itemSchemas;
        this.containedItemSchema = b.containedItemSchema;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class DraftV7SchemaArrayBuilder extends DraftV7SchemaBuilder<DraftV7SchemaArray.DraftV7SchemaArrayBuilder>
    {
        private Integer minItems;
        private Integer maxItems;
        private Boolean uniqueItems;
        private Object itemSchemas;
        private DraftV7Schema containedItemSchema;

        public DraftV7SchemaArray build()
        {
            return new DraftV7SchemaArray(this);
        }

        public DraftV7SchemaArrayBuilder minItems(Integer minItems)
        {
            this.minItems = minItems;
            return this;
        }

        public DraftV7SchemaArrayBuilder maxItems(Integer maxItems)
        {
            this.maxItems = maxItems;
            return this;
        }

        public DraftV7SchemaArrayBuilder uniqueItems(Boolean uniqueItems)
        {
            this.uniqueItems = uniqueItems;
            return this;
        }

        @JsonProperty("items")
        public DraftV7SchemaArrayBuilder itemSchemas(Object itemSchemas)
        {
            this.itemSchemas = itemSchemas;
            return this;
        }

        @JsonProperty("contains")
        public DraftV7SchemaArrayBuilder containedItemSchema(DraftV7Schema containedItemSchema)
        {
            this.containedItemSchema = containedItemSchema;
            return this;
        }

    }

    public <T> T accept(DraftV7SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static DraftV7SchemaArray.DraftV7SchemaArrayBuilder builder()
    {
        return new DraftV7SchemaArray.DraftV7SchemaArrayBuilder();
    }

}
