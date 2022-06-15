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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(
        builder = OpenAPIv3_0_3Components.OpenAPIv3_0_3ComponentsBuilder.class
)
public class OpenAPIv3_0_3Components
{
    public final Map<String, OpenAPIv3_0_3Schema> schemas;

    protected OpenAPIv3_0_3Components(OpenAPIv3_0_3Components.OpenAPIv3_0_3ComponentsBuilder b)
    {
        this.schemas = b.schemas;
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3ComponentsBuilder
    {
        private Map<String, OpenAPIv3_0_3Schema> schemas;

        public OpenAPIv3_0_3Components build()
        {
            return new OpenAPIv3_0_3Components(this);
        }

        public OpenAPIv3_0_3Components.OpenAPIv3_0_3ComponentsBuilder schemas(Map<String, OpenAPIv3_0_3Schema> schemas)
        {
            this.schemas = schemas;
            return this;
        }
    }

    public static OpenAPIv3_0_3Components.OpenAPIv3_0_3ComponentsBuilder builder()
    {
        return new OpenAPIv3_0_3Components.OpenAPIv3_0_3ComponentsBuilder();
    }
}
