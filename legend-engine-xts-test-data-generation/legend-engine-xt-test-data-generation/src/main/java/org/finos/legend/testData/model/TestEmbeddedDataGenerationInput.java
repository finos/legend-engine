//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.testData.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TestEmbeddedDataGenerationInput.class, name = "testEmbeddedDataGenerationInput")
})
public class TestEmbeddedDataGenerationInput
{
    @JsonProperty
    public PureModelContext model;
    @JsonProperty
    public String clientVersion;

    public TestEmbeddedDataGenerationInput(@JsonProperty("model") PureModelContext model, @JsonProperty("clientVersion") String clientVersion)
    {
        this.model = model;
        this.clientVersion = clientVersion;
    }

    public TestEmbeddedDataGenerationInput()
    {

    }
}
