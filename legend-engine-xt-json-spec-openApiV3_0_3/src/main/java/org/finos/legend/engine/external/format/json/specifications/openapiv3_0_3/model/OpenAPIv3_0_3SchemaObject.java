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

import java.util.List;
import java.util.Map;

@JsonDeserialize(
        builder = OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder.class
)
public class OpenAPIv3_0_3SchemaObject extends OpenAPIv3_0_3Schema
{
    public final Integer minProperties;
    public final Integer maxProperties;
    public final Map<String, OpenAPIv3_0_3Schema> properties;
    @JsonProperty("required")
    public final List<String> requiredProperties;
    public final Object additionalProperties;

    protected OpenAPIv3_0_3SchemaObject(OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder b)
    {
        super(b);
        this.minProperties = b.minProperties;
        this.maxProperties = b.maxProperties;
        this.properties = b.properties;
        this.requiredProperties = b.requiredProperties;
        this.additionalProperties = b.additionalProperties;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3SchemaObjectBuilder extends OpenAPIv3_0_3SchemaBuilder<OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder>
    {
        private Integer minProperties;
        private Integer maxProperties;
        private Map<String, OpenAPIv3_0_3Schema> properties;
        private List<String> requiredProperties;
        private Object additionalProperties;

        public OpenAPIv3_0_3SchemaObject build()
        {
            return new OpenAPIv3_0_3SchemaObject(this);
        }

        public OpenAPIv3_0_3SchemaObjectBuilder minProperties(Integer minProperties)
        {
            this.minProperties = minProperties;
            return this;
        }

        public OpenAPIv3_0_3SchemaObjectBuilder maxProperties(Integer maxProperties)
        {
            this.maxProperties = maxProperties;
            return this;
        }

        public OpenAPIv3_0_3SchemaObjectBuilder properties(Map<String, OpenAPIv3_0_3Schema> properties)
        {
            this.properties = properties;
            return this;
        }

        @JsonProperty("required")
        public OpenAPIv3_0_3SchemaObjectBuilder requiredProperties(List<String> requiredProperties)
        {
            this.requiredProperties = requiredProperties;
            return this;
        }

        public OpenAPIv3_0_3SchemaObjectBuilder additionalProperties(Object additionalProperties)
        {
            this.additionalProperties = additionalProperties;
            return this;
        }
    }

    public <T> T accept(OpenAPIv3_0_3SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder builder()
    {
        return new OpenAPIv3_0_3SchemaObject.OpenAPIv3_0_3SchemaObjectBuilder();
    }

}
