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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;

@JsonInclude(Include.NON_NULL)
@JsonDeserialize(
        builder = OpenAPIv3_0_3Discriminator.OpenAPIv3_0_3DiscriminatorBuilder.class
)
public class OpenAPIv3_0_3Discriminator
{
    public final String propertyName;
    public final Map<String, String> mapping;

    protected OpenAPIv3_0_3Discriminator(OpenAPIv3_0_3Discriminator.OpenAPIv3_0_3DiscriminatorBuilder b)
    {
        this.propertyName = b.propertyName;
        this.mapping = b.mapping;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3DiscriminatorBuilder
    {
        private String propertyName;
        private Map<String, String> mapping;

        public OpenAPIv3_0_3Discriminator build()
        {
            return new OpenAPIv3_0_3Discriminator(this);
        }

        public OpenAPIv3_0_3DiscriminatorBuilder propertyName(String propertyName)
        {
            this.propertyName = propertyName;
            return this;
        }

        public OpenAPIv3_0_3DiscriminatorBuilder mapping(Map<String, String> mapping)
        {
            this.mapping = mapping;
            return this;
        }
    }

    public static OpenAPIv3_0_3Discriminator.OpenAPIv3_0_3DiscriminatorBuilder builder()
    {
        return new OpenAPIv3_0_3Discriminator.OpenAPIv3_0_3DiscriminatorBuilder();
    }
}
