/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.legend.engine.testData.generation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;

public class TestDataGenerationInput
{
    /**
     * query should be optional. For now, It's required.
     */
    @JsonProperty
    public Lambda query;

    @JsonProperty
    public PureModelContext model;

    @JsonProperty
    public String runtime;

    @JsonProperty
    public String mapping;

    @JsonProperty
    public String clientVersion;

    public TestDataGenerationInput(@JsonProperty("query") Lambda query, @JsonProperty("model") PureModelContext model, @JsonProperty("runtime") String runtime, @JsonProperty("mapping") String mapping, @JsonProperty("clientVersion") String clientVersion)
    {
        this.query = query;
        this.runtime = runtime;
        this.mapping = mapping;
        this.model = model;
        this.clientVersion = clientVersion;
    }
}
