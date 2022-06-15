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
        builder = OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder.class
)
public class OpenAPIv3_0_3SchemaString extends OpenAPIv3_0_3Schema
{
    public final Integer minLength;
    public final Integer maxLength;
    public final String pattern;
    public final String format;

    protected OpenAPIv3_0_3SchemaString(OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder b)
    {
        super(b);
        this.minLength = b.minLength;
        this.maxLength = b.maxLength;
        this.pattern = b.pattern;
        this.format = b.format;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3SchemaStringBuilder extends OpenAPIv3_0_3SchemaBuilder<OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder>
    {
        private Integer minLength;
        private Integer maxLength;
        private String pattern;
        private String format;

        public OpenAPIv3_0_3SchemaString build()
        {
            return new OpenAPIv3_0_3SchemaString(this);
        }

        public OpenAPIv3_0_3SchemaStringBuilder minLength(Integer minLength)
        {
            this.minLength = minLength;
            return this;
        }

        public OpenAPIv3_0_3SchemaStringBuilder maxLength(Integer maxLength)
        {
            this.maxLength = maxLength;
            return this;
        }

        public OpenAPIv3_0_3SchemaStringBuilder pattern(String pattern)
        {
            this.pattern = pattern;
            return this;
        }

        public OpenAPIv3_0_3SchemaStringBuilder format(String format)
        {
            this.format = format;
            return this;
        }
    }

    public <T> T accept(OpenAPIv3_0_3SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder builder()
    {
        return new OpenAPIv3_0_3SchemaString.OpenAPIv3_0_3SchemaStringBuilder();
    }

}
