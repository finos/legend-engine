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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.visitor.OpenAPIv3_0_3SchemaVisitor;

@JsonDeserialize(
        builder = OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder.class
)
public class OpenAPIv3_0_3SchemaNumber extends OpenAPIv3_0_3Schema
{
    public final Number minimum;
    public final Number maximum;
    public final Boolean exclusiveMinimum;
    public final Boolean exclusiveMaximum;
    public final Number multipleOf;
    public final String format;

    protected OpenAPIv3_0_3SchemaNumber(OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<?> b)
    {
        super(b);
        this.minimum = b.minimum;
        this.maximum = b.maximum;
        this.exclusiveMinimum = b.exclusiveMinimum;
        this.exclusiveMaximum = b.exclusiveMaximum;
        this.multipleOf = b.multipleOf;
        this.format = b.format;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3SchemaNumberBuilder<B extends OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<B>> extends OpenAPIv3_0_3SchemaBuilder<B>
    {
        private Number minimum;
        private Number maximum;
        private Boolean exclusiveMinimum;
        private Boolean exclusiveMaximum;
        private Number multipleOf;
        private String format;

        public OpenAPIv3_0_3SchemaNumber build()
        {
            return new OpenAPIv3_0_3SchemaNumber(this);
        }

        public B minimum(Number minimum)
        {
            this.minimum = minimum;
            return this.self();
        }

        public B maximum(Number maximum)
        {
            this.maximum = maximum;
            return this.self();
        }

        public B exclusiveMinimum(Boolean exclusiveMinimum)
        {
            this.exclusiveMinimum = exclusiveMinimum;
            return this.self();
        }

        public B exclusiveMaximum(Boolean exclusiveMaximum)
        {
            this.exclusiveMaximum = exclusiveMaximum;
            return this.self();
        }

        public B multipleOf(Number multipleOf)
        {
            this.multipleOf = multipleOf;
            return this.self();
        }

        public B format(String format)
        {
            this.format = format;
            return this.self();
        }
    }

    public <T> T accept(OpenAPIv3_0_3SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<?> builder()
    {
        return new OpenAPIv3_0_3SchemaNumber.OpenAPIv3_0_3SchemaNumberBuilder<>();
    }

}
