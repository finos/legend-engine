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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.visitor.OpenAPIv3_0_3SchemaVisitor;

@JsonDeserialize(
        builder = OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder.class
)
public class OpenAPIv3_0_3SchemaArray extends OpenAPIv3_0_3Schema
{
    public final Integer minItems;
    public final Integer maxItems;
    public final Boolean uniqueItems;
    @JsonProperty("items")
    public final Object itemSchemas;

    protected OpenAPIv3_0_3SchemaArray(OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder b)
    {
        super(b);
        this.minItems = b.minItems;
        this.maxItems = b.maxItems;
        this.uniqueItems = b.uniqueItems;
        this.itemSchemas = b.itemSchemas;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3SchemaArrayBuilder extends OpenAPIv3_0_3SchemaBuilder<OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder>
    {
        private Integer minItems;
        private Integer maxItems;
        private Boolean uniqueItems;
        private Object itemSchemas;

        public OpenAPIv3_0_3SchemaArray build()
        {
            return new OpenAPIv3_0_3SchemaArray(this);
        }

        public OpenAPIv3_0_3SchemaArrayBuilder minItems(Integer minItems)
        {
            this.minItems = minItems;
            return this;
        }

        public OpenAPIv3_0_3SchemaArrayBuilder maxItems(Integer maxItems)
        {
            this.maxItems = maxItems;
            return this;
        }

        public OpenAPIv3_0_3SchemaArrayBuilder uniqueItems(Boolean uniqueItems)
        {
            this.uniqueItems = uniqueItems;
            return this;
        }

        @JsonProperty("items")
        public OpenAPIv3_0_3SchemaArrayBuilder itemSchemas(Object itemSchemas)
        {
            this.itemSchemas = itemSchemas;
            return this;
        }
    }

    public <T> T accept(OpenAPIv3_0_3SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder builder()
    {
        return new OpenAPIv3_0_3SchemaArray.OpenAPIv3_0_3SchemaArrayBuilder();
    }

}
