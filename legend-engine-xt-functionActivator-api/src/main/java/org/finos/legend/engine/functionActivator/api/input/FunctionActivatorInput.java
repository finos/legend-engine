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

package org.finos.legend.engine.functionActivator.api.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

public class FunctionActivatorInput
{
    public String clientVersion;

    @JsonProperty(required = true)
    public String functionActivator;

    @JsonProperty(required = true)
    public PureModelContext model;

    @JsonCreator
    public FunctionActivatorInput(
            @JsonProperty("clientVersion") String clientVersion,
            @JsonProperty("functionActivator") String functionActivator,
            @JsonProperty("model") PureModelContext model)
    {
        this.clientVersion = clientVersion;
        this.functionActivator = functionActivator;
        this.model = model;
    }
}
