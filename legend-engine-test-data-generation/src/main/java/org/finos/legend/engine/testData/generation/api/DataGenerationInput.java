// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.testData.generation.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.testData.generation.model.DataGenerationRequest;

public class DataGenerationInput
{
    @JsonProperty
    DataGenerationRequest dataGenerationRequest;
    @JsonProperty
    public PureModelContext model;
    @JsonProperty
    public String clientVersion;

    public DataGenerationInput(@JsonProperty("query") DataGenerationRequest dataGenerationRequest, @JsonProperty("model") PureModelContext model, @JsonProperty("clientVersion") String clientVersion)
    {
        this.dataGenerationRequest = dataGenerationRequest;
        this.model = model;
        this.clientVersion = clientVersion;
    }
}
