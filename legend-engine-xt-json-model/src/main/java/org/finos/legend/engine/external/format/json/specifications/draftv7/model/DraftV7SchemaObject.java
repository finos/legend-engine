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

import java.util.List;
import java.util.Map;

@JsonDeserialize(
        builder = DraftV7SchemaObject.DraftV7SchemaObjectBuilder.class
)
public class DraftV7SchemaObject extends DraftV7Schema
{
    public final Integer minProperties;
    public final Integer maxProperties;
    public final DraftV7Schema propertyNames;
    public final Map<String, DraftV7Schema> properties;
    @JsonProperty("required")
    public final List<String> requiredProperties;
    public final Object additionalProperties;

    protected DraftV7SchemaObject(DraftV7SchemaObject.DraftV7SchemaObjectBuilder b)
    {
        super(b);
        this.minProperties = b.minProperties;
        this.maxProperties = b.maxProperties;
        this.propertyNames = b.propertyNames;
        this.properties = b.properties;
        this.requiredProperties = b.requiredProperties;
        this.additionalProperties = b.additionalProperties;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class DraftV7SchemaObjectBuilder extends DraftV7SchemaBuilder<DraftV7SchemaObject.DraftV7SchemaObjectBuilder>
    {
        private Integer minProperties;
        private Integer maxProperties;
        private DraftV7Schema propertyNames;
        private Map<String, DraftV7Schema> properties;
        private List<String> requiredProperties;
        private Object additionalProperties;

        public DraftV7SchemaObject build()
        {
            return new DraftV7SchemaObject(this);
        }

        public DraftV7SchemaObjectBuilder minProperties(Integer minProperties)
        {
            this.minProperties = minProperties;
            return this;
        }

        public DraftV7SchemaObjectBuilder maxProperties(Integer maxProperties)
        {
            this.maxProperties = maxProperties;
            return this;
        }

        public DraftV7SchemaObjectBuilder propertyNames(DraftV7Schema propertyNames)
        {
            this.propertyNames = propertyNames;
            return this;
        }

        public DraftV7SchemaObjectBuilder properties(Map<String, DraftV7Schema> properties)
        {
            this.properties = properties;
            return this;
        }

        @JsonProperty("required")
        public DraftV7SchemaObjectBuilder requiredProperties(List<String> requiredProperties)
        {
            this.requiredProperties = requiredProperties;
            return this;
        }

        public DraftV7SchemaObjectBuilder additionalProperties(Object additionalProperties)
        {
            this.additionalProperties = additionalProperties;
            return this;
        }
    }

    public <T> T accept(DraftV7SchemaVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static DraftV7SchemaObject.DraftV7SchemaObjectBuilder builder()
    {
        return new DraftV7SchemaObject.DraftV7SchemaObjectBuilder();
    }

}
